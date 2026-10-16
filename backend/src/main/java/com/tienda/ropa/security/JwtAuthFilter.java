package com.tienda.ropa.security;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tienda.ropa.repository.TokenRevocadoRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final TokenRevocadoRepository revocados;
    private final ObjectMapper mapper = new ObjectMapper();

    public JwtAuthFilter(JwtService jwtService, TokenRevocadoRepository revocados) {
        this.jwtService = jwtService;
        this.revocados = revocados;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(PREFIX)) {
            chain.doFilter(request, response);
            return;
        }
        try {
            Claims claims = jwtService.parsear(header.substring(PREFIX.length()));
            if (revocados.existsByJti(claims.getId())) {
                rechazar(response, "La sesion fue cerrada. Inicie sesion nuevamente.");
                return;
            }
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            Long uid = ((Number) claims.get("uid")).longValue();
            UsuarioTokenAuthentication auth = new UsuarioTokenAuthentication(claims.getSubject(), uid, roles);
            request.setAttribute("AUTH_HEADER_SET", Boolean.TRUE);
            SecurityContext ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(auth);
            SecurityContextHolder.setContext(ctx);
            chain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            rechazar(response, "El token es invalido o expiro. Inicie sesion nuevamente.");
        }
    }

    private void rechazar(HttpServletResponse response, String mensaje) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write(mapper.writeValueAsString(
                java.util.Map.of("status", 401, "message", mensaje)));
    }
}
