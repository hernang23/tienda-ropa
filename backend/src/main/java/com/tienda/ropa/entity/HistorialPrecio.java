package com.tienda.ropa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "historial_precio")
public class HistorialPrecio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variante_id", nullable = false)
    private VarianteProducto variante;

    @Column(name = "precio_anterior", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioAnterior;

    @Column(name = "precio_nuevo", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioNuevo;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    public HistorialPrecio(VarianteProducto variante, BigDecimal anterior, BigDecimal nuevo, Long usuarioId) {
        this.variante = variante;
        this.precioAnterior = anterior;
        this.precioNuevo = nuevo;
        this.usuarioId = usuarioId;
    }

    public Long getId() { return id; }
    public VarianteProducto getVariante() { return variante; }
    public BigDecimal getPrecioAnterior() { return precioAnterior; }
    public BigDecimal getPrecioNuevo() { return precioNuevo; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
}
