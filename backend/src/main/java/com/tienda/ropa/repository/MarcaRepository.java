package com.tienda.ropa.repository;

import com.tienda.ropa.entity.Marca;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MarcaRepository extends JpaRepository<Marca, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
    List<Marca> findByActivoTrueOrderByNameAsc();
}
