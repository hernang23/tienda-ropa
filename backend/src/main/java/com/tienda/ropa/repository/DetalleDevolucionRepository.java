package com.tienda.ropa.repository;

import com.tienda.ropa.entity.DetalleDevolucion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DetalleDevolucionRepository extends JpaRepository<DetalleDevolucion, Long> {
    List<DetalleDevolucion> findByDevolucionId(Long devolucionId);
}
