package com.tienda.ropa.service;

import com.tienda.ropa.dto.AuthDtos;
import com.tienda.ropa.entity.Rol;
import com.tienda.ropa.entity.TokenRevocado;
import com.tienda.ropa.entity.Usuario;
import com.tienda.ropa.enums.RolNombre;
import com.tienda.ropa.exception.BusinessException;
import com.tienda.ropa.exception.ConflictException;
import com.tienda.ropa.exception.ResourceNotFoundException;
import com.tienda.ropa.mapper.Mappers;
import com.tienda.ropa.repository.RolRepository;
import com.tienda.ropa.repository.TokenRevocadoRepository;
import com.tienda.ropa.repository.UsuarioRepository;
import com.tienda.ropa.security.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final TokenRevocadoRepository revocados;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditoriaService auditoria;
    private final Mappers mappers;

    public AuthService(UsuarioRepository usuarios, RolRepository roles, TokenRevocadoRepository revocados,
                       PasswordEncoder encoder, JwtService jwt, AuditoriaService auditoria, Mappers mappers) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.revocados = revocados;
        this.encoder = encoder;
        this.jwt = jwt;
        this.auditoria = auditoria;
        this.mappers = mappers;
    }

    @Transactional
    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest req, String ip) {
        Usuario u = usuarios.findByUsername(req.username()).orElse(null);
        // Mensaje uniforme: no revelar si el usuario existe
        if (u == null || !encoder.matches(req.password(), u.getPasswordHash())) {
            auditoria.registrar(u == null ? null : u.getId(), "LOGIN_FALLIDO", "usuario",
                    u == null ? null : String.valueOf(u.getId()), "Credenciales invalidas", ip);
            throw new BusinessException("Usuario o contrasena incorrectos");
        }
        if (!u.isActivo()) {
            throw new ConflictException("La cuenta esta desactivada. Contacte al administrador.");
        }
        u.setUltimoAcceso(OffsetDateTime.now());
        usuarios.save(u);
        List<String> roleNames = u.getRoles().stream().map(r -> r.getNombre().name()).sorted().toList();
        String token = jwt.generar(u.getUsername(), u.getId(), roleNames);
        auditoria.registrar(u.getId(), "LOGIN", "usuario", String.valueOf(u.getId()), "Inicio de sesion correcto", ip);
        return new AuthDtos.LoginResponse(token, mappers.aUsuarioDto(u));
    }

    @Transactional
    public void logout(String bearerToken, String ip) {
        try {
            Claims claims = jwt.parsear(bearerToken);
            TokenRevocado t = new TokenRevocado();
            t.setJti(claims.getId());
            t.setExpiraEn(claims.getExpiration().toInstant());
            revocados.save(t);
            auditoria.registrar(((Number) claims.get("uid")).longValue(), "LOGOUT", "usuario",
                    String.valueOf(((Number) claims.get("uid")).longValue()), "Cierre de sesion", ip);
        } catch (Exception ex) {
            throw new BusinessException("Token invalido");
        }
    }

    @Transactional(readOnly = true)
    public AuthDtos.UsuarioDto perfil(Long usuarioId) {
        return mappers.aUsuarioDto(obtener(usuarioId));
    }

    @Transactional
    public void cambiarPassword(Long usuarioId, AuthDtos.CambioPasswordRequest req, String ip) {
        Usuario u = obtener(usuarioId);
        if (!encoder.matches(req.passwordActual(), u.getPasswordHash())) {
            throw new BusinessException("La contrasena actual no es correcta");
        }
        u.setPasswordHash(encoder.encode(req.passwordNuevo()));
        u.setActualizadoEn(OffsetDateTime.now());
        usuarios.save(u);
        auditoria.registrar(usuarioId, "CAMBIO_PASSWORD", "usuario", String.valueOf(usuarioId),
                "Contraseña actualizada", ip);
    }

    public Usuario obtener(Long id) {
        return usuarios.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    @Transactional(readOnly = true)
    public Page<AuthDtos.UsuarioDto> listar(Pageable pageable) {
        return usuarios.findAll(pageable).map(mappers::aUsuarioDto);
    }

    @Transactional(readOnly = true)
    public Page<AuthDtos.UsuarioDto> buscar(String q, Pageable pageable) {
        return usuarios.buscar(q == null || q.isBlank() ? null : q, pageable).map(mappers::aUsuarioDto);
    }

    @Transactional
    public AuthDtos.UsuarioDto crear(AuthDtos.CrearUsuarioRequest req, String ip) {
        if (usuarios.existsByUsernameIgnoreCase(req.username())) {
            throw new ConflictException("El nombre de usuario ya existe");
        }
        Usuario u = new Usuario();
        u.setUsername(req.username());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setNombreCompleto(req.nombreCompleto());
        u.setCorreo(req.correo());
        u.setRoles(resolverRoles(req.roles()));
        usuarios.save(u);
        auditoria.registrar(null, "USUARIO_CREAR", "usuario", String.valueOf(u.getId()),
                "Se creo el usuario " + u.getUsername(), ip);
        return mappers.aUsuarioDto(u);
    }

    @Transactional
    public AuthDtos.UsuarioDto actualizar(Long id, AuthDtos.ActualizarUsuarioRequest req, String ip) {
        Usuario u = obtener(id);
        u.setNombreCompleto(req.nombreCompleto());
        u.setCorreo(req.correo());
        if (req.roles() != null && !req.roles().isEmpty()) {
            u.setRoles(resolverRoles(req.roles()));
        }
        u.setActualizadoEn(OffsetDateTime.now());
        usuarios.save(u);
        auditoria.registrar(null, "USUARIO_EDITAR", "usuario", String.valueOf(id), "Datos actualizados", ip);
        return mappers.aUsuarioDto(u);
    }

    @Transactional
    public AuthDtos.UsuarioDto activarDesactivar(Long id, boolean activo, String ip) {
        Usuario u = obtener(id);
        if (!activo && esAdminUnico(u)) {
            throw new BusinessException("No se puede desactivar al ultimo administrador activo");
        }
        u.setActivo(activo);
        u.setActualizadoEn(OffsetDateTime.now());
        usuarios.save(u);
        auditoria.registrar(null, activo ? "USUARIO_ACTIVAR" : "USUARIO_DESACTIVAR", "usuario",
                String.valueOf(id), "Estado cambiado a " + activo, ip);
        return mappers.aUsuarioDto(u);
    }

    private boolean esAdminUnico(Usuario u) {
        boolean esAdmin = u.getRoles().stream().anyMatch(r -> r.getNombre() == RolNombre.ADMIN);
        if (!esAdmin) return false;
        long adminsActivos = usuarios.findAll().stream()
                .filter(x -> x.isActivo() && x.getRoles().stream().anyMatch(r -> r.getNombre() == RolNombre.ADMIN))
                .count();
        return adminsActivos <= 1;
    }

    private Set<Rol> resolverRoles(List<String> nombres) {
        Set<Rol> resultado = new HashSet<>();
        for (String n : nombres) {
            RolNombre rn;
            try { rn = RolNombre.valueOf(n.toUpperCase()); }
            catch (IllegalArgumentException ex) { throw new BusinessException("Rol desconocido: " + n); }
            resultado.add(roles.findByNombre(rn)
                    .orElseThrow(() -> new BusinessException("El rol " + n + " no esta configurado")));
        }
        if (resultado.isEmpty()) throw new BusinessException("Asigne al menos un rol");
        return resultado;
    }
}
