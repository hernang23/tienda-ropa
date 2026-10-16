package com.tienda.ropa.repository;

import com.tienda.ropa.entity.Devolucion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface DevolucionRepository extends JpaRepository<Devolucion, Long> {
    Optional<Devolucion> findByClaveIdempotencia(String clave);

    @Query("SELECT d FROM Devolucion d JOIN FETCH d.venta ORDER BY d.fecha DESC")
    Page<Devolucion> listar(Pageable pageable);

    @Query(value = "SELECT COALESCE(MAX(SUBSTRING(numero FROM 6)::int), 0) FROM devolucion", nativeQuery = true)
    int ultimoNumeroSecuencial();
}
