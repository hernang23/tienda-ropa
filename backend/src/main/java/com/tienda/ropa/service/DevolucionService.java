package com.tienda.ropa.service;

import com.tienda.ropa.dto.DevolucionDtos;
import com.tienda.ropa.entity.*;
import com.tienda.ropa.enums.*;
import com.tienda.ropa.exception.BusinessException;
import com.tienda.ropa.exception.ConflictException;
import com.tienda.ropa.exception.ResourceNotFoundException;
import com.tienda.ropa.mapper.Mappers;
import com.tienda.ropa.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Devoluciones parciales o totales asociadas a ventas existentes, con trazabilidad de inventario. */
@Service
public class DevolucionService {

    private final DevolucionRepository devoluciones;
    private final DetalleDevolucionRepository detallesDevolucion;
    private final VentaRepository ventas;
    private final DetalleVentaRepository detallesVenta;
    private final InventarioService inventario;
    private final NumeroService numeros;
    private final AuditoriaService auditoria;
    private final Mappers mappers;

    public DevolucionService(DevolucionRepository devoluciones, DetalleDevolucionRepository detallesDevolucion,
                             VentaRepository ventas, DetalleVentaRepository detallesVenta,
                             InventarioService inventario, NumeroService numeros,
                             AuditoriaService auditoria, Mappers mappers) {
        this.devoluciones = devoluciones;
        this.detallesDevolucion = detallesDevolucion;
        this.ventas = ventas;
        this.detallesVenta = detallesVenta;
        this.inventario = inventario;
        this.numeros = numeros;
        this.auditoria = auditoria;
        this.mappers = mappers;
    }

    @Transactional(readOnly = true)
    public Page<DevolucionDtos.DevolucionDto> listar(Pageable pageable) {
        return devoluciones.listar(pageable).map(d ->
                mappers.aDevolucionDto(d, detallesDevolucion.findByDevolucionId(d.getId())));
    }

