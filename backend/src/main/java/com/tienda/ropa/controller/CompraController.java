package com.tienda.ropa.controller;

import com.tienda.ropa.dto.CompraDtos;
import com.tienda.ropa.enums.EstadoCompra;
import com.tienda.ropa.security.UsuarioTokenAuthentication;
import com.tienda.ropa.service.CompraService;
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

/** Compras a proveedores y recepción de mercancía. */
@RestController
@RequestMapping("/api/v1/compras")
@PreAuthorize("hasAnyRole('ADMIN','INVENTARIO')")
public class CompraController {

    private final CompraService compras;

    public CompraController(CompraService compras) { this.compras = compras; }

    @GetMapping
    public Page<CompraDtos.CompraDto> listar(@RequestParam(required = false) String q,
                                             @PageableDefault(size = 20) Pageable pageable) {
        return compras.listar(q, pageable);
    }

    @GetMapping("/{id}")
    public CompraDtos.CompraDto obtener(@PathVariable Long id) {
        return compras.obtener(id);
    }

    @PostMapping
    public ResponseEntity<CompraDtos.CompraDto> crear(@Valid @RequestBody CompraDtos.CompraRequest req,
            @AuthenticationPrincipal UsuarioTokenAuthentication principal, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(compras.crear(req, principal.getUsuarioId(), AuthController.ip(http)));
    }

    @PatchMapping("/{id}/estado")
    public CompraDtos.CompraDto cambiarEstado(@PathVariable Long id, @RequestParam EstadoCompra estado,
            @AuthenticationPrincipal UsuarioTokenAuthentication principal, HttpServletRequest http) {
        return compras.cambiarEstado(id, estado, principal.getUsuarioId(), AuthController.ip(http));
    }

    @PostMapping("/{id}/recepcion")
    public CompraDtos.CompraDto recibir(@PathVariable Long id,
            @AuthenticationPrincipal UsuarioTokenAuthentication principal, HttpServletRequest http) {
        return compras.recibir(id, principal.getUsuarioId(), AuthController.ip(http));
    }
}
