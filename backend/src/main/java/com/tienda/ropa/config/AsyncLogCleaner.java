package com.tienda.ropa.config;

import com.tienda.ropa.repository.TokenRevocadoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Limpieza periodica de tokens revocados ya expirados. */
@Component
public class AsyncLogCleaner {
    private final TokenRevocadoRepository repo;
    public AsyncLogCleaner(TokenRevocadoRepository repo) { this.repo = repo; }

    @Scheduled(fixedDelay = 3600_000)
    @Transactional
    public void limpiarTokensExpirados() {
        repo.deleteByExpiraEnBefore(Instant.now());
    }
}
