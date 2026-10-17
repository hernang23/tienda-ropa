package com.tienda.ropa.dto;

import java.time.OffsetDateTime;

public final class AuditoriaDtos {
    private AuditoriaDtos() { }

    public record RegistroAuditoriaDto(Long id, Long usuarioId, String accion, String entidad,
                                       String entidadId, String detalle, String ip, OffsetDateTime fecha) { }
}
