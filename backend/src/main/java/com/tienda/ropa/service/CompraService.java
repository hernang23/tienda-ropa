package com.tienda.ropa.service;

import com.tienda.ropa.dto.CompraDtos;
import com.tienda.ropa.entity.Compra;
import com.tienda.ropa.entity.DetalleCompra;
import com.tienda.ropa.entity.Proveedor;
import com.tienda.ropa.entity.VarianteProducto;
import com.tienda.ropa.enums.EstadoCompra;
import com.tienda.ropa.enums.TipoMovimiento;
import com.tienda.ropa.exception.BusinessException;
import com.tienda.ropa.exception.ConflictException;
import com.tienda.ropa.exception.ResourceNotFoundException;
import com.tienda.ropa.mapper.Mappers;
import com.tienda.ropa.repository.CompraRepository;
import com.tienda.ropa.repository.DetalleCompraRepository;
import com.tienda.ropa.repository.ProveedorRepository;
import com.tienda.ropa.repository.VarianteProductoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Compras a proveedores. Reglas:
 * - BORRADOR/PENDIENTE no alteran el inventario.
 * - La recepcion incrementa existencias UNA sola vez (estado RECIBIDA + movimiento ENTRADA).
 * - Una compra recibida no se edita ni se recibe de nuevo.
 */
@Service
public class CompraService {

    private static final int MAX_INTENTOS_NUMERO = 5;

    private final CompraRepository compras;
    private final DetalleCompraRepository detalles;
    private final ProveedorRepository proveedores;
    private final VarianteProductoRepository variantes;
    private final InventarioService inventario;
    private final NumeroService numeros;
    private final AuditoriaService auditoria;
    private final Mappers mappers;

    public CompraService(CompraRepository compras, DetalleCompraRepository detalles,
                         ProveedorRepository proveedores, VarianteProductoRepository variantes,
                         InventarioService inventario, NumeroService numeros,
                         AuditoriaService auditoria, Mappers mappers) {
        this.compras = compras;
        this.detalles = detalles;
        this.proveedores = proveedores;
        this.variantes = variantes;
        this.inventario = inventario;
        this.numeros = numeros;
        this.auditoria = auditoria;
        this.mappers = mappers;
    }

    @Transactional(readOnly = true)
    public Page<CompraDtos.CompraDto> listar(String q, Pageable pageable) {
        return compras.buscar(q == null || q.isBlank() ? null : q.trim(), pageable)
                .map(c -> mappers.aCompraDto(c, detalles.findByCompraId(c.getId())));
    }

    @Transactional(readOnly = true)
    public CompraDtos.CompraDto obtener(Long id) {
        Compra c = compra(id);
        return mappers.aCompraDto(c, detalles.findByCompraId(id));
    }

    @Transactional
    public CompraDtos.CompraDto crear(CompraDtos.CompraRequest req, Long usuarioId, String ip) {
        Proveedor prov = proveedores.findById(req.proveedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));
        if (!prov.isActivo()) throw new BusinessException("El proveedor esta inactivo");

        List<CompraDtos.DetalleRequest> drs = req.detalles();
        BigDecimal descuentoGlobal = nvl(req.descuento());
        BigDecimal impuestosGlobal = nvl(req.impuestos());

        // Validaciones y calculo por detalle ANTES de persistir (el backend nunca confia en totales del cliente)
        BigDecimal subtotalDetalles = BigDecimal.ZERO;
        java.util.Set<Long> vistos = new java.util.HashSet<>();
        for (CompraDtos.DetalleRequest dr : drs) {
            VarianteProducto v = variante(dr.varianteId());
            if (!v.isActivo()) throw new BusinessException("La variante " + v.getSku() + " esta inactiva");
            if (!vistos.add(v.getId())) {
                throw new BusinessException("La variante " + v.getSku() + " esta repetida en la compra");
            }
            subtotalDetalles = subtotalDetalles.add(suDetalle(dr.cantidad(), dr.costoUnitario(),
                    nvl(dr.descuento()), nvl(dr.impuestos())));
        }
        if (descuentoGlobal.compareTo(subtotalDetalles) > 0) {
            throw new BusinessException("El descuento global supera el subtotal de la compra");
        }

        Compra c = new Compra();
        c.setProveedor(prov);
        c.setEstado(EstadoCompra.BORRADOR);
        c.setUsuarioId(usuarioId);
        c.setObservaciones(req.observaciones());
        c.setDescuento(descuentoGlobal.setScale(2, RoundingMode.HALF_UP));
        c.setImpuestos(impuestosGlobal.setScale(2, RoundingMode.HALF_UP));
        c.setSubtotal(subtotalDetalles.setScale(2, RoundingMode.HALF_UP));
        c.setTotal(subtotalDetalles.subtract(descuentoGlobal).add(impuestosGlobal).setScale(2, RoundingMode.HALF_UP));

