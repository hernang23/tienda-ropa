package com.tienda.ropa.controller;

import com.tienda.ropa.dto.AuthDtos;
import com.tienda.ropa.security.UsuarioTokenAuthentication;
import com.tienda.ropa.service.AuthService;
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

/** Autenticación y gestión de usuarios. */
@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/auth/login")
    public AuthDtos.LoginResponse login(@Valid @RequestBody AuthDtos.LoginRequest req, HttpServletRequest http) {
        return auth.login(req, ip(http));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String bearer, HttpServletRequest http) {
        auth.logout(bearer, ip(http));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/auth/perfil")
    public AuthDtos.UsuarioDto perfil(@AuthenticationPrincipal UsuarioTokenAuthentication principal) {
        return auth.perfil(principal.getUsuarioId());
    }

    @PutMapping("/auth/password")
    public ResponseEntity<Void> cambiarPassword(@AuthenticationPrincipal UsuarioTokenAuthentication principal,
                                                @Valid @RequestBody AuthDtos.CambioPasswordRequest req,
                                                HttpServletRequest http) {
        auth.cambiarPassword(principal.getUsuarioId(), req, ip(http));
        return ResponseEntity.noContent().build();
    }

    // ----- Gestión de usuarios (solo administrador) -----

    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AuthDtos.UsuarioDto> listar(@RequestParam(required = false) String q,
                                             @PageableDefault(size = 20) Pageable pageable) {
        if (q != null && !q.isBlank()) return auth.buscar(q, pageable);
        return auth.listar(pageable);
    }

    @PostMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthDtos.UsuarioDto> crear(@Valid @RequestBody AuthDtos.CrearUsuarioRequest req,
                                                     HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.crear(req, ip(http)));
    }

    @PutMapping("/usuarios/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AuthDtos.UsuarioDto actualizar(@PathVariable Long id,
                                          @Valid @RequestBody AuthDtos.ActualizarUsuarioRequest req,
                                          HttpServletRequest http) {
        return auth.actualizar(id, req, ip(http));
    }

    @PatchMapping("/usuarios/{id}/activo")
    @PreAuthorize("hasRole('ADMIN')")
    public AuthDtos.UsuarioDto activarDesactivar(@PathVariable Long id, @RequestParam boolean activo,
                                                 HttpServletRequest http) {
        return auth.activarDesactivar(id, activo, ip(http));
    }

    static String ip(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
