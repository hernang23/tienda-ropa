package com.tienda.ropa.controller;

import com.tienda.ropa.dto.InventarioDtos;
import com.tienda.ropa.enums.TipoMovimiento;
import com.tienda.ropa.security.UsuarioTokenAuthentication;
import com.tienda.ropa.service.InventarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Inventario: existencias, movimientos y ajustes trazables. */
@RestController
@RequestMapping("/api/v1/inventario")
public class InventarioController {

    private final InventarioService inventario;

    public InventarioController(InventarioService inventario) { this.inventario = inventario; }

    @GetMapping
    public Page<com.tienda.ropa.dto.CatalogoDtos.VarianteDto> consultar(
            @RequestParam(required = false) String q, @RequestParam(required = false) String talla,
            @RequestParam(required = false) String color, @RequestParam(required = false) Boolean bajoMinimo,
            @RequestParam(required = false) Boolean agotado, @PageableDefault(size = 20) Pageable pageable) {
        return inventario.consultarInventario(q, talla, color, bajoMinimo, agotado, pageable);
    }

    @GetMapping("/movimientos")
    public Page<InventarioDtos.MovimientoDto> historial(@RequestParam(required = false) Long varianteId,
            @RequestParam(required = false) TipoMovimiento tipo, @PageableDefault(size = 20) Pageable pageable) {
        return inventario.historial(varianteId, tipo, pageable);
    }

    @PostMapping("/variantes/{varianteId}/ajuste")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTARIO')")
    public InventarioDtos.MovimientoDto ajustar(@PathVariable Long varianteId,
            @Valid @RequestBody InventarioDtos.AjusteRequest req,
            @AuthenticationPrincipal UsuarioTokenAuthentication principal, HttpServletRequest http) {
        return inventario.ajustar(varianteId, req, principal.getUsuarioId(), AuthController.ip(http));
    }

    @GetMapping("/stock-bajo")
    public List<com.tienda.ropa.dto.CatalogoDtos.VarianteDto> stockBajo() {
        return inventario.stockBajo();
    }

    @GetMapping("/agotados")
    public List<com.tienda.ropa.dto.CatalogoDtos.VarianteDto> agotados() {
        return inventario.agotados();
    }

    @GetMapping("/resumen")
    public InventarioDtos.ResumenInventarioDto resumen() {
        return inventario.resumen();
    }
}
