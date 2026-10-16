package com.tienda.ropa.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public final class DevolucionDtos {
    private DevolucionDtos() { }

    public record DetalleDevolucionDto(Long id, Long detalleVentaId, String sku, String productoNombre, String talla,
                                       String color, int cantidad, boolean regresaInventario, String estadoFisico,
                                       BigDecimal montoReembolso, String observacion) { }

    public record DevolucionDto(Long id, String numero, Long ventaId, String ventaNumero, String fecha, String motivo,
                                String tipo, String estado, Long usuarioId, BigDecimal totalReembolsado,
                                List<DetalleDevolucionDto> detalles) { }

    public record ItemDevolucionRequest(
        @NotNull Long detalleVentaId,
        @NotNull @Min(1) int cantidad,
        @NotNull Boolean regresaInventario,
        @NotBlank @Pattern(regexp = "BUENO|DANADO", message = "Estado fisico debe ser BUENO o DANADO") String estadoFisico,
        @DecimalMin("0.0") BigDecimal montoReembolso,
        @Size(max = 250) String observacion
    ) { }

    public record DevolucionRequest(
        @NotNull Long ventaId,
        @NotBlank @Size(max = 250) String motivo,
        @NotBlank @Pattern(regexp = "REEMBOLSO|CAMBIO|SALDO_FAVOR") String tipo,
        @NotEmpty @Valid List<ItemDevolucionRequest> items,
        @Size(max = 64) String claveIdempotencia
    ) { }
}
