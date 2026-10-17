package com.tienda.ropa.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public final class CompraDtos {
    private CompraDtos() { }

    public record DetalleCompraDto(Long id, Long varianteId, String sku, String productoNombre, String talla, String color,
                                   int cantidad, BigDecimal costoUnitario, BigDecimal descuento, BigDecimal impuestos, BigDecimal subtotal) { }

    public record CompraDto(Long id, String numero, Long proveedorId, String proveedorNombre, String estado,
                            String fechaRegistro, String fechaRecepcion, BigDecimal subtotal, BigDecimal descuento,
                            BigDecimal impuestos, BigDecimal total, String observaciones, Long usuarioId,
                            List<DetalleCompraDto> detalles) { }

    public record DetalleRequest(
        @NotNull Long varianteId,
        @NotNull @Min(1) int cantidad,
        @NotNull @DecimalMin("0.0") BigDecimal costoUnitario,
        @DecimalMin("0.0") BigDecimal descuento,
        @DecimalMin("0.0") BigDecimal impuestos
    ) { }

    public record CompraRequest(
        @NotNull Long proveedorId,
        @DecimalMin("0.0") BigDecimal descuento,
        @DecimalMin("0.0") BigDecimal impuestos,
        @Size(max = 1000) String observaciones,
        @NotEmpty @Valid List<DetalleRequest> detalles
    ) { }
}
