package com.tienda.ropa.service;

import com.tienda.ropa.dto.CajaDtos;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** Caja y cierre diario: una única jornada abierta, movimientos manuales auditados y conciliación de efectivo. */
@Service
public class CajaService {

    private final CajaRepository cajas;
    private final MovimientoCajaRepository movimientos;
    private final VentaRepository ventas;
    private final PagoVentaRepository pagos;
    private final DevolucionRepository devoluciones;
    private final NumeroService numeros;
    private final AuditoriaService auditoria;
    private final Mappers mappers;

    public CajaService(CajaRepository cajas, MovimientoCajaRepository movimientos, VentaRepository ventas,
                       PagoVentaRepository pagos, DevolucionRepository devoluciones,
                       NumeroService numeros, AuditoriaService auditoria, Mappers mappers) {
        this.cajas = cajas;
        this.movimientos = movimientos;
        this.ventas = ventas;
        this.pagos = pagos;
        this.devoluciones = devoluciones;
        this.numeros = numeros;
        this.auditoria = auditoria;
        this.mappers = mappers;
    }

    @Transactional(readOnly = true)
    public Page<CajaDtos.CajaDto> listar(Pageable pageable) {
        return cajas.findAllByOrderByIdDesc(pageable).map(this::aDto);
    }

    @Transactional(readOnly = true)
    public CajaDtos.CajaDto obtener(Long id) {
        return aDto(caja(id));
    }

    @Transactional(readOnly = true)
    public CajaDtos.CajaDto cajaAbierta() {
        return cajas.findFirstByEstadoOrderByIdDesc(EstadoCaja.ABIERTA)
                .map(this::aDto)
                .orElse(null);
    }

    @Transactional
    public CajaDtos.CajaDto abrir(CajaDtos.AperturaCajaRequest req, Long usuarioId, String ip) {
        cajas.findFirstByEstadoOrderByIdDesc(EstadoCaja.ABIERTA).ifPresent(c -> {
            throw new ConflictException("Ya existe una caja abierta (" + c.getNumero() + "). Ciérrela antes de abrir otra.");
        });
        if (req.fondoInicial().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("El fondo inicial no puede ser negativo");
        }
        Caja c = new Caja();
        c.setNumero(numeros.nuevoNumeroCaja());
        c.setAbiertoPor(usuarioId);
        c.setFondoInicial(req.fondoInicial());
        c.setEstado(EstadoCaja.ABIERTA);
        c.setObservaciones(req.observaciones());
        Caja guardada = cajas.save(c);
        auditoria.registrar(usuarioId, "CAJA_APERTURA", "caja", String.valueOf(guardada.getId()),
                "Fondo " + req.fondoInicial().toPlainString(), ip);
        return aDto(guardada);
    }

    @Transactional
    public CajaDtos.CajaDto registrarMovimientoManual(Long cajaId, CajaDtos.MovimientoManualRequest req,
                                                      Long usuarioId, String ip) {
        Caja c = caja(cajaId);
        if (c.getEstado() != EstadoCaja.ABIERTA) {
            throw new ConflictException("Solo se admiten movimientos manuales en una caja abierta");
        }
        MovimientoCaja m = new MovimientoCaja();
        m.setCaja(c);
        m.setTipo(TipoMovimientoCaja.valueOf(req.tipo()));
        m.setMonto(req.monto());
        m.setMotivo(req.motivo());
        m.setUsuarioId(usuarioId);
        movimientos.save(m);
        auditoria.registrar(usuarioId, "CAJA_MOVIMIENTO", "caja", String.valueOf(cajaId),
                req.tipo() + " " + req.monto().toPlainString() + ": " + req.motivo(), ip);
        return aDto(c);
    }

