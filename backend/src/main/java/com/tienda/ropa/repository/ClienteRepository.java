package com.tienda.ropa.repository;

import com.tienda.ropa.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    boolean existsByDocumento(String documento);

    @Query("SELECT c FROM Cliente c WHERE (:q IS NULL OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(c.documento) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Cliente> buscar(String q, Pageable pageable);
}
