package com.tienda.ropa.controller;

import com.tienda.ropa.dto.VentaDtos;
import com.tienda.ropa.security.UsuarioTokenAuthentication;
import com.tienda.ropa.service.VentaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Punto de venta, historial de ventas, pagos y anulación. */
@RestController
@RequestMapping("/api/v1/ventas")
public class VentaController {

    private final VentaService ventas;

    public VentaController(VentaService ventas) { this.ventas = ventas; }

    @GetMapping
    public Page<VentaDtos.VentaDto> listar(@RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String estado,
            @PageableDefault(size = 20) Pageable pageable) {
        return ventas.listar(q, desde, hasta, estado, pageable);
    }

    @GetMapping("/{id}")
    public VentaDtos.VentaDto obtener(@PathVariable Long id) {
        return ventas.obtener(id);
    }

    @GetMapping("/numero/{numero}")
    public VentaDtos.VentaDto porNumero(@PathVariable String numero) {
        return ventas.porNumero(numero);
    }

    /** Calcula totales en el servidor para mostrar al usuario antes de confirmar (no persiste nada). */
    @PostMapping("/simular")
    public Map<String, Object> simular(@Valid @RequestBody VentaDtos.VentaRequest req,
                                       @AuthenticationPrincipal UsuarioTokenAuthentication principal) {
        return ventas.simular(req, principal.getUsuarioId());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<VentaDtos.VentaDto> crear(@Valid @RequestBody VentaDtos.VentaRequest req,
            @AuthenticationPrincipal UsuarioTokenAuthentication principal, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ventas.crear(req, principal.getUsuarioId(), AuthController.ip(http)));
    }

    @PostMapping("/{ventaId}/pagos")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public VentaDtos.VentaDto registrarPagos(@PathVariable Long ventaId,
            @Valid @RequestBody List<VentaDtos.PagoRequest> pagos,
            @AuthenticationPrincipal UsuarioTokenAuthentication principal, HttpServletRequest http) {
        return ventas.registrarPagos(ventaId, pagos, principal.getUsuarioId(), AuthController.ip(http));
    }

    @PostMapping("/{ventaId}/anular")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public VentaDtos.VentaDto anular(@PathVariable Long ventaId,
            @Valid @RequestBody VentaDtos.AnularVentaRequest req,
            @AuthenticationPrincipal UsuarioTokenAuthentication principal, HttpServletRequest http) {
        boolean esAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ventas.anular(ventaId, req.motivo(), principal.getUsuarioId(), esAdmin, AuthController.ip(http));
    }
}
