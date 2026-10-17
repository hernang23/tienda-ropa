package com.tienda.ropa.repository;

import com.tienda.ropa.entity.TokenRevocado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;

public interface TokenRevocadoRepository extends JpaRepository<TokenRevocado, Long> {
    boolean existsByJti(String jti);
    void deleteByExpiraEnBefore(Instant corte);
}
