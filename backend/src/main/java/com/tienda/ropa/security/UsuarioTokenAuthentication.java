package com.tienda.ropa.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

/** Autenticacion autenticada por JWT que ademas lleva el id interno del usuario. */
public class UsuarioTokenAuthentication extends AbstractAuthenticationToken {

    private final String username;
    private final Long usuarioId;

    public UsuarioTokenAuthentication(String username, Long usuarioId, List<String> roles) {
        super(roles.stream().map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r)).toList());
        this.username = username;
        this.usuarioId = usuarioId;
        setAuthenticated(true);
    }

    @Override public Object getCredentials() { return null; }
    @Override public Object getPrincipal() { return username; }
    public Long getUsuarioId() { return usuarioId; }
}
