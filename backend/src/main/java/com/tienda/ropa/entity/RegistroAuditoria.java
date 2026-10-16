package com.tienda.ropa.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "registro_auditoria")
public class RegistroAuditoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "accion", nullable = false, length = 60)
    private String accion;

    @Column(name = "entidad", length = 60)
    private String entidad;

    @Column(name = "entidad_id", length = 40)
    private String entidadId;

    @Column(name = "detalle", columnDefinition = "text")
    private String detalle;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "fecha", nullable = false)
    private OffsetDateTime fecha = OffsetDateTime.now();

    public RegistroAuditoria() { }

    public RegistroAuditoria(Long usuarioId, String accion, String entidad, String entidadId, String detalle, String ip) {
        this.usuarioId = usuarioId;
        this.accion = accion;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.detalle = detalle;
        this.ip = ip;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public String getAccion() { return accion; }
    public String getEntidad() { return entidad; }
    public String getEntidadId() { return entidadId; }
    public String getDetalle() { return detalle; }
    public OffsetDateTime getFecha() { return fecha; }
}