    @Transactional(readOnly = true)
    public DevolucionDtos.DevolucionDto obtener(Long id) {
        Devolucion d = devoluciones.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Devolución no encontrada"));
        return mappers.aDevolucionDto(d, detallesDevolucion.findByDevolucionId(id));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DevolucionDtos.DevolucionDto registrar(DevolucionDtos.DevolucionRequest req, Long usuarioId, String ip) {
        // Idempotencia: si la clave ya fue procesada, se devuelve la devolución existente sin duplicar movimientos.
        if (req.claveIdempotencia() != null && !req.claveIdempotencia().isBlank()) {
            var existente = devoluciones.findByClaveIdempotencia(req.claveIdempotencia());
            if (existente.isPresent()) {
                Devolucion prev = existente.get();
                return mappers.aDevolucionDto(prev, detallesDevolucion.findByDevolucionId(prev.getId()));
            }
        }

        Venta venta = ventas.findById(req.ventaId())
                .orElseThrow(() -> new ResourceNotFoundException("La venta indicada no existe"));
        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new ConflictException("No se pueden devolver artículos de una venta anulada");
        }

        TipoDevolucion tipo;
        try {
            tipo = TipoDevolucion.valueOf(req.tipo());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Tipo de devolución inválido");
        }

        Devolucion dev = new Devolucion();
        dev.setVenta(venta);
        dev.setNumero(numeros.nuevoNumeroDevolucion());
        dev.setMotivo(req.motivo());
        dev.setTipo(tipo);
        dev.setEstado(EstadoDevolucion.REGISTRADA);
        dev.setUsuarioId(usuarioId);
        if (req.claveIdempotencia() != null && !req.claveIdempotencia().isBlank()) {
            dev.setClaveIdempotencia(req.claveIdempotencia());
        }

        BigDecimal totalReembolso = BigDecimal.ZERO;
        for (DevolucionDtos.ItemDevolucionRequest item : req.items()) {
            DetalleVenta dv = detallesVenta.findById(item.detalleVentaId())
                    .orElseThrow(() -> new BusinessException("Un detalle de la venta no existe"));
            if (!dv.getVenta().getId().equals(venta.getId())) {
                throw new BusinessException("Un detalle indicado no pertenece a la venta");
            }
            int disponible = dv.getCantidad() - dv.getDevuelto();
            if (item.cantidad() > disponible) {
                throw new ConflictException(String.format(
                        "No se puede devolver %d unidad(es) del SKU %s: solo hay %d pendientes de devolución",
                        item.cantidad(), dv.getVariante().getSku(), disponible));
            }

            BigDecimal monto = item.montoReembolso() == null ? BigDecimal.ZERO : item.montoReembolso();
            if (monto.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("El monto de reembolso no puede ser negativo");
            }
            // Tope: el reembolso por artículo no puede superar el subtotal efectivamente cobrado proporcional.
            BigDecimal maximo = dv.getSubtotal();
            if (monto.compareTo(maximo) > 0) {
                throw new ConflictException("El reembolso supera el valor cobrado por el artículo");
            }

            DetalleDevolucion dd = new DetalleDevolucion();
            dd.setDetalleVenta(dv);
            dd.setCantidad(item.cantidad());
            dd.setRegresaInventario(Boolean.TRUE.equals(item.regresaInventario()));
            dd.setEstadoFisico(item.estadoFisico());
            dd.setMontoReembolso(monto);
            dd.setObservacion(item.observacion());
            dev.agregarDetalle(dd);
            totalReembolso = totalReembolso.add(monto);

            dv.setDevuelto(dv.getDevuelto() + item.cantidad());
        }

        dev.setTotalReembolsado(totalReembolso);
        Devolucion guardada = devoluciones.save(dev);

        // Movimientos de inventario después de persistir para referenciar la devolución.
        for (DetalleDevolucion dd : guardada.getDetalles()) {
            if (dd.isRegresaInventario() && "BUENO".equals(dd.getEstadoFisico())) {
                inventario.aplicarMovimiento(dd.getDetalleVenta().getVariante().getId(),
                        TipoMovimiento.DEVOLUCION, dd.getCantidad(),
                        "Devolución " + guardada.getNumero(), usuarioId,
                        venta.getId(), null, guardada.getId());
            }
        }

        auditoria.registrar(usuarioId, "DEVOLUCION_REGISTRADA", "devolucion",
                String.valueOf(guardada.getId()),
                "Venta " + venta.getNumero() + ", reembolso " + totalReembolso.toPlainString(), ip);
        return mappers.aDevolucionDto(guardada, detallesDevolucion.findByDevolucionId(guardada.getId()));
    }

    @Transactional
    public DevolucionDtos.DevolucionDto anular(Long id, Long usuarioId, boolean esAdmin, String ip) {
        Devolucion d = devoluciones.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Devolución no encontrada"));
        if (d.getEstado() == EstadoDevolucion.ANULADA) {
            throw new ConflictException("La devolución ya está anulada");
        }
        if (!esAdmin) {
            throw new ConflictException("Solo el administrador puede anular devoluciones");
        }
        // Revertir los movimientos de inventario que regresaron unidades.
        List<DetalleDevolucion> detalles = detallesDevolucion.findByDevolucionId(id);
        for (DetalleDevolucion dd : detalles) {
            if (dd.isRegresaInventario() && "BUENO".equals(dd.getEstadoFisico())) {
                inventario.aplicarMovimiento(dd.getDetalleVenta().getVariante().getId(),
                        TipoMovimiento.SALIDA, dd.getCantidad(),
                        "Anulación de devolución " + d.getNumero(), usuarioId,
                        d.getVenta().getId(), null, d.getId());
            }
            DetalleVenta dv = dd.getDetalleVenta();
            dv.setDevuelto(dv.getDevuelto() - dd.getCantidad());
        }
        d.setEstado(EstadoDevolucion.ANULADA);
        devoluciones.save(d);
        auditoria.registrar(usuarioId, "DEVOLUCION_ANULADA", "devolucion", String.valueOf(id),
                "Número " + d.getNumero(), ip);
        return mappers.aDevolucionDto(d, detallesDevolucion.findByDevolucionId(id));
    }
}
