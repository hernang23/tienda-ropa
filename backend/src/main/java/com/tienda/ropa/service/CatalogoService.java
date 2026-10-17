package com.tienda.ropa.service;

import com.tienda.ropa.dto.CatalogoDtos;
import com.tienda.ropa.entity.*;
import com.tienda.ropa.exception.BusinessException;
import com.tienda.ropa.exception.ConflictException;
import com.tienda.ropa.exception.ResourceNotFoundException;
import com.tienda.ropa.mapper.Mappers;
import com.tienda.ropa.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Catalogo: productos, variantes, categorias y marcas. */
@Service
public class CatalogoService {

    private final ProductoRepository productos;
    private final VarianteProductoRepository variantes;
    private final MovimientoInventarioRepository movimientos;
    private final CategoriaRepository categorias;
    private final MarcaRepository marcas;
    private final HistorialPrecioRepository historialPrecios;
    private final ConfiguracionService configuracion;
    private final AuditoriaService auditoria;
    private final Mappers mappers;

    public CatalogoService(ProductoRepository productos, VarianteProductoRepository variantes,
                           MovimientoInventarioRepository movimientos,
                           CategoriaRepository categorias, MarcaRepository marcas,
                           HistorialPrecioRepository historialPrecios, ConfiguracionService configuracion,
                           AuditoriaService auditoria, Mappers mappers) {
        this.productos = productos;
        this.variantes = variantes;
        this.movimientos = movimientos;
        this.categorias = categorias;
        this.marcas = marcas;
        this.historialPrecios = historialPrecios;
        this.configuracion = configuracion;
        this.auditoria = auditoria;
        this.mappers = mappers;
    }

    // ---------- Categorias ----------

    @Transactional(readOnly = true)
    public List<CatalogoDtos.CategoriaDto> listarCategorias(boolean soloActivas) {
        return (soloActivas ? categorias.findByActivoTrueOrderByNameAsc() : categorias.findAll())
                .stream().map(mappers::aCategoriaDto).toList();
    }

    @Transactional
    public CatalogoDtos.CategoriaDto crearCategoria(CatalogoDtos.NombreDescActivoRequest req, Long usuarioId, String ip) {
        if (categorias.existsByNombreIgnoreCase(req.nombre())) {
            throw new ConflictException("Ya existe una categoria con ese nombre");
        }
        Categoria c = new Categoria();
        c.setNombre(req.nombre().trim());
        c.setDescripcion(req.descripcion());
        c.setActivo(req.activo() == null || req.activo());
        categorias.save(c);
        auditoria.registrar(usuarioId, "CATEGORIA_CREAR", "categoria", String.valueOf(c.getId()), c.getNombre(), ip);
        return mappers.aCategoriaDto(c);
    }

