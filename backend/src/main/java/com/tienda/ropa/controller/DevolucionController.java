package com.tienda.ropa.controller;

import com.tienda.ropa.dto.DevolucionDtos;
import com.tienda.ropa.security.UsuarioTokenAuthentication;
import com.tienda.ropa.service.DevolucionService;
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

/** Devoluciones de ventas: registro con idempotencia, consulta y anulación (solo admin). */
@RestController
@RequestMapping("/api/v1/devoluciones")
public class DevolucionController {

    private final DevolucionService devoluciones;

    public DevolucionController(DevolucionService devoluciones) { this.devoluciones = devoluciones; }

    @GetMapping
    public Page<DevolucionDtos.DevolucionDto> listar(@PageableDefault(size = 20) Pageable pageable) {
        return devoluciones.listar(pageable);
    }

    @GetMapping("/{id}")
    public DevolucionDtos.DevolucionDto obtener(@PathVariable Long id) {
        return devoluciones.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<DevolucionDtos.DevolucionDto> registrar(@Valid @RequestBody DevolucionDtos.DevolucionRequest req,
            @AuthenticationPrincipal UsuarioTokenAuthentication principal, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(devoluciones.registrar(req, principal.getUsuarioId(), AuthController.ip(http)));
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasRole('ADMIN')")
    public DevolucionDtos.DevolucionDto anular(@PathVariable Long id,
            @AuthenticationPrincipal UsuarioTokenAuthentication principal, HttpServletRequest http) {
        boolean esAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return devoluciones.anular(id, principal.getUsuarioId(), esAdmin, AuthController.ip(http));
    }
}
