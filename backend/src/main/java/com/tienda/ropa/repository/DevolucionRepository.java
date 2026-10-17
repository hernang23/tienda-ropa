package com.tienda.ropa.repository;

import com.tienda.ropa.entity.Devolucion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface DevolucionRepository extends JpaRepository<Devolucion, Long> {
    Optional<Devolucion> findByClaveIdempotencia(String clave);

    List<Devolucion> findByVentaId(Long ventaId);

    @Query("SELECT d FROM Devolucion d JOIN FETCH d.venta ORDER BY d.fecha DESC")
    Page<Devolucion> listar(Pageable pageable);

    /** Secuencia del dia: el prefijo DEV-aaammdd ocupa las primeras 10 posiciones. */
    @Query(value = "SELECT COALESCE(MAX(SUBSTRING(numero FROM 12)::int), 0) FROM devolucion WHERE numero LIKE 'DEV-%'", nativeQuery = true)
    int ultimoNumeroSecuencial();
}
