package com.tienda.ropa.repository;

import com.tienda.ropa.entity.PagoVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PagoVentaRepository extends JpaRepository<PagoVenta, Long> {
    List<PagoVenta> findByVentaId(Long ventaId);
}
