package com.tienda.ropa.repository;

import com.tienda.ropa.entity.RegistroAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {
    Page<RegistroAuditoria> findAllByOrderByIdDesc(Pageable pageable);
}
