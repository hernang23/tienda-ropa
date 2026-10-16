package com.tienda.ropa.entity;

import com.tienda.ropa.enums.EstadoDevolucion;
import com.tienda.ropa.enums.TipoDevolucion;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "devolucion")
public class Devolucion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false, unique = true, length = 30)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @Column(name = "fecha", nullable = false)
    private OffsetDateTime fecha = OffsetDateTime.now();

    @Column(name = "motivo", nullable = false, length = 250)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 15)
    private TipoDevolucion tipo = TipoDevolucion.REEMBOLSO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    private EstadoDevolucion estado = EstadoDevolucion.REGISTRADA;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "total_reembolsado", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalReembolsado = BigDecimal.ZERO;

    @Column(name = "clave_idempotencia", unique = true, length = 64)
    private String claveIdempotencia;

    @OneToMany(mappedBy = "devolucion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleDevolucion> detalles = new ArrayList<>();

    public void agregarDetalle(DetalleDevolucion d) {
        d.setDevolucion(this);
        detalles.add(d);
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public Venta getVenta() { return venta; }
    public void setVenta(Venta venta) { this.venta = venta; }
    public OffsetDateTime getFecha() { return fecha; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public TipoDevolucion getTipo() { return tipo; }
    public void setTipo(TipoDevolucion tipo) { this.tipo = tipo; }
    public EstadoDevolucion getEstado() { return estado; }
    public void setEstado(EstadoDevolucion estado) { this.estado = estado; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public BigDecimal getTotalReembolsado() { return totalReembolsado; }
    public void setTotalReembolsado(BigDecimal totalReembolsado) { this.totalReembolsado = totalReembolsado; }
    public String getClaveIdempotencia() { return claveIdempotencia; }
    public void setClaveIdempotencia(String claveIdempotencia) { this.claveIdempotencia = claveIdempotencia; }
    public List<DetalleDevolucion> getDetalles() { return detalles; }
}
