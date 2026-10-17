package com.tienda.ropa.repository;

import com.tienda.ropa.entity.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @Query("""
        SELECT p FROM Producto p
        LEFT JOIN FETCH p.categoria c
        LEFT JOIN FETCH p.marca m
        WHERE (:q IS NULL OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:categoriaId IS NULL OR c.id = :categoriaId)
          AND (:marcaId IS NULL OR m.id = :marcaId)
          AND (:soloActivos IS NULL OR p.activo = :soloActivos)
        """)
    Page<Producto> buscar(@Param("q") String q,
                          @Param("categoriaId") Long categoriaId,
                          @Param("marcaId") Long marcaId,
                          @Param("soloActivos") Boolean soloActivos,
                          Pageable pageable);

    @Query("SELECT COUNT(v) FROM VarianteProducto v WHERE v.producto.id = :productoId")
    long contarVariantes(Long productoId);
}
