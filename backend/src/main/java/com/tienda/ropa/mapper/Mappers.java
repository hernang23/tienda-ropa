package com.tienda.ropa.mapper;

import com.tienda.ropa.dto.*;
import com.tienda.ropa.entity.*;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/** Mappers explicitos Entidad -> DTO. Nunca se exponen entidades JPA en la API. */
@Component
public class Mappers {

    private static String fmt(OffsetDateTime dt) {
        return dt == null ? null : dt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public AuthDtos.UsuarioDto aUsuarioDto(Usuario u) {
        List<String> roles = u.getRoles().stream().map(r -> r.getNombre().name()).sorted().collect(Collectors.toList());
        return new AuthDtos.UsuarioDto(u.getId(), u.getUsername(), u.getNombreCompleto(), u.getCorreo(),
                u.isActivo(), roles, fmt(u.getUltimoAcceso()), fmt(u.getCreadoEn()));
    }

    public CatalogoDtos.CategoriaDto aCategoriaDto(Categoria c) {
        return new CatalogoDtos.CategoriaDto(c.getId(), c.getNombre(), c.getDescripcion(), c.isActivo(), fmt(c.getCreadoEn()));
    }

    public CatalogoDtos.MarcaDto aMarcaDto(Marca m) {
        return new CatalogoDtos.MarcaDto(m.getId(), m.getNombre(), m.getDescripcion(), m.isActivo(), fmt(m.getCreadoEn()));
    }

    public CatalogoDtos.VarianteDto aVarianteDto(VarianteProducto v) {
        return new CatalogoDtos.VarianteDto(v.getId(), v.getProducto().getId(), v.getSku(), v.getCodigoBarras(),
                v.getTalla(), v.getColor(), v.getPrecioVenta(), v.getCostoUnitario(), v.getExistencias(),
                v.getStockMinimo(), v.isActivo(), v.getExistencias() <= v.getStockMinimo(), v.getExistencias() == 0);
    }

    public CatalogoDtos.ProductoDto aProductoDto(Producto p, List<VarianteProducto> variantes) {
        int totalEx = variantes.stream().mapToInt(VarianteProducto::getExistencias).sum();
        return new CatalogoDtos.ProductoDto(p.getId(), p.getNombre(), p.getDescripcion(),
                p.getCategoria() == null ? null : p.getCategoria().getId(),
                p.getCategoria() == null ? null : p.getCategoria().getNombre(),
                p.getMarca() == null ? null : p.getMarca().getId(),
                p.getMarca() == null ? null : p.getMarca().getNombre(),
                p.getGenero(), p.getTipoPrenda(), p.getTemporada(), p.isActivo(), p.getImagenUrl(),
                fmt(p.getCreadoEn()), fmt(p.getActualizadoEn()),
                variantes.stream().map(this::aVarianteDto).collect(Collectors.toList()), totalEx);
    }

    public PersonaDtos.ProveedorDto aProveedorDto(Proveedor p) {
        return new PersonaDtos.ProveedorDto(p.getId(), p.getNombre(), p.getNit(), p.getContacto(), p.getTelefono(),
                p.getCorreo(), p.getDireccion(), p.getCiudad(), p.getObservaciones(), p.isActivo(), fmt(p.getCreadoEn()));
    }

    public PersonaDtos.ClienteDto aClienteDto(Cliente c) {
        return new PersonaDtos.ClienteDto(c.getId(), c.getTipo().name(), c.getNombre(), c.getDocumento(), c.getCorreo(),
                c.getTelefono(), c.getDireccion(), c.isActivo(), fmt(c.getCreadoEn()));
    }

    public CompraDtos.DetalleCompraDto aDetalleCompraDto(DetalleCompra d) {
        VarianteProducto v = d.getVariante();
        return new CompraDtos.DetalleCompraDto(d.getId(), v.getId(), v.getSku(), v.getProducto().getNombre(),
                v.getTalla(), v.getColor(), d.getCantidad(), d.getCostoUnitario(), d.getDescuento(),
                d.getImpuestos(), d.getSubtotal());
    }

    public CompraDtos.CompraDto aCompraDto(Compra c, List<DetalleCompra> detalles) {
        return new CompraDtos.CompraDto(c.getId(), c.getNumero(), c.getProveedor().getId(), c.getProveedor().getNombre(),
                c.getEstado().name(), fmt(c.getFechaRegistro()), fmt(c.getFechaRecepcion()), c.getSubtotal(),
                c.getDescuento(), c.getImpuestos(), c.getTotal(), c.getObservaciones(), c.getUsuarioId(),
                detalles.stream().map(this::aDetalleCompraDto).collect(Collectors.toList()));
    }

    public VentaDtos.DetalleVentaDto aDetalleVentaDto(DetalleVenta d) {
        VarianteProducto v = d.getVariante();
        return new VentaDtos.DetalleVentaDto(d.getId(), v.getId(), v.getSku(), v.getProducto().getNombre(),
                v.getTalla(), v.getColor(), d.getCantidad(), d.getPrecioUnitario(), d.getDescuento(),
                d.getImpuestos(), d.getSubtotal(), d.getDevuelto());
    }

    public VentaDtos.PagoDto aPagoDto(PagoVenta p) {
        return new VentaDtos.PagoDto(p.getId(), p.getMetodo().name(), p.getMonto(), p.getReferencia(),
                p.getEstado(), fmt(p.getFecha()));
    }

    public VentaDtos.VentaDto aVentaDto(Venta v, List<DetalleVenta> detalles, List<PagoVenta> pagos) {
        return new VentaDtos.VentaDto(v.getId(), v.getNumero(),
                v.getCliente() == null ? null : v.getCliente().getId(),
                v.getCliente() == null ? null : v.getCliente().getNombre(),
                v.getUsuarioId(), fmt(v.getFecha()), v.getSubtotal(), v.getDescuento(), v.getImpuestos(),
                v.getTotal(), v.getEstado().name(), v.getEstadoPago().name(), v.getObservaciones(),
                detalles.stream().map(this::aDetalleVentaDto).collect(Collectors.toList()),
                pagos.stream().map(this::aPagoDto).collect(Collectors.toList()));
    }

    public InventarioDtos.MovimientoDto aMovimientoDto(MovimientoInventario m) {
        VarianteProducto v = m.getVariante();
        return new InventarioDtos.MovimientoDto(m.getId(), v.getId(), v.getSku(), v.getProducto().getNombre(),
                v.getTalla(), v.getColor(), m.getTipo().name(), m.getCantidad(), m.getExistenciasAntes(),
                m.getExistenciasDespues(), m.getMotivo(), m.getUsuarioId(), fmt(m.getFecha()),
                m.getVentaId(), m.getCompraId(), m.getDevolucionId());
    }

    public DevolucionDtos.DetalleDevolucionDto aDetalleDevolucionDto(DetalleDevolucion d) {
        VarianteProducto v = d.getDetalleVenta().getVariante();
        return new DevolucionDtos.DetalleDevolucionDto(d.getId(), d.getDetalleVenta().getId(), v.getSku(),
                v.getProducto().getNombre(), v.getTalla(), v.getColor(), d.getCantidad(), d.isRegresaInventario(),
                d.getEstadoFisico(), d.getMontoReembolso(), d.getObservacion());
    }

    public DevolucionDtos.DevolucionDto aDevolucionDto(Devolucion dv, List<DetalleDevolucion> detalles) {
        return new DevolucionDtos.DevolucionDto(dv.getId(), dv.getNumero(), dv.getVenta().getId(),
                dv.getVenta().getNumero(), fmt(dv.getFecha()), dv.getMotivo(), dv.getTipo().name(),
                dv.getEstado().name(), dv.getUsuarioId(), dv.getTotalReembolsado(),
                detalles.stream().map(this::aDetalleDevolucionDto).collect(Collectors.toList()));
    }

    public CajaDtos.MovimientoCajaDto aMovimientoCajaDto(MovimientoCaja m) {
        return new CajaDtos.MovimientoCajaDto(m.getId(), m.getTipo().name(), m.getMonto(), m.getMotivo(),
                fmt(m.getFecha()), m.getUsuarioId());
    }

    public CajaDtos.CajaDto aCajaDto(Caja c, List<MovimientoCaja> movimientos, List<CajaDtos.PagoResumenMetodoDto> porMetodo) {
        return new CajaDtos.CajaDto(c.getId(), c.getNumero(), c.getAbiertoPor(), c.getCerradoPor(),
                fmt(c.getApertura()), fmt(c.getCierre()), c.getFondoInicial(), c.getEfectivoEsperado(),
                c.getEfectivoContado(), c.getDiferencia(), c.getEstado().name(), c.getObservaciones(),
                movimientos.stream().map(this::aMovimientoCajaDto).collect(Collectors.toList()), porMetodo);
    }

    public ReporteDtos.ConfiguracionDto aConfiguracionDto(ConfiguracionTienda cfg) {
        return new ReporteDtos.ConfiguracionDto(cfg.getId(), cfg.getNombreComercial(), cfg.getLogoUrl(),
                cfg.getDireccion(), cfg.getTelefono(), cfg.getCorreo(), cfg.getNit(), cfg.getMoneda(),
                cfg.getZonaHoraria(), cfg.getIvaPorcentaje(), cfg.getDescuentoMaxVendedor(),
                cfg.getDescuentoMaxAdmin(), cfg.getStockMinimoDefault(), cfg.getMetodosPago(),
                cfg.isPermitirClienteOcasional(), cfg.getTextoComprobante());
    }
}
