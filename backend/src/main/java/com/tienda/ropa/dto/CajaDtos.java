package com.tienda.ropa.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public final class CajaDtos {
    private CajaDtos() { }

    public record MovimientoCajaDto(Long id, String tipo, BigDecimal monto, String motivo, String fecha, Long usuarioId) { }

    public record CajaDto(Long id, String numero, Long abiertoPor, Long cerradoPor, String apertura, String cierre,
                          BigDecimal fondoInicial, BigDecimal efectivoEsperado, BigDecimal efectivoContado,
                          BigDecimal diferencia, String estado, String observaciones,
                          List<MovimientoCajaDto> movimientos, List<PagoResumenMetodoDto> ventasPorMetodo) { }

    public record AperturaCajaRequest(
        @NotNull @DecimalMin("0.0") BigDecimal fondoInicial,
        @Size(max = 500) String observaciones
    ) { }

    public record CierreCajaRequest(
        @NotNull @DecimalMin("0.0") BigDecimal efectivoContado,
        @Size(max = 500) String observaciones
    ) { }

    public record MovimientoManualRequest(
        @NotBlank @Pattern(regexp = "INGRESO|EGRESO", message = "Tipo debe ser INGRESO o EGRESO") String tipo,
        @NotNull @DecimalMin(value = "0.01", message = "El valor debe ser positivo") BigDecimal monto,
        @NotBlank(message = "El motivo es obligatorio") @Size(max = 250) String motivo
    ) { }

    public record PagoResumenMetodoDto(String metodo, BigDecimal total) { }
}
