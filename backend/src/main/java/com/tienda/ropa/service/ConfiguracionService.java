package com.tienda.ropa.service;

import com.tienda.ropa.dto.ReporteDtos;
import com.tienda.ropa.entity.ConfiguracionTienda;
import com.tienda.ropa.exception.BusinessException;
import com.tienda.ropa.mapper.Mappers;
import com.tienda.ropa.repository.ConfiguracionTiendaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/** Configuracion maestra de la tienda (fila unica id=1 sembrada por Flyway). */
@Service
public class ConfiguracionService {

    private static final List<String> METODOS_VALIDOS =
            List.of("EFECTIVO", "TRANSFERENCIA", "TARJETA", "OTRO");

    private final ConfiguracionTiendaRepository repo;
    private final Mappers mappers;
    private final AuditoriaService auditoria;

    public ConfiguracionService(ConfiguracionTiendaRepository repo, Mappers mappers, AuditoriaService auditoria) {
        this.repo = repo;
        this.mappers = mappers;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public ConfiguracionTienda entidad() {
        return repo.findById(1L).orElseThrow(() -> new BusinessException("La configuracion de la tienda no esta inicializada"));
    }

    @Transactional(readOnly = true)
    public ReporteDtos.ConfiguracionDto obtener() {
        return mappers.aConfiguracionDto(entidad());
    }

    @Transactional
    public ReporteDtos.ConfiguracionDto actualizar(ReporteDtos.ConfiguracionRequest req, Long usuarioId, String ip) {
        ConfiguracionTienda c = entidad();
        if (req.nombreComercial() != null && !req.nombreComercial().isBlank()) c.setNombreComercial(req.nombreComercial().trim());
        c.setLogoUrl(req.logoUrl());
        c.setDireccion(req.direccion());
        c.setTelefono(req.telefono());
        c.setCorreo(req.correo());
        c.setNit(req.nit());
        if (req.ivaPorcentaje() != null) {
            if (req.ivaPorcentaje().compareTo(BigDecimal.ZERO) < 0 || req.ivaPorcentaje().compareTo(new BigDecimal("99.99")) > 0) {
                throw new BusinessException("El porcentaje de IVA debe estar entre 0 y 99.99");
            }
            c.setIvaPorcentaje(req.ivaPorcentaje());
        }
        if (req.descuentoMaxVendedor() != null) validarPorcentaje(req.descuentoMaxVendedor(), "descuento maximo del vendedor");
        if (req.descuentoMaxVendedor() != null) c.setDescuentoMaxVendedor(req.descuentoMaxVendedor());
        if (req.descuentoMaxAdmin() != null) validarPorcentaje(req.descuentoMaxAdmin(), "descuento maximo del administrador");
        if (req.descuentoMaxAdmin() != null) c.setDescuentoMaxAdmin(req.descuentoMaxAdmin());
        if (c.getDescuentoMaxVendedor().compareTo(c.getDescuentoMaxAdmin()) > 0) {
            throw new BusinessException("El descuento maximo del vendedor no puede superar al del administrador");
        }
        if (req.stockMinimoDefault() != null) {
            if (req.stockMinimoDefault() < 0) throw new BusinessException("El stock minimo no puede ser negativo");
            c.setStockMinimoDefault(req.stockMinimoDefault());
        }
        if (req.metodosPago() != null && !req.metodosPago().isBlank()) {
            List<String> metodos = Arrays.stream(req.metodosPago().split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).map(String::toUpperCase).toList();
            if (metodos.isEmpty() || !METODOS_VALIDOS.containsAll(metodos)) {
                throw new BusinessException("Metodos de pago validos: " + String.join(", ", METODOS_VALIDOS));
            }
            c.setMetodosPago(String.join(",", metodos));
        }
        if (req.permitirClienteOcasional() != null) c.setPermitirClienteOcasional(req.permitirClienteOcasional());
        c.setTextoComprobante(req.textoComprobante());
        repo.save(c);
        auditoria.registrar(usuarioId, "CONFIG_ACTUALIZAR", "configuracion", "1", "Configuracion de la tienda actualizada", ip);
        return mappers.aConfiguracionDto(c);
    }

    private void validarPorcentaje(BigDecimal v, String nombre) {
        if (v.compareTo(BigDecimal.ZERO) < 0 || v.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException("El " + nombre + " debe estar entre 0 y 100");
        }
    }
}
