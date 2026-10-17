package com.tienda.ropa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "configuracion_tienda")
public class ConfiguracionTienda {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_comercial", nullable = false, length = 150)
    private String nombreComercial;

    @Column(name = "logo_url", length = 400)
    private String logoUrl;

    @Column(name = "direccion", length = 200)
    private String direccion;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "correo", length = 120)
    private String correo;

    @Column(name = "nit", length = 20)
    private String nit;

    @Column(name = "moneda", nullable = false, length = 10)
    private String moneda = "COP";

    @Column(name = "zona_horaria", nullable = false, length = 50)
    private String zonaHoraria = "America/Bogota";

    @Column(name = "iva_porcentaje", nullable = false, precision = 5, scale = 2)
    private BigDecimal ivaPorcentaje = new BigDecimal("19.00");

    @Column(name = "descuento_max_vendedor", nullable = false, precision = 5, scale = 2)
    private BigDecimal descuentoMaxVendedor = new BigDecimal("10.00");

    @Column(name = "descuento_max_admin", nullable = false, precision = 5, scale = 2)
    private BigDecimal descuentoMaxAdmin = new BigDecimal("50.00");

    @Column(name = "stock_minimo_default", nullable = false)
    private int stockMinimoDefault = 3;

    @Column(name = "metodos_pago", nullable = false, length = 200)
    private String metodosPago = "EFECTIVO,TRANSFERENCIA,TARJETA,OTRO";

    @Column(name = "permitir_cliente_ocasional", nullable = false)
    private boolean permitirClienteOcasional = true;

    @Column(name = "texto_comprobante", columnDefinition = "text")
    private String textoComprobante;

    public Long getId() { return id; }
    public String getNombreComercial() { return nombreComercial; }
    public void setNombreComercial(String nombreComercial) { this.nombreComercial = nombreComercial; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getNit() { return nit; }
    public void setNit(String nit) { this.nit = nit; }
    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    public String getZonaHoraria() { return zonaHoraria; }
    public void setZonaHoraria(String zonaHoraria) { this.zonaHoraria = zonaHoraria; }
    public BigDecimal getIvaPorcentaje() { return ivaPorcentaje; }
    public void setIvaPorcentaje(BigDecimal ivaPorcentaje) { this.ivaPorcentaje = ivaPorcentaje; }
    public BigDecimal getDescuentoMaxVendedor() { return descuentoMaxVendedor; }
    public void setDescuentoMaxVendedor(BigDecimal v) { this.descuentoMaxVendedor = v; }
    public BigDecimal getDescuentoMaxAdmin() { return descuentoMaxAdmin; }
    public void setDescuentoMaxAdmin(BigDecimal v) { this.descuentoMaxAdmin = v; }
    public int getStockMinimoDefault() { return stockMinimoDefault; }
    public void setStockMinimoDefault(int v) { this.stockMinimoDefault = v; }
    public String getMetodosPago() { return metodosPago; }
    public void setMetodosPago(String metodosPago) { this.metodosPago = metodosPago; }
    public boolean isPermitirClienteOcasional() { return permitirClienteOcasional; }
    public void setPermitirClienteOcasional(boolean v) { this.permitirClienteOcasional = v; }
    public String getTextoComprobante() { return textoComprobante; }
    public void setTextoComprobante(String textoComprobante) { this.textoComprobante = textoComprobante; }
}
