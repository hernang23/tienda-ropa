package com.tienda.ropa.service;

import com.tienda.ropa.dto.InventarioDtos;
import com.tienda.ropa.entity.MovimientoInventario;
import com.tienda.ropa.entity.VarianteProducto;
import com.tienda.ropa.enums.TipoMovimiento;
import com.tienda.ropa.exception.BusinessException;
import com.tienda.ropa.exception.InsufficientStockException;
import com.tienda.ropa.mapper.Mappers;
import com.tienda.ropa.repository.MovimientoInventarioRepository;
import com.tienda.ropa.repository.VarianteProductoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Corazon del control de existencias. Toda variacion de stock pasa por aqui,
 * con bloqueo pesimista de la variante y movimiento auditable obligatorio.
 */
@Service
public class InventarioService {

    private final VarianteProductoRepository variantes;
    private final MovimientoInventarioRepository movimientos;
    private final Mappers mappers;

    public InventarioService(VarianteProductoRepository variantes, MovimientoInventarioRepository movimientos, Mappers mappers) {
        this.variantes = variantes;
        this.movimientos = movimientos;
        this.mappers = mappers;
    }

    /** Aplica una variacion de stock con bloqueo pesimista y registra el movimiento. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MovimientoInventario aplicarMovimiento(Long varianteId, TipoMovimiento tipo, int cantidadAbsoluta,
                                                  String motivo, Long usuarioId,
                                                  Long ventaId, Long compraId, Long devolucionId) {
        if (cantidadAbsoluta <= 0 && tipo != TipoMovimiento.AJUSTE) {
            throw new BusinessException("La cantidad del movimiento debe ser positiva");
        }
        VarianteProducto v = variantes.findByIdForUpdate(varianteId)
                .orElseThrow(() -> new BusinessException("La variante no existe"));

        int antes = v.getExistencias();
        int delta = switch (tipo) {
            case ENTRADA, DEVOLUCION -> cantidadAbsoluta;
            case SALIDA -> -cantidadAbsoluta;
            case AJUSTE -> cantidadAbsoluta == 0 ? 0 : (motivo.startsWith("__NEG__") ? -Math.abs(cantidadAbsoluta) : Math.abs(cantidadAbsoluta));
        };
        // Para AJUSTE el servicio de ajustes pasa el signo en la cantidad con prefijo de motivo controlado
        if (tipo == TipoMovimiento.AJUSTE) {
            delta = cantidadAbsoluta; // ya firmado por el llamador
            if (antes + delta < 0) {
                throw new InsufficientStockException("El ajuste dejaria el inventario en negativo (" + (antes + delta) + ")");
            }
        } else if (antes + delta < 0) {
            throw new InsufficientStockException(
                "Inventario insuficiente para " + v.getProducto().getNombre() + " " + v.getTalla() + "/" + v.getColor()
                + ". Disponible: " + antes + ", solicitado: " + cantidadAbsoluta);
        }

        int despues = antes + delta;
        v.setExistencias(despues);
        variantes.save(v);

        MovimientoInventario m = new MovimientoInventario();
        m.setVariante(v);
        m.setTipo(tipo);
        m.setCantidad(tipo == TipoMovimiento.AJUSTE ? delta : cantidadAbsoluta);
        m.setExistenciasAntes(antes);
        m.setExistenciasDespues(despues);
        m.setMotivo(motivo);
        m.setUsuarioId(usuarioId);
        m.setVentaId(ventaId);
        m.setCompraId(compraId);
        m.setDevolucionId(devolucionId);
        return movimientos.save(m);
    }

    @Transactional
    public InventarioDtos.MovimientoDto ajustar(Long varianteId, InventarioDtos.AjusteRequest req, Long usuarioId) {
        if (req.delta() == 0) {
            throw new BusinessException("El ajuste no puede ser cero");
        }
        MovimientoInventario m = aplicarMovimiento(varianteId, TipoMovimiento.AJUSTE, req.delta(),
                req.motivo(), usuarioId, null, null, null);
        return mappers.aMovimientoDto(m);
    }

    @Transactional(readOnly = true)
    public Page<InventarioDtos.MovimientoDto> historial(Long varianteId, TipoMovimiento tipo, Pageable pageable) {
        return movimientos.historial(varianteId, tipo, pageable).map(mappers::aMovimientoDto);
    }

    @Transactional(readOnly = true)
    public Page<com.tienda.ropa.dto.CatalogoDtos.VarianteDto> consultarInventario(String q, String talla, String color,
                                                                                 Boolean bajoMinimo, Boolean agotado, Pageable pageable) {
        return variantes.buscarInventario(q, talla, color, bajoMinimo, agotado, pageable).map(mappers::aVarianteDto);
    }

    @Transactional(readOnly = true)
    public List<com.tienda.ropa.dto.CatalogoDtos.VarianteDto> stockBajo() {
        return variantes.encontrarStockBajo().stream().map(mappers::aVarianteDto).toList();
    }

    @Transactional(readOnly = true)
    public List<com.tienda.ropa.dto.CatalogoDtos.VarianteDto> agotados() {
        return variantes.encontrarAgotados().stream().map(mappers::aVarianteDto).toList();
    }

    @Transactional(readOnly = true)
    public InventarioDtos.ResumenInventarioDto resumen() {
        BigDecimal valor = variantes.valorInventarioAlCosto();
        long unidades = variantes.findAll().stream().filter(VarianteProducto::isActivo)
                .mapToLong(VarianteProducto::getExistencias).sum();
        long bajo = variantes.encontrarStockBajo().size();
        long agot = variantes.encontrarAgotados().size();
        return new InventarioDtos.ResumenInventarioDto(valor, unidades, bajo, agot);
    }
}