    @Transactional
    public CatalogoDtos.CategoriaDto actualizarCategoria(Long id, CatalogoDtos.NombreDescActivoRequest req, Long usuarioId, String ip) {
        Categoria c = categorias.findById(id).orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));
        if (!c.getNombre().equalsIgnoreCase(req.nombre()) && categorias.existsByNombreIgnoreCase(req.nombre())) {
            throw new ConflictException("Ya existe una categoria con ese nombre");
        }
        c.setNombre(req.nombre().trim());
        c.setDescripcion(req.descripcion());
        if (req.activo() != null) c.setActivo(req.activo());
        categorias.save(c);
        auditoria.registrar(usuarioId, "CATEGORIA_EDITAR", "categoria", String.valueOf(id), c.getNombre(), ip);
        return mappers.aCategoriaDto(c);
    }

    // ---------- Marcas ----------

    @Transactional(readOnly = true)
    public List<CatalogoDtos.MarcaDto> listarMarcas(boolean soloActivas) {
        return (soloActivas ? marcas.findByActivoTrueOrderByNameAsc() : marcas.findAll())
                .stream().map(mappers::aMarcaDto).toList();
    }

    @Transactional
    public CatalogoDtos.MarcaDto crearMarca(CatalogoDtos.NombreDescActivoRequest req, Long usuarioId, String ip) {
        if (marcas.existsByNombreIgnoreCase(req.nombre())) {
            throw new ConflictException("Ya existe una marca con ese nombre");
        }
        Marca m = new Marca();
        m.setNombre(req.nombre().trim());
        m.setDescripcion(req.descripcion());
        m.setActivo(req.activo() == null || req.activo());
        marcas.save(m);
        auditoria.registrar(usuarioId, "MARCA_CREAR", "marca", String.valueOf(m.getId()), m.getNombre(), ip);
        return mappers.aMarcaDto(m);
    }

    @Transactional
    public CatalogoDtos.MarcaDto actualizarMarca(Long id, CatalogoDtos.NombreDescActivoRequest req, Long usuarioId, String ip) {
        Marca m = marcas.findById(id).orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada"));
        if (!m.getNombre().equalsIgnoreCase(req.nombre()) && marcas.existsByNombreIgnoreCase(req.nombre())) {
            throw new ConflictException("Ya existe una marca con ese nombre");
        }
        m.setNombre(req.nombre().trim());
        m.setDescripcion(req.descripcion());
        if (req.activo() != null) m.setActivo(req.activo());
        marcas.save(m);
        auditoria.registrar(usuarioId, "MARCA_EDITAR", "marca", String.valueOf(id), m.getNombre(), ip);
        return mappers.aMarcaDto(m);
    }

    // ---------- Productos ----------

    @Transactional(readOnly = true)
    public Page<CatalogoDtos.ProductoDto> listarProductos(String q, Long categoriaId, Long marcaId,
                                                           Boolean soloActivos, Pageable pageable) {
        Page<Producto> page = productos.buscar(blankToNull(q), categoriaId, marcaId, soloActivos, pageable);
        return page.map(p -> mappers.aProductoDto(p, variantes.findByProductoIdOrderByTallaAscColorAsc(p.getId())));
    }

    @Transactional(readOnly = true)
    public CatalogoDtos.ProductoDto obtenerProducto(Long id) {
        Producto p = productos.findById(id).orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        return mappers.aProductoDto(p, variantes.findByProductoIdOrderByTallaAscColorAsc(id));
    }

    @Transactional
    public CatalogoDtos.ProductoDto crearProducto(CatalogoDtos.ProductoCrearRequest req, Long usuarioId, String ip) {
        Producto p = new Producto();
        aplicarDatosProducto(p, req.nombre(), req.descripcion(), req.categoriaId(), req.marcaId(),
                req.genero(), req.tipoPrenda(), req.temporada(), req.imagenUrl(), null);
        productos.save(p);

        if (req.variantes() != null) {
            for (CatalogoDtos.VarianteCrearRequest vr : req.variantes()) {
                crearVarianteEn(p, vr, usuarioId);
            }
        }
        auditoria.registrar(usuarioId, "PRODUCTO_CREAR", "producto", String.valueOf(p.getId()), p.getNombre(), ip);
        return mappers.aProductoDto(p, variantes.findByProductoIdOrderByTallaAscColorAsc(p.getId()));
    }

    @Transactional
    public CatalogoDtos.ProductoDto actualizarProducto(Long id, CatalogoDtos.ProductoActualizarRequest req, Long usuarioId, String ip) {
        Producto p = productos.findById(id).orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        aplicarDatosProducto(p, req.nombre(), req.descripcion(), req.categoriaId(), req.marcaId(),
                req.genero(), req.tipoPrenda(), req.temporada(), req.imagenUrl(), req.activo());
        p.setActualizadoEn(OffsetDateTime.now());
        productos.save(p);
        auditoria.registrar(usuarioId, "PRODUCTO_EDITAR", "producto", String.valueOf(id), p.getNombre(), ip);
        return mappers.aProductoDto(p, variantes.findByProductoIdOrderByTallaAscColorAsc(id));
    }

    /** Desactivacion logica: los productos con historia comercial nunca se eliminan fisicamente. */
    @Transactional
    public void desactivarProducto(Long id, Long usuarioId, String ip) {
        Producto p = productos.findById(id).orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        p.setActivo(false);
        p.setActualizadoEn(OffsetDateTime.now());
        productos.save(p);
        auditoria.registrar(usuarioId, "PRODUCTO_DESACTIVAR", "producto", String.valueOf(id), p.getNombre(), ip);
    }

    private void aplicarDatosProducto(Producto p, String nombre, String descripcion, Long categoriaId, Long marcaId,
                                      String genero, String tipoPrenda, String temporada, String imagenUrl, Boolean activo) {
        p.setNombre(nombre.trim());
        p.setDescripcion(descripcion);
        p.setCategoria(categoriaId == null ? null : categorias.findById(categoriaId)
                .orElseThrow(() -> new BusinessException("La categoria indicada no existe")));
        p.setMarca(marcaId == null ? null : marcas.findById(marcaId)
                .orElseThrow(() -> new BusinessException("La marca indicada no existe")));
        p.setGenero(genero);
        p.setTipoPrenda(tipoPrenda);
        p.setTemporada(temporada);
        p.setImagenUrl(imagenUrl);
        if (activo != null) p.setActivo(activo);
    }

    // ---------- Variantes ----------

    @Transactional
    public CatalogoDtos.VarianteDto agregarVariante(Long productoId, CatalogoDtos.VarianteCrearRequest req, Long usuarioId, String ip) {
        Producto p = productos.findById(productoId).orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        VarianteProducto v = crearVarianteEn(p, req, usuarioId);
        auditoria.registrar(usuarioId, "VARIANTE_CREAR", "variante", String.valueOf(v.getId()), v.getSku(), ip);
        return mappers.aVarianteDto(v);
    }

    private VarianteProducto crearVarianteEn(Producto p, CatalogoDtos.VarianteCrearRequest req, Long usuarioId) {
        if (variantes.existsBySku(req.sku())) {
            throw new ConflictException("El SKU " + req.sku() + " ya esta registrado en otra variante");
        }
        if (variantes.existsByProductoIdAndTallaAndColorIgnoreCase(p.getId(), req.talla(), req.color())) {
            throw new ConflictException("El producto ya tiene una variante " + req.talla() + "/" + req.color());
        }
        VarianteProducto v = new VarianteProducto();
        v.setSku(req.sku().trim());
        v.setCodigoBarras(blankToNull(req.codigoBarras()));
        v.setTalla(req.talla().trim().toUpperCase());
        v.setColor(req.color().trim());
        v.setPrecioVenta(req.precioVenta());
        v.setCostoUnitario(req.costoUnitario());
        v.setExistencias(0); // las existencias iniciales se registran como ENTRADA de inventario auditable
        v.setStockMinimo(req.stockMinimo() != null ? req.stockMinimo() : configuracion.entidad().getStockMinimoDefault());
        v.setActivo(req.activo() == null || req.activo());
        p.agregarVariante(v);
        try {
            variantes.save(v);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("SKU o codigo de barras duplicado");
        }
        if (req.existenciasIniciales() > 0) {
            // Regla: el stock nunca se fija "a mano"; una existencia inicial es una ENTRADA auditable.
            MovimientoInventario mi = new MovimientoInventario();
            mi.setVariante(v);
            mi.setTipo(com.tienda.ropa.enums.TipoMovimiento.ENTRADA);
            mi.setCantidad(req.existenciasIniciales());
            mi.setExistenciasAntes(0);
            mi.setExistenciasDespues(req.existenciasIniciales());
            mi.setMotivo("Existencias iniciales del producto");
            mi.setUsuarioId(usuarioId);
            movimientos.save(mi);
        }
        return v;
    }

    @Transactional
    public CatalogoDtos.VarianteDto actualizarVariante(Long varianteId, CatalogoDtos.VarianteActualizarRequest req,
                                                       Long usuarioId, String ip) {
        VarianteProducto v = variantes.findById(varianteId)
                .orElseThrow(() -> new ResourceNotFoundException("Variante no encontrada"));
        BigDecimal precioAnterior = v.getPrecioVenta();
        v.setPrecioVenta(req.precioVenta());
        v.setCostoUnitario(req.costoUnitario());
        v.setCodigoBarras(blankToNull(req.codigoBarras()));
        if (req.stockMinimo() != null) v.setStockMinimo(req.stockMinimo());
        if (req.activo() != null) v.setActivo(req.activo());
        v.setActualizadoEn(OffsetDateTime.now());
        variantes.save(v);
        if (precioAnterior.compareTo(req.precioVenta()) != 0) {
            historialPrecios.save(new HistorialPrecio(v, precioAnterior, req.precioVenta(), usuarioId));
        }
        auditoria.registrar(usuarioId, "VARIANTE_EDITAR", "variante", String.valueOf(varianteId),
                v.getSku() + " precio=" + req.precioVenta(), ip);
        return mappers.aVarianteDto(v);
    }

    @Transactional(readOnly = true)
    public List<CatalogoDtos.VarianteDto> variantesDeProducto(Long productoId) {
        productos.findById(productoId).orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        return variantes.findByProductoIdOrderByTallaAscColorAsc(productoId).stream().map(mappers::aVarianteDto).toList();
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> historialPrecios(Long varianteId) {
        return historialPrecios.findTop50ByVarianteIdOrderByCreadoEnDesc(varianteId).stream()
                .map(h -> java.util.Map.<String, Object>of(
                        "fecha", h.getCreadoEn() == null ? "" : h.getCreadoEn().toString(),
                        "precioAnterior", h.getPrecioAnterior(),
                        "precioNuevo", h.getPrecioNuevo(),
                        "usuarioId", h.getUsuarioId() == null ? -1L : h.getUsuarioId()))
                .toList();
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
