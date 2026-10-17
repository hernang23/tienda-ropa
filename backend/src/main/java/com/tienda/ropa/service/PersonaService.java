package com.tienda.ropa.service;

import com.tienda.ropa.dto.PersonaDtos;
import com.tienda.ropa.entity.Cliente;
import com.tienda.ropa.entity.Proveedor;
import com.tienda.ropa.enums.TipoCliente;
import com.tienda.ropa.exception.BusinessException;
import com.tienda.ropa.exception.ConflictException;
import com.tienda.ropa.exception.ResourceNotFoundException;
import com.tienda.ropa.mapper.Mappers;
import com.tienda.ropa.repository.ClienteRepository;
import com.tienda.ropa.repository.CompraRepository;
import com.tienda.ropa.repository.DetalleCompraRepository;
import com.tienda.ropa.repository.ProveedorRepository;
import com.tienda.ropa.repository.VentaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Personas del negocio: proveedores y clientes. El borrado fisico no existe: se desactivan. */
@Service
public class PersonaService {

    private final ProveedorRepository proveedores;
    private final ClienteRepository clientes;
    private final CompraRepository compras;
    private final DetalleCompraRepository detalleCompras;
    private final VentaRepository ventas;
    private final AuditoriaService auditoria;
    private final Mappers mappers;

    public PersonaService(ProveedorRepository proveedores, ClienteRepository clientes,
                          CompraRepository compras, DetalleCompraRepository detalleCompras,
                          VentaRepository ventas,
                          AuditoriaService auditoria, Mappers mappers) {
        this.proveedores = proveedores;
        this.clientes = clientes;
        this.compras = compras;
        this.detalleCompras = detalleCompras;
        this.ventas = ventas;
        this.auditoria = auditoria;
        this.mappers = mappers;
    }

    // ---------- Proveedores ----------

    @Transactional(readOnly = true)
    public Page<PersonaDtos.ProveedorDto> listarProveedores(String q, Pageable pageable) {
        return proveedores.buscar(q == null || q.isBlank() ? null : q.trim(), pageable).map(mappers::aProveedorDto);
    }

    @Transactional(readOnly = true)
    public PersonaDtos.ProveedorDto obtenerProveedor(Long id) {
        return mappers.aProveedorDto(proveedor(id));
    }

    @Transactional
    public PersonaDtos.ProveedorDto crearProveedor(PersonaDtos.ProveedorRequest req, Long usuarioId, String ip) {
        String nit = blankToNull(req.nit());
        if (nit != null && proveedores.existsByNit(nit)) {
            throw new ConflictException("Ya existe un proveedor con ese NIT");
        }
        Proveedor p = new Proveedor();
        aplicar(p, req, nit);
        proveedores.save(p);
        auditoria.registrar(usuarioId, "PROVEEDOR_CREAR", "proveedor", String.valueOf(p.getId()), p.getNombre(), ip);
        return mappers.aProveedorDto(p);
    }

    @Transactional
    public PersonaDtos.ProveedorDto actualizarProveedor(Long id, PersonaDtos.ProveedorRequest req, Long usuarioId, String ip) {
        Proveedor p = proveedor(id);
        String nit = blankToNull(req.nit());
        if (nit != null && !nit.equals(p.getNit()) && proveedores.existsByNit(nit)) {
            throw new ConflictException("Ya existe un proveedor con ese NIT");
        }
        aplicar(p, req, nit);
        proveedores.save(p);
        auditoria.registrar(usuarioId, "PROVEEDOR_EDITAR", "proveedor", String.valueOf(id), p.getNombre(), ip);
        return mappers.aProveedorDto(p);
    }

    /** Historial de compras del proveedor (referencias históricas preservadas). */
    @Transactional(readOnly = true)
    public Page<com.tienda.ropa.dto.CompraDtos.CompraDto> comprasDeProveedor(Long id, Pageable pageable) {
        return compras.porProveedor(id, pageable)
                .map(c -> mappers.aCompraDto(c, detalleCompras.findByCompraId(c.getId())));
    }

