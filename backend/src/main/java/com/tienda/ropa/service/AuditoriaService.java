package com.tienda.ropa.service;

import com.tienda.ropa.entity.RegistroAuditoria;
import com.tienda.ropa.repository.RegistroAuditoriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Registro de eventos sensibles. Nunca guarda contrasenas, tokens ni datos financieros completos. */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);
    private final RegistroAuditoriaRepository repo;

    public AuditoriaService(RegistroAuditoriaRepository repo) { this.repo = repo; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Long usuarioId, String accion, String entidad, String entidadId, String detalle, String ip) {
        try {
            repo.save(new RegistroAuditoria(usuarioId, accion, entidad, entidadId, detalle, ip));
        } catch (Exception ex) {
            // La auditoria no debe tumbar la operacion de negocio, pero si deja rastro tecnico
            log.error("No se pudo registrar auditoria para accion {}: {}", accion, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<RegistroAuditoria> listar(Pageable pageable) {
        return repo.findAllByOrderByIdDesc(pageable);
    }
}
