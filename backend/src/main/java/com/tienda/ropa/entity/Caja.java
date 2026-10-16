package com.tienda.ropa.entity;

import com.tienda.ropa.enums.EstadoCaja;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "caja")
public class Caja {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false, unique = true, length = 30)
    private String numero;

    @Column(name = "abierto_por", nullable = false)
    private Long abiertoPor;

    @Column(name = "cerrado_por")
    private Long cerradoPor;

    @Column(name = "apertura", nullable = false)
    private OffsetDateTime apertura = OffsetDateTime.now();

    @Column(name = "cierre")
    private OffsetDateTime cierre;

    @Column(name = "fondo_inicial", nullable = false, precision = 12, scale = 2)
    private BigDecimal fondoInicial = BigDecimal.ZERO;

    @Column(name = "efectivo_esperado", precision = 12, scale = 2)
    private BigDecimal efectivoEsperado;

    @Column(name = "efectivo_contado", precision = 12, scale = 2)
    private BigDecimal efectivoContado;

    @Column(name = "diferencia", precision = 12, scale = 2)
    private BigDecimal diferencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    private EstadoCaja estado = EstadoCaja.ABIERTA;

    @Column(name = "observaciones", columnDefinition = "text")
    private String observaciones;

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public Long getAbiertoPor() { return abiertoPor; }
    public void setAbiertoPor(Long abiertoPor) { this.abiertoPor = abiertoPor; }
    public Long getCerradoPor() { return cerradoPor; }
    public void setCerradoPor(Long cerradoPor) { this.cerradoPor = cerradoPor; }
    public OffsetDateTime getApertura() { return apertura; }
    public OffsetDateTime getCierre() { return cierre; }
    public void setCierre(OffsetDateTime cierre) { this.cierre = cierre; }
    public BigDecimal getFondoInicial() { return fondoInicial; }
    public void setFondoInicial(BigDecimal fondoInicial) { this.fondoInicial = fondoInicial; }
    public BigDecimal getEfectivoEsperado() { return efectivoEsperado; }
    public void setEfectivoEsperado(BigDecimal efectivoEsperado) { this.efectivoEsperado = efectivoEsperado; }
    public BigDecimal getEfectivoContado() { return efectivoContado; }
    public void setEfectivoContado(BigDecimal efectivoContado) { this.efectivoContado = efectivoContado; }
    public BigDecimal getDiferencia() { return diferencia; }
    public void setDiferencia(BigDecimal diferencia) { this.diferencia = diferencia; }
    public EstadoCaja getEstado() { return estado; }
    public void setEstado(EstadoCaja estado) { this.estado = estado; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