    private void aplicar(Proveedor p, PersonaDtos.ProveedorRequest req, String nit) {
        p.setNombre(req.nombre().trim());
        p.setNit(nit);
        p.setContacto(blankToNull(req.contacto()));
        p.setTelefono(blankToNull(req.telefono()));
        p.setCorreo(blankToNull(req.correo()));
        p.setDireccion(blankToNull(req.direccion()));
        p.setCiudad(blankToNull(req.ciudad()));
        p.setObservaciones(req.observaciones());
        if (req.activo() != null) p.setActivo(req.activo());
    }

    private Proveedor proveedor(Long id) {
        return proveedores.findById(id).orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));
    }

    // ---------- Clientes ----------

    @Transactional(readOnly = true)
    public Page<PersonaDtos.ClienteDto> listarClientes(String q, Pageable pageable) {
        return clientes.buscar(q == null || q.isBlank() ? null : q.trim(), pageable).map(mappers::aClienteDto);
    }

    @Transactional(readOnly = true)
    public PersonaDtos.ClienteDto obtenerCliente(Long id) {
        return mappers.aClienteDto(cliente(id));
    }

    @Transactional
    public PersonaDtos.ClienteDto crearCliente(PersonaDtos.ClienteRequest req, Long usuarioId, String ip) {
        String doc = blankToNull(req.documento());
        if (doc != null && clientes.existsByDocumento(doc)) {
            throw new ConflictException("Ya existe un cliente con ese documento");
        }
        Cliente c = new Cliente();
        aplicar(c, req, doc);
        clientes.save(c);
        auditoria.registrar(usuarioId, "CLIENTE_CREAR", "cliente", String.valueOf(c.getId()), c.getNombre(), ip);
        return mappers.aClienteDto(c);
    }

    @Transactional
    public PersonaDtos.ClienteDto actualizarCliente(Long id, PersonaDtos.ClienteRequest req, Long usuarioId, String ip) {
        Cliente c = cliente(id);
        String doc = blankToNull(req.documento());
        if (doc != null && !doc.equals(c.getDocumento()) && clientes.existsByDocumento(doc)) {
            throw new ConflictException("Ya existe un cliente con ese documento");
        }
        aplicar(c, req, doc);
        clientes.save(c);
        auditoria.registrar(usuarioId, "CLIENTE_EDITAR", "cliente", String.valueOf(id), c.getNombre(), ip);
        return mappers.aClienteDto(c);
    }

    @Transactional(readOnly = true)
    public Page<VentaResumenDto> historialCliente(Long id, Pageable pageable) {
        cliente(id);
        return ventas.porCliente(id, pageable)
                .map(v -> new VentaResumenDto(v.getId(), v.getNumero(),
                        v.getFecha() == null ? null : v.getFecha().toString(),
                        v.getTotal(), v.getEstado().name()));
    }

    public record VentaResumenDto(Long id, String numero, String fecha,
                                  java.math.BigDecimal total, String estado) { }

    private void aplicar(Cliente c, PersonaDtos.ClienteRequest req, String doc) {
        TipoCliente tipo;
        try { tipo = TipoCliente.valueOf(req.tipo().toUpperCase()); }
        catch (IllegalArgumentException ex) { throw new BusinessException("Tipo de cliente invalido (NATURAL o EMPRESA)"); }
        c.setTipo(tipo);
        c.setNombre(req.nombre().trim());
        c.setDocumento(doc);
        c.setCorreo(blankToNull(req.correo()));
        c.setTelefono(blankToNull(req.telefono()));
        c.setDireccion(blankToNull(req.direccion()));
        if (req.activo() != null) c.setActivo(req.activo());
    }

    private Cliente cliente(Long id) {
        return clientes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
