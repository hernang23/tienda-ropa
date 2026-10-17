package com.tienda.ropa.controller;

import com.tienda.ropa.dto.ReporteDtos;
import com.tienda.ropa.service.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Dashboard e indicadores calculados desde datos persistidos (nunca cifras estáticas). */
@RestController
@RequestMapping("/api/v1/reportes")
public class ReporteController {

    private final ReporteService reportes;

    public ReporteController(ReporteService reportes) { this.reportes = reportes; }

    @GetMapping("/dashboard")
    public ReporteDtos.DashboardDto dashboard() {
        return reportes.dashboard();
    }

    @GetMapping("/serie-ventas")
    public List<ReporteDtos.PuntoSerieDto> serieVentas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reportes.serieVentas(desde, hasta);
    }

    @GetMapping("/productos-mas-vendidos")
    public List<ReporteDtos.ProductoMasVendidoDto> productosMasVendidos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reportes.productosMasVendidos(desde, hasta);
    }

    @GetMapping("/ingresos-por-metodo")
    public List<com.tienda.ropa.dto.CajaDtos.PagoResumenMetodoDto> ingresosPorMetodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reportes.ingresosPorMetodo(desde, hasta);
    }
}
