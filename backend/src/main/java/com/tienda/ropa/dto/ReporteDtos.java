package com.tienda.ropa.dto;

import java.math.BigDecimal;
import java.util.List;

public final class ReporteDtos {
    private ReporteDtos() { }

    public record IndicadorDto(String etiqueta, BigDecimal valor) { }

    public record PuntoSerieDto(String fecha, BigDecimal total, long transacciones) { }

    public record ProductoMasVendidoDto(String sku, String producto, String talla, String color, long unidades, BigDecimal ingresos) { }

    public record DashboardDto(
        BigDecimal ventasHoy, BigDecimal ventasMes, long transaccionesHoy, long unidadesVendidasMes,
        BigDecimal ticketPromedioMes, BigDecimal gananciaBrutaMes, BigDecimal valorInventarioCosto,
        long comprasMes, long variantesBajoMinimo, long variantesAgotadas,
        List<PuntoSerieDto> serieUltimos30Dias,
        List<ProductoMasVendidoDto> topProductos,
        List<CajaDtos.PagoResumenMetodoDto> ingresosPorMetodoMes
    ) { }

    public record ConfiguracionDto(Long id, String nombreComercial, String logoUrl, String direccion, String telefono,
                                   String correo, String nit, String moneda, String zonaHoraria, BigDecimal ivaPorcentaje,
                                   BigDecimal descuentoMaxVendedor, BigDecimal descuentoMaxAdmin, int stockMinimoDefault,
                                   String metodosPago, boolean permitirClienteOcasional, String textoComprobante) { }

    public record ConfiguracionRequest(
        String nombreComercial, String logoUrl, String direccion, String telefono, String correo, String nit,
        java.math.BigDecimal ivaPorcentaje, java.math.BigDecimal descuentoMaxVendedor, java.math.BigDecimal descuentoMaxAdmin,
        Integer stockMinimoDefault, String metodosPago, Boolean permitirClienteOcasional, String textoComprobante
    ) { }
}
