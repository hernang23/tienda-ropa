package com.tienda.ropa.repository;

import com.tienda.ropa.entity.Rol;
import com.tienda.ropa.enums.RolNombre;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {
    Optional<Rol> findByNombre(RolNombre nombre);
}
