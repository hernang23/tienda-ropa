package com.tienda.ropa.controller;

import com.tienda.ropa.dto.PersonaDtos;
import com.tienda.ropa.security.UsuarioTokenAuthentication;
import com.tienda.ropa.service.PersonaService;
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

/** Proveedores y clientes. */
@RestController
@RequestMapping("/api/v1")
public class PersonaController {

    private final PersonaService personas;

    public PersonaController(PersonaService personas) { this.personas = personas; }

    // ----- Proveedores -----

    @GetMapping("/proveedores")
    public Page<PersonaDtos.ProveedorDto> proveedores(@RequestParam(required = false) String q,
                                                      @PageableDefault(size = 20) Pageable pageable) {
        return personas.listarProveedores(q, pageable);
    }

    @GetMapping("/proveedores/{id}")
    public PersonaDtos.ProveedorDto proveedor(@PathVariable Long id) {
        return personas.obtenerProveedor(id);
    }

    @PostMapping("/proveedores")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTARIO')")
    public ResponseEntity<PersonaDtos.ProveedorDto> crearProveedor(@Valid @RequestBody PersonaDtos.ProveedorRequest req,
            @AuthenticationPrincipal UsuarioTokenAuthentication principal, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(personas.crearProveedor(req, principal.getUsuarioId(), AuthController.ip(http)));
    }

    @PutMapping("/proveedores/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTARIO')")
    public PersonaDtos.ProveedorDto actualizarProveedor(@PathVariable Long id,
            @Valid @RequestBody PersonaDtos.ProveedorRequest req,
            @AuthenticationPrincipal UsuarioTokenAuthentication principal, HttpServletRequest http) {
        return personas.actualizarProveedor(id, req, principal.getUsuarioId(), AuthController.ip(http));
    }

    @GetMapping("/proveedores/{id}/compras")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTARIO')")
    public Page<com.tienda.ropa.dto.CompraDtos.CompraDto> comprasProveedor(@PathVariable Long id,
                                                                            @PageableDefault(size = 20) Pageable pageable) {
        return personas.comprasDeProveedor(id, pageable);
    }

    // ----- Clientes -----

    @GetMapping("/clientes")
    public Page<PersonaDtos.ClienteDto> clientes(@RequestParam(required = false) String q,
                                                 @PageableDefault(size = 20) Pageable pageable) {
        return personas.listarClientes(q, pageable);
    }

    @GetMapping("/clientes/{id}")
    public PersonaDtos.ClienteDto cliente(@PathVariable Long id) {
        return personas.obtenerCliente(id);
    }

    @PostMapping("/clientes")
    public ResponseEntity<PersonaDtos.ClienteDto> crearCliente(@Valid @RequestBody PersonaDtos.ClienteRequest req,
            @AuthenticationPrincipal UsuarioTokenAuthentication principal, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(personas.crearCliente(req, principal.getUsuarioId(), AuthController.ip(http)));
    }

    @PutMapping("/clientes/{id}")
    public PersonaDtos.ClienteDto actualizarCliente(@PathVariable Long id,
            @Valid @RequestBody PersonaDtos.ClienteRequest req,
            @AuthenticationPrincipal UsuarioTokenAuthentication principal, HttpServletRequest http) {
        return personas.actualizarCliente(id, req, principal.getUsuarioId(), AuthController.ip(http));
    }

    @GetMapping("/clientes/{id}/historial")
    public Page<PersonaService.VentaResumenDto> historialCliente(@PathVariable Long id,
                                                                  @PageableDefault(size = 20) Pageable pageable) {
        return personas.historialCliente(id, pageable);
    }
}
