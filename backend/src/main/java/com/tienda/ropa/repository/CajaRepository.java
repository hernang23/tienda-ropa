package com.tienda.ropa.repository;

import com.tienda.ropa.entity.Caja;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CajaRepository extends JpaRepository<Caja, Long> {
    Optional<Caja> findFirstByEstadoOrderByIdDesc(com.tienda.ropa.enums.EstadoCaja estado);
    List<Caja> findByEstado(com.tienda.ropa.enums.EstadoCaja estado);
    Page<Caja> findAllByOrderByIdDesc(Pageable pageable);

    @Query(value = "SELECT COALESCE(MAX(SUBSTRING(numero FROM 5)::int), 0) FROM caja", nativeQuery = true)
    int ultimoNumeroSecuencial();
}
