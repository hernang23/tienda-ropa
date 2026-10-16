package com.tienda.ropa.entity;

import com.tienda.ropa.enums.EstadoPagoVenta;
import com.tienda.ropa.enums.EstadoVenta;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "venta")
public class Venta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false, unique = true, length = 30)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "fecha", nullable = false)
    private OffsetDateTime fecha = OffsetDateTime.now();

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "descuento", nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento;

    @Column(name = "impuestos", nullable = false, precision = 12, scale = 2)
    private BigDecimal impuestos;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    private EstadoVenta estado = EstadoVenta.COMPLETADA;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false, length = 15)
    private EstadoPagoVenta estadoPago = EstadoPagoVenta.PAGADO;

    @Column(name = "observaciones", columnDefinition = "text")
    private String observaciones;

    @Column(name = "caja_id")
    private Long cajaId;

    @Column(name = "clave_idempotencia", unique = true, length = 64)
    private String claveIdempotencia;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL)
    private List<PagoVenta> pagos = new ArrayList<>();

    public void agregarDetalle(DetalleVenta d) {
        d.setVenta(this);
        detalles.add(d);
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public OffsetDateTime getFecha() { return fecha; }
    public void setFecha(OffsetDateTime fecha) { this.fecha = fecha; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal descuento) { this.descuento = descuento; }
    public BigDecimal getImpuestos() { return impuestos; }
    public void setImpuestos(BigDecimal impuestos) { this.impuestos = impuestos; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public EstadoVenta getEstado() { return estado; }
    public void setEstado(EstadoVenta estado) { this.estado = estado; }
    public EstadoPagoVenta getEstadoPago() { return estadoPago; }
    public void setEstadoPago(EstadoPagoVenta estadoPago) { this.estadoPago = estadoPago; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public Long getCajaId() { return cajaId; }
    public void setCajaId(Long cajaId) { this.cajaId = cajaId; }
    public String getClaveIdempotencia() { return claveIdempotencia; }
    public void setClaveIdempotencia(String claveIdempotencia) { this.claveIdempotencia = claveIdempotencia; }
    public List<DetalleVenta> getDetalles() { return detalles; }
    public List<PagoVenta> getPagos() { return pagos; }
}
