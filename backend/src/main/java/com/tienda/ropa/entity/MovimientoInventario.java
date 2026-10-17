package com.tienda.ropa.entity;

import com.tienda.ropa.enums.TipoMovimiento;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "movimiento_inventario")
public class MovimientoInventario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "variante_id", nullable = false)
    private VarianteProducto variante;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 15)
    private TipoMovimiento tipo;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Column(name = "existencias_antes", nullable = false)
    private int existenciasAntes;

    @Column(name = "existencias_despues", nullable = false)
    private int existenciasDespues;

    @Column(name = "motivo", nullable = false, length = 250)
    private String motivo;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "fecha", nullable = false)
    private OffsetDateTime fecha = OffsetDateTime.now();

    @Column(name = "venta_id")
    private Long ventaId;

    @Column(name = "compra_id")
    private Long compraId;

    @Column(name = "devolucion_id")
    private Long devolucionId;

    public Long getId() { return id; }
    public VarianteProducto getVariante() { return variante; }
    public void setVariante(VarianteProducto variante) { this.variante = variante; }
    public TipoMovimiento getTipo() { return tipo; }
    public void setTipo(TipoMovimiento tipo) { this.tipo = tipo; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public int getExistenciasAntes() { return existenciasAntes; }
    public void setExistenciasAntes(int existenciasAntes) { this.existenciasAntes = existenciasAntes; }
    public int getExistenciasDespues() { return existenciasDespues; }
    public void setExistenciasDespues(int existenciasDespues) { this.existenciasDespues = existenciasDespues; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public OffsetDateTime getFecha() { return fecha; }
    public Long getVentaId() { return ventaId; }
    public void setVentaId(Long ventaId) { this.ventaId = ventaId; }
    public Long getCompraId() { return compraId; }
    public void setCompraId(Long compraId) { this.compraId = compraId; }
    public Long getDevolucionId() { return devolucionId; }
    public void setDevolucionId(Long devolucionId) { this.devolucionId = devolucionId; }
}
