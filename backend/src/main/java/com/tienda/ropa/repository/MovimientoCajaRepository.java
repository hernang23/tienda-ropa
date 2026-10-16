package com.tienda.ropa.repository;

import com.tienda.ropa.entity.MovimientoCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {
    List<MovimientoCaja> findByCajaIdOrderByFechaAsc(Long cajaId);
}
