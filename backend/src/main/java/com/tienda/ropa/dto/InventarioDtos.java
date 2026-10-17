package com.tienda.ropa.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class InventarioDtos {
    private InventarioDtos() { }

    public record MovimientoDto(Long id, Long varianteId, String sku, String productoNombre, String talla, String color,
                                String tipo, int cantidad, int existenciasAntes, int existenciasDespues,
                                String motivo, Long usuarioId, String fecha,
                                Long ventaId, Long compraId, Long devolucionId) { }

    /** Ajuste manual de inventario: el motivo es obligatorio por regla de negocio. */
    public record AjusteRequest(
        @NotNull @Min(value = -1000000, message = "Ajuste demasiado grande") Integer delta,
        @NotBlank(message = "El motivo del ajuste es obligatorio") @Size(max = 250) String motivo
    ) { }

    public record ResumenInventarioDto(java.math.BigDecimal valorAlCosto, long unidadesTotales,
                                       long variantesBajoMinimo, long variantesAgotadas) { }
}
