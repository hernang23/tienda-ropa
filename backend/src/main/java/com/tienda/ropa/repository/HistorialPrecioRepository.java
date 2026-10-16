package com.tienda.ropa.repository;

import com.tienda.ropa.entity.HistorialPrecio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistorialPrecioRepository extends JpaRepository<HistorialPrecio, Long> {
    List<HistorialPrecio> findTop50ByVarianteIdOrderByCreadoEnDesc(Long varianteId);
}
