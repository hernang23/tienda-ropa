package com.tienda.ropa.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public final class CatalogoDtos {
    private CatalogoDtos() { }

    public record CategoriaDto(Long id, String nombre, String descripcion, boolean activo, String creadoEn) { }

    public record NombreDescActivoRequest(
        @NotBlank @Size(max = 80) String nombre,
        @Size(max = 300) String descripcion,
        Boolean activo
    ) { }

    public record MarcaDto(Long id, String nombre, String descripcion, boolean activo, String creadoEn) { }

    public record VarianteDto(Long id, Long productoId, String sku, String codigoBarras, String talla, String color,
                              BigDecimal precioVenta, BigDecimal costoUnitario, int existencias, int stockMinimo,
                              boolean activo, boolean bajoMinimo, boolean agotado) { }

    public record VarianteCrearRequest(
        @NotBlank @Size(max = 60) String sku,
        @Size(max = 40) String codigoBarras,
        @NotBlank @Size(max = 15) String talla,
        @NotBlank @Size(max = 30) String color,
        @NotNull @DecimalMin(value = "0.0", message = "El precio no puede ser negativo") BigDecimal precioVenta,
        @NotNull @DecimalMin(value = "0.0", message = "El costo no puede ser negativo") BigDecimal costoUnitario,
        @Min(0) int existenciasIniciales,
        @Min(0) Integer stockMinimo,
        Boolean activo
    ) { }

    public record VarianteActualizarRequest(
        @Size(max = 40) String codigoBarras,
        @NotNull @DecimalMin("0.0") BigDecimal precioVenta,
        @NotNull @DecimalMin("0.0") BigDecimal costoUnitario,
        @Min(0) Integer stockMinimo,
        Boolean activo
    ) { }

    public record ProductoDto(Long id, String nombre, String descripcion, Long categoriaId, String categoriaNombre,
                              Long marcaId, String marcaNombre, String genero, String tipoPrenda, String temporada,
                              boolean activo, String imagenUrl, String creadoEn, String actualizadoEn,
                              List<VarianteDto> variantes, int totalExistencias) { }

    public record ProductoCrearRequest(
        @NotBlank @Size(max = 150) String nombre,
        @Size(max = 2000) String descripcion,
        Long categoriaId,
        Long marcaId,
        @Size(max = 20) String genero,
        @Size(max = 60) String tipoPrenda,
        @Size(max = 40) String temporada,
        @Size(max = 400) String imagenUrl,
        List<VarianteCrearRequest> variantes
    ) { }

    public record ProductoActualizarRequest(
        @NotBlank @Size(max = 150) String nombre,
        @Size(max = 2000) String descripcion,
        Long categoriaId,
        Long marcaId,
        @Size(max = 20) String genero,
        @Size(max = 60) String tipoPrenda,
        @Size(max = 40) String temporada,
        @Size(max = 400) String imagenUrl,
        Boolean activo
    ) { }
}
