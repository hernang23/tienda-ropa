package com.tienda.ropa.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Emision y validacion de tokens JWT (HS256). El secreto proviene de variable de entorno. */
@Service
public class JwtService {

    private final Key key;
    private final Duration expiracion;

    public JwtService(@Value("${app.jwt.secret}") String secretB64,
                      @Value("${app.jwt.expiracion-minutos:480}") long expMinutos) {
        byte[] decoded = Base64.getDecoder().decode(secretB64);
        if (decoded.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 256 bits (32 bytes)");
        }
        this.key = Keys.hmacShaKeyFor(decoded);
        this.expiracion = Duration.ofMinutes(expMinutos);
    }

    public String generar(String username, Long usuarioId, List<String> roles) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())               // jti para revocacion
                .setSubject(username)
                .addClaims(Map.of("uid", usuarioId, "roles", roles))
                .setIssuedAt(Date.from(ahora))
                .setExpiration(Date.from(ahora.plus(expiracion)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parsear(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
    }

    public Duration getExpiracion() { return expiracion; }
}
