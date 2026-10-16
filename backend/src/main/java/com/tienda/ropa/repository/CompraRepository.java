package com.tienda.ropa.repository;

import com.tienda.ropa.entity.Compra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CompraRepository extends JpaRepository<Compra, Long> {
    @Query("SELECT c FROM Compra c JOIN FETCH c.proveedor WHERE (:q IS NULL OR LOWER(c.numero) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(c.proveedor.nombre) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Compra> buscar(String q, Pageable pageable);

    @Query(value = "SELECT COALESCE(MAX(SUBSTRING(numero FROM 3)::int), 0) FROM compra", nativeQuery = true)
    int ultimoNumeroSecuencial();
}
