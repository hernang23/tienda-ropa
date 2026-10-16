package com.tienda.ropa.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class PersonaDtos {
    private PersonaDtos() { }

    public record ProveedorDto(Long id, String nombre, String nit, String contacto, String telefono,
                               String correo, String direccion, String ciudad, String observaciones,
                               boolean activo, String creadoEn) { }

    public record ProveedorRequest(
        @NotBlank @Size(max = 150) String nombre,
        @Pattern(regexp = "^$|^\\d{9,20}(-\\d{1,2})?$", message = "El NIT debe contener digitos (opcional digito de verificacion)") String nit,
        @Size(max = 120) String contacto,
        @Pattern(regexp = "^$|^[+\\d\\s()-]{7,30}$", message = "Telefono invalido") String telefono,
        @Email String correo,
        @Size(max = 200) String direccion,
        @Size(max = 80) String ciudad,
        @Size(max = 1000) String observaciones,
        Boolean activo
    ) { }

    public record ClienteDto(Long id, String tipo, String nombre, String documento, String correo,
                             String telefono, String direccion, boolean activo, String creadoEn) { }

    public record ClienteRequest(
        @NotBlank String tipo,
        @NotBlank @Size(max = 150) String nombre,
        @Pattern(regexp = "^$|^[\\d\\.\\-]{6,20}$", message = "Documento invalido") String documento,
        @Email String correo,
        @Pattern(regexp = "^$|^[+\\d\\s()-]{7,30}$", message = "Telefono invalido") String telefono,
        @Size(max = 200) String direccion,
        Boolean activo
    ) { }
}
