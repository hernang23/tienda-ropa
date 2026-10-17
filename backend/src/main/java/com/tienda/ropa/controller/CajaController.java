package com.tienda.ropa.controller;

import com.tienda.ropa.dto.CajaDtos;
import com.tienda.ropa.security.UsuarioTokenAuthentication;
import com.tienda.ropa.service.CajaService;
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

/** Caja: apertura, movimientos manuales autorizados, cierre y consulta de jornadas. */
@RestController
@RequestMapping("/api/v1/caja")
public class CajaController {

    private final CajaService caja;

    public CajaController(CajaService caja) { this.caja = caja; }

    @GetMapping
    public Page<CajaDtos.CajaDto> listar(@PageableDefault(size = 20) Pageable pageable) {
        return caja.listar(pageable);
    }

    @GetMapping("/abierta")
    public ResponseEntity<CajaDtos.CajaDto> abierta() {
        CajaDtos.CajaDto dto = caja.cajaAbierta();
        return dto == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}")
    public CajaDtos.CajaDto obtener(@PathVariable Long id) {
        return caja.obtener(id);
    }

    @PostMapping("/abrir")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<CajaDtos.CajaDto> abrir(@Valid @RequestBody CajaDtos.AperturaCajaRequest req,
            @AuthenticationPrincipal UsuarioTokenAuthentication principal, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(caja.abrir(req, principal.getUsuarioId(), AuthController.ip(http)));
    }

    @PostMapping("/{cajaId}/movimientos")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public CajaDtos.CajaDto movimientoManual(@PathVariable Long cajaId,
            @Valid @RequestBody CajaDtos.MovimientoManualRequest req,
            @AuthenticationPrincipal UsuarioTokenAuthentication principal, HttpServletRequest http) {
        return caja.registrarMovimientoManual(cajaId, req, principal.getUsuarioId(), AuthController.ip(http));
    }

    @PostMapping("/{cajaId}/cerrar")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public CajaDtos.CajaDto cerrar(@PathVariable Long cajaId,
            @Valid @RequestBody CajaDtos.CierreCajaRequest req,
            @AuthenticationPrincipal UsuarioTokenAuthentication principal, HttpServletRequest http) {
        return caja.cerrar(cajaId, req, principal.getUsuarioId(), AuthController.ip(http));
    }
}
