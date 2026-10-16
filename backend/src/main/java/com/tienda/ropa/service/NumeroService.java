package com.tienda.ropa.service;

import com.tienda.ropa.repository.CajaRepository;
import com.tienda.ropa.repository.CompraRepository;
import com.tienda.ropa.repository.DevolucionRepository;
import com.tienda.ropa.repository.VentaRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generacion de numeros consecutivos por dia (VENT-20260417-0001).
 * La unicidad final la garantiza la restriccion UNIQUE de la base de datos;
 * ante colision por concurrencia, el reintento del llamador obtiene el siguiente numero.
 */
@Service
public class NumeroService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final VentaRepository ventas;
    private final CompraRepository compras;
    private final DevolucionRepository devoluciones;
    private final CajaRepository cajas;

    public NumeroService(VentaRepository ventas, CompraRepository compras,
                         DevolucionRepository devoluciones, CajaRepository cajas) {
        this.ventas = ventas;
        this.compras = compras;
        this.devoluciones = devoluciones;
        this.cajas = cajas;
    }

    private String prefijoFechas() { return LocalDate.now().format(FMT); }

    public synchronized String nuevoNumeroVenta() {
        return "VENT-" + prefijoFechas() + "-" + String.format("%04d", ventas.ultimoNumeroSecuencial() + 1);
    }

    public synchronized String nuevoNumeroCompra() {
        return "CMP-" + prefijoFechas() + "-" + String.format("%04d", compras.ultimoNumeroSecuencial() + 1);
    }

    public synchronized String nuevoNumeroDevolucion() {
        return "DEV-" + prefijoFechas() + "-" + String.format("%04d", devoluciones.ultimoNumeroSecuencial() + 1);
    }

    public synchronized String nuevoNumeroCaja() {
        return "CAJA-" + prefijoFechas() + "-" + String.format("%04d", cajas.ultimoNumeroSecuencial() + 1);
    }
}
