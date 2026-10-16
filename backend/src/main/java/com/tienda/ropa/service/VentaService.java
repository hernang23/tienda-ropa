package com.tienda.ropa.service;

import com.tienda.ropa.dto.CajaDtos;
import com.tienda.ropa.dto.VentaDtos;
import com.tienda.ropa.entity.*;
import com.tienda.ropa.enums.*;
import com.tienda.ropa.exception.BusinessException;
import com.tienda.ropa.exception.ConflictException;
import com.tienda.ropa.exception.ResourceNotFoundException;
import com.tienda.ropa.mapper.Mappers;
import com.tienda.ropa.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ventas. Reglas centrales:
 * - Los importes SIEMPRE los calcula el servidor a partir del precio vigente de la variante.
 * - La disponibilidad se verifica con bloqueo pesimista al confirmar (dentro de la transaccion).
 * - Idempotencia por clave: reintentos del mismo POST devuelven la venta ya creada.
 * - Numeracion unica diaria garantizada por UNIQUE en BD con reintentos.
 */
@Service
public class VentaService {

    private static final int MAX_INTENTOS_NUMERO = 5;

    private final VentaRepository ventas;
    private final DetalleVentaRepository detalleVentas;
    private final PagoVentaRepository pagos;
    private final VarianteProductoRepository variantes;
    private final ClienteRepository clientes;
    private final CajaRepository cajas;
    private final DevolucionRepository devoluciones;
    private final InventarioService inventario;
    private final ConfiguracionService configuracion;
    private final NumeroService numeros;
    private final AuditoriaService auditoria;
    private final Mappers mappers;
    private final UsuarioRepository usuariosParaRoles;

    public VentaService(VentaRepository ventas, DetalleVentaRepository detalleVentas, PagoVentaRepository pagos,
                        VarianteProductoRepository variantes, ClienteRepository clientes, CajaRepository cajas,
                        DevolucionRepository devoluciones, InventarioService inventario,
                        ConfiguracionService configuracion, NumeroService numeros,
                        AuditoriaService auditoria, Mappers mappers, UsuarioRepository usuariosParaRoles) {
        this.ventas = ventas;
        this.detalleVentas = detalleVentas;
        this.pagos = pagos;
        this.variantes = variantes;
        this.clientes = clientes;
        this.cajas = cajas;
        this.devoluciones = devoluciones;
        this.inventario = inventario;
        this.configuracion = configuracion;
        this.numeros = numeros;
        this.auditoria = auditoria;
        this.mappers = mappers;
        this.usuariosParaRoles = usuariosParaRoles;
    }

    // ---------- Consulta ----------

