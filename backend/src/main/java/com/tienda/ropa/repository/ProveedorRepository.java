package com.tienda.ropa.repository;

import com.tienda.ropa.entity.Proveedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    boolean existsByNit(String nit);

    @Query("SELECT pr FROM Proveedor pr WHERE (:q IS NULL OR LOWER(pr.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(pr.nit) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Proveedor> buscar(String q, Pageable pageable);
}
