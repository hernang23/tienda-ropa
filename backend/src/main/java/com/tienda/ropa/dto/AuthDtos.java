package com.tienda.ropa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class AuthDtos {
    private AuthDtos() { }

    public record LoginRequest(
        @NotBlank(message = "El usuario es obligatorio") String username,
        @NotBlank(message = "La contrasena es obligatoria") String password
    ) { }

    public record LoginResponse(String token, UsuarioDto usuario) { }

    public record CambioPasswordRequest(
        @NotBlank(message = "La contrasena actual es obligatoria") String passwordActual,
        @NotBlank @Size(min = 8, message = "La nueva contrasena debe tener al menos 8 caracteres") String passwordNuevo
    ) { }

    public record UsuarioDto(Long id, String username, String nombreCompleto, String correo,
                             boolean activo, List<String> roles, String ultimoAcceso, String creadoEn) { }

    public record CrearUsuarioRequest(
        @NotBlank @Size(min = 3, max = 60) String username,
        @NotBlank @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres") String password,
        @NotBlank String nombreCompleto,
        String correo,
        @NotBlank List<String> roles
    ) { }

    public record ActualizarUsuarioRequest(@NotBlank String nombreCompleto, String correo, List<String> roles) { }
}
