package com.tienda.ropa.repository;

import com.tienda.ropa.entity.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    Optional<Venta> findByClaveIdempotencia(String clave);
    Optional<Venta> findByNumero(String numero);

    @Query("""
        SELECT v FROM Venta v
        LEFT JOIN FETCH v.cliente
        WHERE (:q IS NULL OR LOWER(v.numero) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(v.cliente.nombre, '')) LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:desde IS NULL OR v.fecha >= :desde)
          AND (:hasta IS NULL OR v.fecha <= :hasta)
        """)
    Page<Venta> buscar(@Param("q") String q,
                       @Param("desde") OffsetDateTime desde,
                       @Param("hasta") OffsetDateTime hasta,
                       Pageable pageable);

    @Query(value = "SELECT COALESCE(MAX(SUBSTRING(numero FROM 4)::int), 0) FROM venta", nativeQuery = true)
    int ultimoNumeroSecuencial();

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v WHERE v.estado = 'COMPLETADA' AND v.fecha BETWEEN :desde AND :hasta")
    BigDecimal totalVentasEntre(@Param("desde") OffsetDateTime desde, @Param("hasta") OffsetDateTime hasta);

    @Query("SELECT COUNT(v) FROM Venta v WHERE v.estado = 'COMPLETADA' AND v.fecha BETWEEN :desde AND :hasta")
    long contarVentasEntre(@Param("desde") OffsetDateTime desde, @Param("hasta") OffsetDateTime hasta);

    @Query("""
        SELECT CAST(v.fecha AS date) as dia, COALESCE(SUM(v.total),0), COUNT(v)
        FROM Venta v WHERE v.estado = 'COMPLETADA' AND v.fecha BETWEEN :desde AND :hasta
        GROUP BY CAST(v.fecha AS date) ORDER BY CAST(v.fecha AS date)
        """)
    java.util.List<Object[]> ventasPorDia(@Param("desde") OffsetDateTime desde, @Param("hasta") OffsetDateTime hasta);

    @Query("""
        SELECT p.metodo, COALESCE(SUM(p.monto), 0)
        FROM PagoVenta p JOIN p.venta v
        WHERE p.estado = 'REGISTRADO' AND v.estado <> 'ANULADA' AND v.fecha BETWEEN :desde AND :hasta
        GROUP BY p.metodo
        """)
    java.util.List<Object[]> ingresosPorMetodo(@Param("desde") OffsetDateTime desde, @Param("hasta") OffsetDateTime hasta);

    @Query("""
        SELECT SUM(d.cantidad * d.precioUnitario - d.descuento + d.impuestos) - SUM(d.cantidad * d.costoUnitario)
        FROM DetalleVenta d JOIN d.venta v
        WHERE v.estado = 'COMPLETADA' AND v.fecha BETWEEN :desde AND :hasta
        """)
    BigDecimal gananciaBrutaEstimada(@Param("desde") OffsetDateTime desde, @Param("hasta") OffsetDateTime hasta);

    @Query("""
        SELECT d.variante.id, COALESCE(SUM(d.cantidad), 0)
        FROM DetalleVenta d JOIN d.venta v
        WHERE v.estado = 'COMPLETADA' AND v.fecha BETWEEN :desde AND :hasta
        GROUP BY d.variante.id ORDER BY SUM(d.cantidad) DESC
        """)
    java.util.List<Object[]> topVariantesVendidas(@Param("desde") OffsetDateTime desde, @Param("hasta") OffsetDateTime hasta, Pageable pageable);

    @Query("SELECT COALESCE(SUM(d.cantidad), 0) FROM DetalleVenta d JOIN d.venta v WHERE v.estado = 'COMPLETADA' AND v.fecha BETWEEN :desde AND :hasta")
    long unidadesVendidasEntre(@Param("desde") OffsetDateTime desde, @Param("hasta") OffsetDateTime hasta);

    @Query("SELECT COUNT(c) FROM Compra c WHERE c.estado IN ('PENDIENTE','RECIBIDA') AND c.fechaRegistro BETWEEN :desde AND :hasta")
    long contarComprasEntre(@Param("desde") OffsetDateTime desde, @Param("hasta") OffsetDateTime hasta);

    @Query("SELECT COALESCE(SUM(c.total), 0) FROM Compra c WHERE c.estado = 'RECIBIDA' AND c.fechaRegistro BETWEEN :desde AND :hasta")
    BigDecimal totalComprasEntre(@Param("desde") OffsetDateTime desde, @Param("hasta") OffsetDateTime hasta);

    @Query("SELECT v FROM Venta v WHERE v.id = :id AND v.estado IN ('COMPLETADA','PENDIENTE_PAGO')")
    Optional<Venta> findEditablesById(Long id);
}