    @Transactional
    public CajaDtos.CajaDto cerrar(Long cajaId, CajaDtos.CierreCajaRequest req, Long usuarioId, String ip) {
        Caja c = caja(cajaId);
        if (c.getEstado() == EstadoCaja.CERRADA) {
            throw new ConflictException("La caja ya está cerrada");
        }
        BigDecimal esperado = calcularEfectivoEsperado(c);
        c.setEfectivoEsperado(esperado);
        c.setEfectivoContado(req.efectivoContado());
        c.setDiferencia(req.efectivoContado().subtract(esperado));
        c.setCierre(OffsetDateTime.now());
        c.setCerradoPor(usuarioId);
        c.setEstado(EstadoCaja.CERRADA);
        if (req.observaciones() != null && !req.observaciones().isBlank()) {
            c.setObservaciones(req.observaciones());
        }
        cajas.save(c);
        auditoria.registrar(usuarioId, "CAJA_CIERRE", "caja", String.valueOf(cajaId),
                "Esperado " + esperado.toPlainString() + ", contado " + req.efectivoContado().toPlainString(), ip);
        return aDto(c);
    }

    /** Efectivo esperado = fondo + pagos en efectivo de ventas de la jornada - reembolsos en efectivo - egresos + ingresos. */
    private BigDecimal calcularEfectivoEsperado(Caja c) {
        BigDecimal total = c.getFondoInicial();
        List<Venta> ventasCaja = ventas.buscar(null, c.getApertura(),
                c.getCierre() == null ? OffsetDateTime.now() : c.getCierre(), org.springframework.data.domain.Pageable.unpaged())
                .stream().filter(v -> c.getId().equals(v.getCajaId())).toList();
        for (Venta v : ventasCaja) {
            if (v.getEstado() == EstadoVenta.ANULADA) continue;
            for (PagoVenta p : pagos.findByVentaId(v.getId())) {
                if (p.getMetodo() == MetodoPago.EFECTIVO && "REGISTRADO".equals(p.getEstado())) {
                    total = total.add(p.getMonto());
                }
            }
        }
        // Reembolsos en efectivo realizados durante la jornada (descuentan del cajón).
        for (Devolucion d : devoluciones.listar(org.springframework.data.domain.Pageable.unpaged()).getContent()) {
            if (d.getFecha().isBefore(c.getApertura())) continue;
            if (c.getCierre() != null && d.getFecha().isAfter(c.getCierre())) continue;
            if (d.getEstado() == EstadoDevolucion.ANULADA) continue;
            if (d.getTipo() == TipoDevolucion.REEMBOLSO) {
                total = total.subtract(d.getTotalReembolsado());
            }
        }
        for (MovimientoCaja m : movimientos.findByCajaIdOrderByFechaAsc(c.getId())) {
            total = m.getTipo() == TipoMovimientoCaja.INGRESO ? total.add(m.getMonto()) : total.subtract(m.getMonto());
        }
        return total;
    }

    private Caja caja(Long id) {
        return cajas.findById(id).orElseThrow(() -> new ResourceNotFoundException("Caja no encontrada"));
    }

    private CajaDtos.CajaDto aDto(Caja c) {
        List<MovimientoCaja> movs = movimientos.findByCajaIdOrderByFechaAsc(c.getId());
        List<CajaDtos.PagoResumenMetodoDto> porMetodo = resumenVentasPorMetodo(c);
        return mappers.aCajaDto(c, movs, porMetodo);
    }

    /** Ventas y pagos registrados dentro de la jornada, agrupados por método, sin duplicar otras fuentes. */
    private List<CajaDtos.PagoResumenMetodoDto> resumenVentasPorMetodo(Caja c) {
        OffsetDateTime hasta = c.getCierre() == null ? OffsetDateTime.now() : c.getCierre();
        List<Venta> enJornada = ventas.buscar(null, c.getApertura(), hasta, org.springframework.data.domain.Pageable.unpaged())
                .stream().filter(v -> c.getId().equals(v.getCajaId()) && v.getEstado() != EstadoVenta.ANULADA).toList();
        java.util.EnumMap<MetodoPago, BigDecimal> acumulado = new java.util.EnumMap<>(MetodoPago.class);
        for (Venta v : enJornada) {
            for (PagoVenta p : pagos.findByVentaId(v.getId())) {
                if ("REGISTRADO".equals(p.getEstado())) {
                    acumulado.merge(p.getMetodo(), p.getMonto(), BigDecimal::add);
                }
            }
        }
        List<CajaDtos.PagoResumenMetodoDto> salida = new ArrayList<>();
        acumulado.forEach((metodo, total) -> salida.add(new CajaDtos.PagoResumenMetodoDto(metodo.name(), total)));
        return salida;
    }
}