        for (CompraDtos.DetalleRequest dr : drs) {
            VarianteProducto v = variante(dr.varianteId());
            DetalleCompra d = new DetalleCompra();
            d.setVariante(v);
            d.setCantidad(dr.cantidad());
            d.setCostoUnitario(dr.costoUnitario().setScale(2, RoundingMode.HALF_UP));
            d.setDescuento(nvl(dr.descuento()).setScale(2, RoundingMode.HALF_UP));
            d.setImpuestos(nvl(dr.impuestos()).setScale(2, RoundingMode.HALF_UP));
            d.setSubtotal(suDetalle(dr.cantidad(), dr.costoUnitario(), nvl(dr.descuento()), nvl(dr.impuestos()))
                    .setScale(2, RoundingMode.HALF_UP));
            c.agregarDetalle(d);
        }
        persistirConNumeroUnico(c);
        auditoria.registrar(usuarioId, "COMPRA_CREAR", "compra", String.valueOf(c.getId()), c.getNumero(), ip);
        return mappers.aCompraDto(c, detalles.findByCompraId(c.getId()));
    }

    /** Transicion de estado con matriz valida: BORRADOR->PENDIENTE, BORRADOR/PENDIENTE->CANCELADA. */
    @Transactional
    public CompraDtos.CompraDto cambiarEstado(Long id, EstadoCompra destino, Long usuarioId, String ip) {
        Compra c = compra(id);
        boolean valido = switch (destino) {
            case PENDIENTE -> c.getEstado() == EstadoCompra.BORRADOR;
            case CANCELADA -> c.getEstado() == EstadoCompra.BORRADOR || c.getEstado() == EstadoCompra.PENDIENTE;
            default -> false; // RECIBIDA solo via recepcion
        };
        if (!valido) {
            throw new ConflictException("No se puede pasar de " + c.getEstado() + " a " + destino);
        }
        c.setEstado(destino);
        compras.save(c);
        auditoria.registrar(usuarioId, "COMPRA_ESTADO", "compra", String.valueOf(id),
                c.getNumero() + " -> " + destino, ip);
        return mappers.aCompraDto(c, detalles.findByCompraId(id));
    }

    /**
     * Recepcion de mercancía: incrementa existencias una unica vez y actualiza el costo
     * de referencia de la variante al ultimo costo de compra.
     */
    @Transactional
    public CompraDtos.CompraDto recibir(Long id, Long usuarioId, String ip) {
        Compra c = compra(id);
        if (c.getEstado() != EstadoCompra.BORRADOR && c.getEstado() != EstadoCompra.PENDIENTE) {
            throw new ConflictException("Solo se pueden recibir compras en borrador o pendientes; esta esta en " + c.getEstado());
        }
        List<DetalleCompra> ds = detalles.findByCompraId(id);
        if (ds.isEmpty()) throw new BusinessException("La compra no tiene detalles que recibir");

        for (DetalleCompra d : ds) {
            inventario.aplicarMovimiento(d.getVariante().getId(), TipoMovimiento.ENTRADA, d.getCantidad(),
                    "Recepcion de compra " + c.getNumero(), usuarioId, null, id, null);
            VarianteProducto v = variantes.findById(d.getVariante().getId()).orElseThrow();
            v.setCostoUnitario(d.getCostoUnitario());
            v.setActualizadoEn(OffsetDateTime.now());
            variantes.save(v);
        }
        c.setEstado(EstadoCompra.RECIBIDA);
        c.setFechaRecepcion(OffsetDateTime.now());
        compras.save(c);
        auditoria.registrar(usuarioId, "COMPRA_RECIBIR", "compra", String.valueOf(id),
                c.getNumero() + " recibida", ip);
        return mappers.aCompraDto(c, detalles.findByCompraId(id));
    }

    private void persistirConNumeroUnico(Compra c) {
        for (int i = 0; i < MAX_INTENTOS_NUMERO; i++) {
            try {
                c.setNumero(numeros.nuevoNumeroCompra());
                compras.saveAndFlush(c);
                return;
            } catch (DataIntegrityViolationException ex) {
                compras.delete(c);
                compras.flush();
            }
        }
        throw new ConflictException("No fue posible generar un numero de compra unico. Intente nuevamente.");
    }

    private static BigDecimal suDetalle(int cantidad, BigDecimal costo, BigDecimal descuento, BigDecimal impuestos) {
        return costo.multiply(BigDecimal.valueOf(cantidad)).subtract(descuento).add(impuestos);
    }

    private static BigDecimal nvl(BigDecimal b) { return b == null ? BigDecimal.ZERO : b; }

    private Compra compra(Long id) {
        return compras.findById(id).orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada"));
    }

    private VarianteProducto variante(Long id) {
        return variantes.findById(id).orElseThrow(() -> new ResourceNotFoundException("La variante indicada no existe"));
    }
}
