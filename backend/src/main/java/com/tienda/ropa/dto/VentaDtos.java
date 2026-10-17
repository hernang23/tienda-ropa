package com.tienda.ropa.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public final class VentaDtos {
    private VentaDtos() { }

    public record DetalleVentaDto(Long id, Long varianteId, String sku, String productoNombre, String talla, String color,
                                  int cantidad, BigDecimal precioUnitario, BigDecimal descuento, BigDecimal impuestos,
                                  BigDecimal subtotal, int devuelto) { }

    public record PagoDto(Long id, String metodo, BigDecimal monto, String referencia, String estado, String fecha) { }

    public record VentaDto(Long id, String numero, Long clienteId, String clienteNombre, Long usuarioId,
                           String fecha, BigDecimal subtotal, BigDecimal descuento, BigDecimal impuestos, BigDecimal total,
                           String estado, String estadoPago, String observaciones,
                           List<DetalleVentaDto> detalles, List<PagoDto> pagos) { }

    public record ItemCarritoRequest(
        @NotNull Long varianteId,
        @NotNull @Min(1) int cantidad,
        @DecimalMin("0.0") BigDecimal descuentoUnidad
    ) { }

    public record PagoRequest(
        @NotBlank String metodo,
        @NotNull @DecimalMin(value = "0.01", message = "El monto del pago debe ser positivo") BigDecimal monto,
        @Size(max = 80) String referencia
    ) { }

    public record VentaRequest(
        Long clienteId,
        @NotEmpty @Valid List<ItemCarritoRequest> items,
        @DecimalMin("0.0") BigDecimal descuentoGlobal,
        @Size(max = 1000) String observaciones,
        @Valid List<PagoRequest> pagos,
        @Size(max = 64) String claveIdempotencia,
        Boolean pendienteDePago
    ) { }

    public record AnularVentaRequest(@NotBlank(message = "Indique el motivo de la anulacion") @Size(max = 250) String motivo) { }
}
