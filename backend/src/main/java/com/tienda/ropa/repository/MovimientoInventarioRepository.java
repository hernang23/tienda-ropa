package com.tienda.ropa.repository;

import com.tienda.ropa.entity.MovimientoInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
    @Query("""
        SELECT m FROM MovimientoInventario m
        JOIN FETCH m.variante v JOIN FETCH v.producto p
        WHERE (:varianteId IS NULL OR v.id = :varianteId)
          AND (:tipo IS NULL OR m.tipo = :tipo)
        ORDER BY m.fecha DESC
        """)
    Page<MovimientoInventario> historial(@Param("varianteId") Long varianteId,
                                         @Param("tipo") com.tienda.ropa.enums.TipoMovimiento tipo,
                                         Pageable pageable);
}
