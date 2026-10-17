package com.tienda.ropa.controller;

import com.tienda.ropa.dto.AuditoriaDtos;
import com.tienda.ropa.dto.ReporteDtos;
import com.tienda.ropa.entity.RegistroAuditoria;
import com.tienda.ropa.security.UsuarioTokenAuthentication;
import com.tienda.ropa.service.ConfiguracionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Configuración de la tienda (solo admin puede modificar) y auditoría (solo admin puede leer). */
@RestController
@RequestMapping("/api/v1")
public class ConfiguracionController {

    private final ConfiguracionService configuracion;
    private final com.tienda.ropa.service.AuditoriaService auditoria;

    public ConfiguracionController(ConfiguracionService configuracion,
                                   com.tienda.ropa.service.AuditoriaService auditoria) {
        this.configuracion = configuracion;
        this.auditoria = auditoria;
    }

    @GetMapping("/configuracion")
    public ReporteDtos.ConfiguracionDto obtener() {
        return configuracion.obtener();
    }

    @PutMapping("/configuracion")
    @PreAuthorize("hasRole('ADMIN')")
    public ReporteDtos.ConfiguracionDto actualizar(@Valid @RequestBody ReporteDtos.ConfiguracionRequest req,
            @AuthenticationPrincipal UsuarioTokenAuthentication principal, HttpServletRequest http) {
        return configuracion.actualizar(req, principal.getUsuarioId(), AuthController.ip(http));
    }

    @GetMapping("/auditoria")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AuditoriaDtos.RegistroAuditoriaDto> auditoria(@PageableDefault(size = 50) Pageable pageable) {
        return auditoria.listar(pageable).map(this::aDto);
    }

    private AuditoriaDtos.RegistroAuditoriaDto aDto(RegistroAuditoria r) {
        return new AuditoriaDtos.RegistroAuditoriaDto(r.getId(), r.getUsuarioId(), r.getAccion(), r.getEntidad(),
                r.getEntidadId(), r.getDetalle(), r.getIp(), r.getFecha());
    }
}