    @Transactional(readOnly = true)
    public Page<VentaDtos.VentaDto> listar(String q, LocalDate desde, LocalDate hasta, String estado, Pageable pageable) {
        OffsetDateTime d0 = desde == null ? null : desde.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        OffsetDateTime h0 = hasta == null ? null : hasta.plusDays(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        return ventas.buscar(q == null || q.isBlank() ? null : q.trim(), d0, h0, pageable)
                .filter(v -> estado == null || estado.isBlank() || v.getEstado().name().equalsIgnoreCase(estado))
                .map(this::aDto);
    }

    @Transactional(readOnly = true)
    public VentaDtos.VentaDto obtener(Long id) {
        return aDto(venta(id));
    }

    @Transactional(readOnly = true)
    public VentaDtos.VentaDto porNumero(String numero) {
        return ventas.findByNumero(numero).map(this::aDto)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada"));
    }

    /** Vista previa de totales calculada por el servidor (no persiste nada). */
    @Transactional(readOnly = true)
    public Map<String, Object> simular(VentaDtos.VentaRequest req, Long usuarioId) {
        Calculo calc = calcular(req, usuarioId, false);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("subtotal", calc.subtotal);
        out.put("descuento", calc.descuento);
        out.put("impuestos", calc.impuestos);
        out.put("total", calc.total);
        out.put("detalles", calc.items.stream().map(i -> Map.of(
                "varianteId", i.variante.getId(),
                "sku", i.variante.getSku(),
                "cantidad", i.cantidad,
                "precioUnitario", i.precio,
                "descuento", i.descuento,
                "impuestos", i.impuestos,
                "subtotal", i.subtotal)).toList());
        return out;
    }

    // ---------- Crear / confirmar ----------

    @Transactional
    public VentaDtos.VentaDto crear(VentaDtos.VentaRequest req, Long usuarioId, String ip) {
        // 1. Idempotencia: si la clave ya existe, devolver la venta registrada (reintentos no duplican).
        if (req.claveIdempotencia() != null && !req.claveIdempotencia().isBlank()) {
            var existente = ventas.findByClaveIdempotencia(req.claveIdempotencia());
            if (existente.isPresent()) return aDto(existente.get());
        }

        boolean pendientePago = Boolean.TRUE.equals(req.pendienteDePago());
        List<VentaDtos.PagoRequest> pagoReqs = req.pagos() == null ? List.of() : req.pagos();

        // 2. Validaciones basicas de pago antes de calcular
        BigDecimal totalPagos = BigDecimal.ZERO;
        for (VentaDtos.PagoRequest pr : pagoReqs) {
            try { MetodoPago.valueOf(pr.metodo().toUpperCase()); }
            catch (IllegalArgumentException ex) { throw new BusinessException("Metodo de pago invalido: " + pr.metodo()); }
            totalPagos = totalPagos.add(pr.monto());
        }
        if (!pagoReqs.isEmpty() && totalPagos.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El total de los pagos debe ser positivo");
        }

        // 3. Calcular importes en el servidor y reservar stock con bloqueo pesimista
        Calculo calc = calcular(req, usuarioId, true);
        if (totalPagos.compareTo(calc.total) > 0) {
            throw new BusinessException("Los pagos superan el total de la venta");
        }

        boolean pagado = !pagoReqs.isEmpty() && totalPagos.compareTo(calc.total) == 0;
        if (pendientePago && !pagoReqs.isEmpty()) {
            throw new BusinessException("Una venta pendiente de pago no puede registrar pagos parciales aqui; confirme la venta sin pagos o registrelos aparte");
        }

        Caja caja = cajaAbiertaObligatoria();

        Venta v = new Venta();
        v.setCliente(calc.cliente);
        v.setUsuarioId(usuarioId);
        v.setSubtotal(calc.subtotal);
        v.setDescuento(calc.descuento);
        v.setImpuestos(calc.impuestos);
        v.setTotal(calc.total);
        v.setObservaciones(req.observaciones());
        v.setCajaId(caja.getId());
        v.setEstado(pendientePago ? EstadoVenta.PENDIENTE_PAGO : EstadoVenta.COMPLETADA);
        v.setEstadoPago(pagado ? EstadoPagoVenta.PAGADO
                : totalPagos.compareTo(BigDecimal.ZERO) > 0 ? EstadoPagoVenta.PARCIAL
                : EstadoPagoVenta.PENDIENTE);
        if (req.claveIdempotencia() != null && !req.claveIdempotencia().isBlank()) {
            v.setClaveIdempotencia(req.claveIdempotencia().trim());
        }

        for (ItemCalculado ic : calc.items) {
            DetalleVenta d = new DetalleVenta();
            d.setVariante(ic.variante);
            d.setCantidad(ic.cantidad);
            d.setPrecioUnitario(ic.precio);
            d.setDescuento(ic.descuento);
            d.setImpuestos(ic.impuestos);
            d.setSubtotal(ic.subtotal);
            d.setCostoUnitario(ic.costo);
            v.getDetalles().add(d);
        }
        for (VentaDtos.PagoRequest pr : pagoReqs) {
            PagoVenta p = new PagoVenta();
            p.setVenta(v);
            p.setMetodo(MetodoPago.valueOf(pr.metodo().toUpperCase()));
            p.setMonto(pr.monto().setScale(2, RoundingMode.HALF_UP));
            p.setReferencia(pr.referencia());
            p.setUsuarioId(usuarioId);
            v.getPagos().add(p);
        }

        persistirConNumeroUnico(v);

        // 4. Descuentos de stock solo cuando la venta queda COMPLETADA
        if (v.getEstado() == EstadoVenta.COMPLETADA) {
            for (DetalleVenta d : v.getDetalles()) {
                inventario.aplicarMovimiento(d.getVariante().getId(), TipoMovimiento.SALIDA, d.getCantidad(),
                        "Venta " + v.getNumero(), usuarioId, v.getId(), null, null);
            }
        }
        auditoria.registrar(usuarioId, "VENTA_CREAR", "venta", String.valueOf(v.getId()),
                v.getNumero() + " total=" + v.getTotal(), ip);
        return aDto(v);
    }

    /** Registrar pagos a una venta pendiente/parcial. */
    @Transactional
    public VentaDtos.VentaDto registrarPagos(Long ventaId, List<VentaDtos.PagoRequest> nuevosPagos, Long usuarioId, String ip) {
        Venta v = venta(ventaId);
        if (v.getEstado() == EstadoVenta.ANULADA) throw new ConflictException("La venta esta anulada");
        if (nuevosPagos == null || nuevosPagos.isEmpty()) throw new BusinessException("Indique al menos un pago");

        BigDecimal yaPagado = v.getPagos().stream().filter(p -> "REGISTRADO".equals(p.getEstado()))
                .map(PagoVenta::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal nuevos = BigDecimal.ZERO;
        for (VentaDtos.PagoRequest pr : nuevosPagos) {
            PagoVenta p = new PagoVenta();
            p.setVenta(v);
            try { p.setMetodo(MetodoPago.valueOf(pr.metodo().toUpperCase())); }
            catch (IllegalArgumentException ex) { throw new BusinessException("Metodo de pago invalido: " + pr.metodo()); }
            p.setMonto(pr.monto().setScale(2, RoundingMode.HALF_UP));
            p.setReferencia(pr.referencia());
            p.setUsuarioId(usuarioId);
            pagos.save(p);
            v.getPagos().add(p);
            nuevos = nuevos.add(p.getMonto());
        }
        if (yaPagado.add(nuevos).compareTo(v.getTotal()) > 0) {
            throw new BusinessException("Los pagos registrados superan el total de la venta");
        }
        v.setEstadoPago(yaPagado.add(nuevos).compareTo(v.getTotal()) >= 0
                ? EstadoPagoVenta.PAGADO : EstadoPagoVenta.PARCIAL);

        // Si estaba pendiente de pago y ahora queda pagada, se confirma (descuenta stock)
        if (v.getEstado() == EstadoVenta.PENDIENTE_PAGO && v.getEstadoPago() == EstadoPagoVenta.PAGADO) {
            for (DetalleVenta d : v.getDetalles()) {
                inventario.aplicarMovimiento(d.getVariante().getId(), TipoMovimiento.SALIDA, d.getCantidad(),
                        "Venta " + v.getNumero(), usuarioId, v.getId(), null, null);
            }
            v.setEstado(EstadoVenta.COMPLETADA);
        }
        ventas.save(v);
        auditoria.registrar(usuarioId, "VENTA_PAGO", "venta", String.valueOf(ventaId),
                "+" + nuevos + " (" + v.getEstadoPago() + ")", ip);
        return aDto(v);
    }

    // ---------- Anular ----------

    @Transactional
    public VentaDtos.VentaDto anular(Long ventaId, String motivo, Long usuarioId, boolean esAdmin, String ip) {
        Venta v = venta(ventaId);
        if (v.getEstado() == EstadoVenta.ANULADA) throw new ConflictException("La venta ya esta anulada");
        if (!esAdmin && !usuarioId.equals(v.getUsuarioId())) {
            throw new BusinessException("Solo el administrador o el vendedor que registr6 la venta pueden anularla");
        }
        long devolucionesActivas = devoluciones.findByVentaId(ventaId).stream()
                .filter(d -> d.getEstado() == EstadoDevolucion.REGISTRADA).count();
        if (devolucionesActivas > 0) {
            throw new ConflictException("La venta tiene devoluciones activas; no puede anularse en bloque");
        }

        // Reversión de inventario únicamente si la venta llegó a descontar stock
        if (v.getEstado() == EstadoVenta.COMPLETADA) {
            for (DetalleVenta d : v.getDetalles()) {
                int disponible = d.getCantidad() - d.getDevuelto();
                if (disponible > 0) {
                    inventario.aplicarMovimiento(d.getVariante().getId(), TipoMovimiento.DEVOLUCION, disponible,
                            "Anulacion de venta " + v.getNumero() + ": " + motivo, usuarioId, v.getId(), null, null);
                }
            }
        }
        // Los pagos quedan marcados como ANULADOS (trazabilidad preservada, registro original intacto)
        for (PagoVenta p : v.getPagos()) {
            if ("REGISTRADO".equals(p.getEstado())) p.setEstado("ANULADO");
        }
        v.setEstado(EstadoVenta.ANULADA);
        String obs = (v.getObservaciones() == null ? "" : v.getObservaciones() + " | ")
                + "ANULADA: " + motivo;
        v.setObservaciones(obs.length() > 1000 ? obs.substring(0, 1000) : obs);
        ventas.save(v);
        auditoria.registrar(usuarioId, "VENTA_ANULAR", "venta", String.valueOf(ventaId),
                v.getNumero() + " motivo: " + motivo, ip);
        return aDto(v);
    }

    // ---------- Internos ----------

    private record ItemCalculado(VarianteProducto variante, int cantidad, BigDecimal precio,
                                 BigDecimal descuento, BigDecimal impuestos, BigDecimal subtotal, BigDecimal costo) { }

    private static class Calculo {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal descuento = BigDecimal.ZERO;
        BigDecimal impuestos = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        Cliente cliente;
        List<ItemCalculado> items = new ArrayList<>();
    }

    private Calculo calcular(VentaDtos.VentaRequest req, Long usuarioId, boolean reservarStock) {
        ConfiguracionTienda cfg = configuracion.entidad();
        Calculo calc = new Calculo();

        if (req.clienteId() != null) {
            Cliente c = clientes.findById(req.clienteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
            if (!c.isActivo()) throw new BusinessException("El cliente esta inactivo");
            calc.cliente = c;
        } else if (!cfg.isPermitirClienteOcasional()) {
            throw new BusinessException("La tienda no permite ventas a cliente ocasional");
        }

        BigDecimal descuentoGlobal = req.descuentoGlobal() == null ? BigDecimal.ZERO : req.descuentoGlobal();
        if (descuentoGlobal.compareTo(BigDecimal.ZERO) < 0) throw new BusinessException("El descuento no puede ser negativo");

        java.util.Set<Long> vistos = new java.util.HashSet<>();
        BigDecimal bruto = BigDecimal.ZERO;
        for (VentaDtos.ItemCarritoRequest it : req.items()) {
            VarianteProducto v;
            if (reservarStock) {
                v = variantes.findByIdForUpdate(it.varianteId())
                        .orElseThrow(() -> new ResourceNotFoundException("La variante indicada no existe"));
            } else {
                v = variantes.findById(it.varianteId())
                        .orElseThrow(() -> new ResourceNotFoundException("La variante indicada no existe"));
            }
            if (!v.isActivo() || !v.getProducto().isActivo()) {
                throw new BusinessException("El producto " + v.getProducto().getNombre() + " (" + v.getSku() + ") esta inactivo");
            }
            if (!vistos.add(v.getId())) {
                throw new BusinessException("La variante " + v.getSku() + " esta repetida; agrupe las cantidades");
            }
            BigDecimal descuentoUnidad = nvl(it.descuentoUnidad());
            BigDecimal precio = v.getPrecioVenta();
            BigDecimal lineaBruta = precio.multiply(BigDecimal.valueOf(it.cantidad()));
            if (descuentoUnidad.compareTo(lineaBruta) > 0) {
                throw new BusinessException("El descuento de la linea " + v.getSku() + " supera su importe");
            }
            // IVA por defecto de configuracion aplicado sobre la linea neta
            BigDecimal netoLinea = lineaBruta.subtract(descuentoUnidad);
            BigDecimal impLinea = netoLinea.multiply(cfg.getIvaPorcentaje())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal subLinea = netoLinea.add(impLinea);

            bruto = bruto.add(lineaBruta);
            calc.subtotal = calc.subtotal.add(lineaBruta);
            calc.impuestos = calc.impuestos.add(impLinea);
            calc.items.add(new ItemCalculado(v, it.cantidad(), precio.setScale(2, RoundingMode.HALF_UP),
                    descuentoUnidad.setScale(2, RoundingMode.HALF_UP), impLinea,
                    subLinea.setScale(2, RoundingMode.HALF_UP), v.getCostoUnitario()));

            if (reservarStock && v.getExistencias() < it.cantidad()) {
                throw new com.tienda.ropa.exception.InsufficientStockException(
                        "Inventario insuficiente para " + v.getProducto().getNombre() + " " + v.getTalla() + "/" + v.getColor()
                        + ". Disponible: " + v.getExistencias() + ", solicitado: " + it.cantidad());
            }
        }

        // Limite de descuento segun rol (el porcentaje maximo se aplica sobre el subtotal bruto)
        BigDecimal maxPct = esAdmin(usuarioId) ? cfg.getDescuentoMaxAdmin() : cfg.getDescuentoMaxVendedor();
        BigDecimal maxDescuento = calc.subtotal.multiply(maxPct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        if (descuentoGlobal.compareTo(maxDescuento) > 0) {
            throw new BusinessException("El descuento global (" + descuentoGlobal + ") supera el maximo permitido ("
                    + maxDescuento + ", " + maxPct + "%)");
        }
        calc.descuento = descuentoGlobal.setScale(2, RoundingMode.HALF_UP);
        calc.total = calc.subtotal.add(calc.impuestos).subtract(calc.descuento).setScale(2, RoundingMode.HALF_UP);
        if (calc.total.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("El total no puede ser negativo");
        }
        return calc;
    }

    private boolean esAdmin(Long usuarioId) {
        // Se apoya en AuthService para leer roles persistedos
        return authServiceRoles(usuarioId).contains(RolNombre.ADMIN.name());
    }

    private List<String> authServiceRoles(Long usuarioId) {
        return usuariosParaRoles.findById(usuarioId)
                .map(u -> u.getRoles().stream().map(r -> r.getNombre().name()).toList())
                .orElse(List.of());
    }

    private Caja cajaAbiertaObligatoria() {
        return cajas.findFirstByEstadoOrderByIdDesc(EstadoCaja.ABIERTA)
                .orElseThrow(() -> new BusinessException("No hay caja abierta. Abra la jornada de caja antes de vender"));
    }

    private void persistirConNumeroUnico(Venta v) {
        for (int i = 0; i < MAX_INTENTOS_NUMERO; i++) {
            try {
                v.setNumero(numeros.nuevoNumeroVenta());
                ventas.saveAndFlush(v);
                return;
            } catch (DataIntegrityViolationException ex) {
                if (v.getClaveIdempotencia() != null && ex.getMessage() != null
                        && ex.getMessage().toLowerCase().contains("clave_idempotencia")) {
                    throw new ConflictException("La clave de idempotencia ya fue utilizada");
                }
                ventas.delete(v);
                ventas.flush();
            }
        }
        throw new ConflictException("No fue posible generar un numero de venta unico. Intente nuevamente.");
    }

    private VentaDtos.VentaDto aDto(Venta v) {
        return mappers.aVentaDto(v, detalleVentas.findByVentaId(v.getId()), pagos.findByVentaId(v.getId()));
    }

    private Venta venta(Long id) {
        return ventas.findById(id).orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada"));
    }

    private static BigDecimal nvl(BigDecimal b) { return b == null ? BigDecimal.ZERO : b; }
}
