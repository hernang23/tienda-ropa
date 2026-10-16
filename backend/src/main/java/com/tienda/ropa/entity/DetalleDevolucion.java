package com.tienda.ropa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "detalle_devolucion")
public class DetalleDevolucion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "devolucion_id", nullable = false)
    private Devolucion devolucion;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "detalle_venta_id", nullable = false)
    private DetalleVenta detalleVenta;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Column(name = "regresa_inventario", nullable = false)
    private boolean regresaInventario = true;

    @Column(name = "estado_fisico", nullable = false, length = 20)
    private String estadoFisico = "BUENO";

    @Column(name = "monto_reembolso", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoReembolso = BigDecimal.ZERO;

    @Column(name = "observacion", length = 250)
    private String observacion;

    public Long getId() { return id; }
    public Devolucion getDevolucion() { return devolucion; }
    public void setDevolucion(Devolucion devolucion) { this.devolucion = devolucion; }
    public DetalleVenta getDetalleVenta() { return detalleVenta; }
    public void setDetalleVenta(DetalleVenta detalleVenta) { this.detalleVenta = detalleVenta; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public boolean isRegresaInventario() { return regresaInventario; }
    public void setRegresaInventario(boolean regresaInventario) { this.regresaInventario = regresaInventario; }
    public String getEstadoFisico() { return estadoFisico; }
    public void setEstadoFisico(String estadoFisico) { this.estadoFisico = estadoFisico; }
    public BigDecimal getMontoReembolso() { return montoReembolso; }
    public void setMontoReembolso(BigDecimal montoReembolso) { this.montoReembolso = montoReembolso; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
