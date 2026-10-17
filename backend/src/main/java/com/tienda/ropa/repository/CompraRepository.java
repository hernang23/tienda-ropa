package com.tienda.ropa.repository;

import com.tienda.ropa.entity.Compra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CompraRepository extends JpaRepository<Compra, Long> {
    @Query("SELECT c FROM Compra c JOIN FETCH c.proveedor WHERE (:q IS NULL OR LOWER(c.numero) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(c.proveedor.nombre) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Compra> buscar(String q, Pageable pageable);

    /** Secuencia del dia: el prefijo CMP-aaammdd ocupa las primeras 9 posiciones. */
    @Query(value = "SELECT COALESCE(MAX(SUBSTRING(numero FROM 11)::int), 0) FROM compra WHERE numero LIKE 'CMP-%'", nativeQuery = true)
    int ultimoNumeroSecuencial();

    @Query("SELECT c FROM Compra c JOIN FETCH c.proveedor WHERE c.proveedor.id = :proveedorId ORDER BY c.fechaRegistro DESC")
    Page<Compra> porProveedor(Long proveedorId, Pageable pageable);
}
