package com.tienda.ropa.repository;

import com.tienda.ropa.entity.VarianteProducto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VarianteProductoRepository extends JpaRepository<VarianteProducto, Long> {

    /** Bloqueo pesimista para operaciones de stock: evita ventas dobles concurrentes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM VarianteProducto v WHERE v.id = :id")
    Optional<VarianteProducto> findByIdForUpdate(@Param("id") Long id);

    List<VarianteProducto> findByProductoIdOrderByTallaAscColorAsc(Long productoId);

    boolean existsBySku(String sku);

    Optional<VarianteProducto> findBySku(String sku);

    Optional<VarianteProducto> findByCodigoBarras(String codigoBarras);

    boolean existsByProductoIdAndTallaAndColorIgnoreCase(Long productoId, String talla, String color);

    @Query("""
        SELECT v FROM VarianteProducto v JOIN FETCH v.producto p
        WHERE v.existencias <= v.stockMinimo ORDER BY v.existencias ASC
        """)
    List<VarianteProducto> encontrarStockBajo();

    @Query("""
        SELECT v FROM VarianteProducto v JOIN FETCH v.producto p
        WHERE v.existencias = 0 AND v.activo = true ORDER BY p.nombre ASC
        """)
    List<VarianteProducto> encontrarAgotados();

    @Query("""
        SELECT v FROM VarianteProducto v JOIN v.producto p
        WHERE (:q IS NULL OR LOWER(v.sku) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(v.codigoBarras) LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:talla IS NULL OR LOWER(v.talla) = LOWER(:talla))
          AND (:color IS NULL OR LOWER(v.color) = LOWER(:color))
          AND (:bajoMinimo IS NULL OR v.existencias <= v.stockMinimo)
          AND (:agotado IS NULL OR v.existencias = 0)
        """)
    Page<VarianteProducto> buscarInventario(@Param("q") String q,
                                            @Param("talla") String talla,
                                            @Param("color") String color,
                                            @Param("bajoMinimo") Boolean bajoMinimo,
                                            @Param("agotado") Boolean agotado,
                                            Pageable pageable);

    @Query("SELECT COALESCE(SUM(v.existencias * v.costoUnitario), 0) FROM VarianteProducto v WHERE v.activo = true")
    java.math.BigDecimal valorInventarioAlCosto();
}
