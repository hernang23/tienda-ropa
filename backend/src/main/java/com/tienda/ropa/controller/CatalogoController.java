package com.tienda.ropa.controller;

import com.tienda.ropa.dto.CatalogoDtos;
import com.tienda.ropa.security.UsuarioTokenAuthentication;
import com.tienda.ropa.service.CatalogoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Productos, variantes, categorías y marcas. */
@RestController
@RequestMapping("/api/v1")
public class CatalogoController {

    private final CatalogoService catalogo;

    public CatalogoController(CatalogoService catalogo) { this.catalogo = catalogo; }

    // ----- Categorías -----

    @GetMapping("/categorias")
    public List<CatalogoDtos.CategoriaDto> categorias(@RequestParam(defaultValue = "false") boolean soloActivas) {
        return catalogo.listarCategorias(soloActivas);
    }

    @PostMapping("/categorias")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTARIO')")
    public ResponseEntity<CatalogoDtos.CategoriaDto> crearCategoria(
            @Valid @RequestBody CatalogoDtos.NombreDescActivoRequest req,
            @AuthenticationPrincipal UsuarioTokenAuthentication principal, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(catalogo.crearCategoria(req, principal.getUsuarioId(), AuthController.ip(http)));
    }

    @PutMapping("/categorias/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTARIO')")
    public CatalogoDtos.CategoriaDto actualizarCategoria(@PathVariable Long id,
            @Valid @RequestBody CatalogoDtos.NombreDescActivoRequest req,
            @AuthenticationPrincipal UsuarioTokenAuthentication principal, HttpServletRequest http) {
        return catalogo.actualizarCategoria(id, req, principal.getUsuarioId(), AuthController.ip(http));
    }

    // ----- Marcas -----

    @GetMapping("/marcas")
    public List<CatalogoDtos.MarcaDto> marcas(@RequestParam(defaultValue = "false") boolean soloActivas) {
        return catalogo.listarMarcas(soloActivas);
    }

    @PostMapping("/marcas")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTARIO')")
    public ResponseEntity<CatalogoDtos.MarcaDto> crearMarca(@Valid @RequestBody CatalogoDtos.NombreDescActivoRequest req,
            @AuthenticationPrincipal UsuarioTokenAuthentication principal, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(catalogo.crearMarca(req, principal.getUsuarioId(), AuthController.ip(http)));
    }

    @PutMapping("/marcas/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTARIO')")
    public CatalogoDtos.MarcaDto actualizarMarca(@PathVariable Long id,
            @Valid @RequestBody CatalogoDtos.NombreDescActivoRequest req,
            @AuthenticationPrincipal UsuarioTokenAuthentication principal, HttpServletRequest http) {
        return catalogo.actualizarMarca(id, req, principal.getUsuarioId(), AuthController.ip(http));
    }

    // ----- Productos -----

    @GetMapping("/productos")
    public Page<CatalogoDtos.ProductoDto> productos(@RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoriaId, @RequestParam(required = false) Long marcaId,
            @RequestParam(required = false) Boolean soloActivos,
            @PageableDefault(size = 20) Pageable pageable) {
        return catalogo.listarProductos(q, categoriaId, marcaId, soloActivos, pageable);
    }

    @GetMapping("/productos/{id}")
    public CatalogoDtos.ProductoDto producto(@PathVariable Long id) {
        return catalogo.obtenerProducto(id);
    }

    @PostMapping("/productos")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTARIO')")
    public ResponseEntity<CatalogoDtos.ProductoDto> crearProducto(@Valid @RequestBody CatalogoDtos.ProductoCrearRequest req,
            @AuthenticationPrincipal UsuarioTokenAuthentication principal, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(catalogo.crearProducto(req, principal.getUsuarioId(), AuthController.ip(http)));
    }

    @PutMapping("/productos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTARIO')")
    public CatalogoDtos.ProductoDto actualizarProducto(@PathVariable Long id,
            @Valid @RequestBody CatalogoDtos.ProductoActualizarRequest req,
            @AuthenticationPrincipal UsuarioTokenAuthentication principal, HttpServletRequest http) {
        return catalogo.actualizarProducto(id, req, principal.getUsuarioId(), AuthController.ip(http));
    }

    @DeleteMapping("/productos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTARIO')")
    public ResponseEntity<Void> desactivarProducto(@PathVariable Long id,
            @AuthenticationPrincipal UsuarioTokenAuthentication principal, HttpServletRequest http) {
        catalogo.desactivarProducto(id, principal.getUsuarioId(), AuthController.ip(http));
        return ResponseEntity.noContent().build();
    }

    // ----- Variantes -----

    @GetMapping("/productos/{productoId}/variantes")
    public List<CatalogoDtos.VarianteDto> variantes(@PathVariable Long productoId) {
        return catalogo.variantesDeProducto(productoId);
    }

    @PostMapping("/productos/{productoId}/variantes")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTARIO')")
    public ResponseEntity<CatalogoDtos.VarianteDto> agregarVariante(@PathVariable Long productoId,
            @Valid @RequestBody CatalogoDtos.VarianteCrearRequest req,
            @AuthenticationPrincipal UsuarioTokenAuthentication principal, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(catalogo.agregarVariante(productoId, req, principal.getUsuarioId(), AuthController.ip(http)));
    }

    @PutMapping("/variantes/{varianteId}")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTARIO')")
    public CatalogoDtos.VarianteDto actualizarVariante(@PathVariable Long varianteId,
            @Valid @RequestBody CatalogoDtos.VarianteActualizarRequest req,
            @AuthenticationPrincipal UsuarioTokenAuthentication principal, HttpServletRequest http) {
        return catalogo.actualizarVariante(varianteId, req, principal.getUsuarioId(), AuthController.ip(http));
    }

    @GetMapping("/variantes/{varianteId}/historial-precios")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTARIO')")
    public List<Map<String, Object>> historialPrecios(@PathVariable Long varianteId) {
        return catalogo.historialPrecios(varianteId);
    }
}
