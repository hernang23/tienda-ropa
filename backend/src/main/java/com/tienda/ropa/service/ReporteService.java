package com.tienda.ropa.service;

import com.tienda.ropa.dto.CajaDtos;
import com.tienda.ropa.dto.ReporteDtos;
import com.tienda.ropa.entity.VarianteProducto;
import com.tienda.ropa.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** Indicadores del panel y reportes calculados siempre sobre datos persistidos. */
@Service
public class ReporteService {

    private static final int TOP_PRODUCTOS = 10;

    private final VentaRepository ventas;
    private final VarianteProductoRepository variantes;

    public ReporteService(VentaRepository ventas, VarianteProductoRepository variantes) {
        this.ventas = ventas;
        this.variantes = variantes;
    }

    @Transactional(readOnly = true)
    public ReporteDtos.DashboardDto dashboard() {
        LocalDate hoy = LocalDate.now();
        OffsetDateTime desdeHoy = hoy.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        OffsetDateTime hastaHoy = hoy.plusDays(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset()).minusNanos(1);
        OffsetDateTime desdeMes = hoy.withDayOfMonth(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        OffsetDateTime ahora = OffsetDateTime.now();

        BigDecimal ventasHoy = nvl(ventas.totalVentasEntre(desdeHoy, hastaHoy));
        BigDecimal ventasMes = nvl(ventas.totalVentasEntre(desdeMes, ahora));
        long transaccionesHoy = ventas.contarVentasEntre(desdeHoy, hastaHoy);
        long unidadesMes = ventas.unidadesVendidasEntre(desdeMes, ahora);
        long transaccionesMes = ventas.contarVentasEntre(desdeMes, ahora);
        BigDecimal ticketPromedio = transaccionesMes == 0 ? BigDecimal.ZERO
                : ventasMes.divide(BigDecimal.valueOf(transaccionesMes), 2, RoundingMode.HALF_UP);
        BigDecimal gananciaBruta = nvl(ventas.gananciaBrutaEstimada(desdeMes, ahora));
        BigDecimal valorInventario = variantes.findAll().stream()
                .map(v -> v.getCostoUnitario().multiply(BigDecimal.valueOf(v.getExistencias())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long comprasMes = ventas.contarComprasEntre(desdeMes, ahora);

        List<ReporteDtos.PuntoSerieDto> serie = new ArrayList<>();
        for (Object[] fila : ventas.ventasPorDia(hoy.minusDays(29).atStartOfDay().atOffset(OffsetDateTime.now().getOffset()), ahora)) {
            serie.add(new ReporteDtos.PuntoSerieDto(String.valueOf(fila[0]), (BigDecimal) fila[1], ((Number) fila[2]).longValue()));
        }

        List<ReporteDtos.ProductoMasVendidoDto> top = new ArrayList<>();
        for (Object[] fila : ventas.topVariantesVendidas(desdeMes, ahora, PageRequest.of(0, TOP_PRODUCTOS))) {
            Long varianteId = ((Number) fila[0]).longValue();
            variantes.findById(varianteId).ifPresent(v -> {
                BigDecimal ingresos = ventas.topIngresosVariante(varianteId, desdeMes, ahora);
                top.add(new ReporteDtos.ProductoMasVendidoDto(v.getSku(), v.getProducto().getNombre(),
                        v.getTalla(), v.getColor(), ((Number) fila[1]).longValue(), nvl(ingresos)));
            });
        }

        List<CajaDtos.PagoResumenMetodoDto> porMetodo = new ArrayList<>();
        for (Object[] fila : ventas.ingresosPorMetodo(desdeMes, ahora)) {
            porMetodo.add(new CajaDtos.PagoResumenMetodoDto(String.valueOf(fila[0]), (BigDecimal) fila[1]));
        }

        return new ReporteDtos.DashboardDto(ventasHoy, ventasMes, transaccionesHoy, unidadesMes,
                ticketPromedio, gananciaBruta, valorInventario, comprasMes,
                variantes.encontrarStockBajo().size(), variantes.encontrarAgotados().size(),
                serie, top, porMetodo);
    }

    /** Serie de ventas diarias entre fechas, para gráficos y reportes filtrables. */
    @Transactional(readOnly = true)
    public List<ReporteDtos.PuntoSerieDto> serieVentas(LocalDate desde, LocalDate hasta) {
        OffsetDateTime d0 = desde.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        OffsetDateTime h0 = hasta.plusDays(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset()).minusNanos(1);
        List<ReporteDtos.PuntoSerieDto> salida = new ArrayList<>();
        for (Object[] fila : ventas.ventasPorDia(d0, h0)) {
            salida.add(new ReporteDtos.PuntoSerieDto(String.valueOf(fila[0]), (BigDecimal) fila[1], ((Number) fila[2]).longValue()));
        }
        return salida;
    }

    @Transactional(readOnly = true)
    public List<ReporteDtos.ProductoMasVendidoDto> productosMasVendidos(LocalDate desde, LocalDate hasta) {
        OffsetDateTime d0 = desde.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        OffsetDateTime h0 = hasta.plusDays(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset()).minusNanos(1);
        List<ReporteDtos.ProductoMasVendidoDto> salida = new ArrayList<>();
        for (Object[] fila : ventas.topVariantesVendidas(d0, h0, PageRequest.of(0, 50))) {
            Long varianteId = ((Number) fila[0]).longValue();
            variantes.findById(varianteId).ifPresent(v -> {
                BigDecimal ingresos = ventas.topIngresosVariante(varianteId, d0, h0);
                salida.add(new ReporteDtos.ProductoMasVendidoDto(v.getSku(), v.getProducto().getNombre(),
                        v.getTalla(), v.getColor(), ((Number) fila[1]).longValue(), nvl(ingresos)));
            });
        }
        return salida;
    }

    @Transactional(readOnly = true)
    public List<CajaDtos.PagoResumenMetodoDto> ingresosPorMetodo(LocalDate desde, LocalDate hasta) {
        OffsetDateTime d0 = desde.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        OffsetDateTime h0 = hasta.plusDays(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset()).minusNanos(1);
        List<CajaDtos.PagoResumenMetodoDto> salida = new ArrayList<>();
        for (Object[] fila : ventas.ingresosPorMetodo(d0, h0)) {
            salida.add(new CajaDtos.PagoResumenMetodoDto(String.valueOf(fila[0]), (BigDecimal) fila[1]));
        }
        return salida;
    }

    @Transactional(readOnly = true)
    public List<VarianteProducto> bajoMinimo() {
        return variantes.encontrarStockBajo();
    }

    @Transactional(readOnly = true)
    public List<VarianteProducto> agotadas() {
        return variantes.encontrarAgotados();
    }

    private static BigDecimal nvl(BigDecimal b) {
        return b == null ? BigDecimal.ZERO : b;
    }
}
