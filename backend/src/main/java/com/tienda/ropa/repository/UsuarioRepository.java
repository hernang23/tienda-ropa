package com.tienda.ropa.repository;

import com.tienda.ropa.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    boolean existsByUsernameIgnoreCase(String username);

    @Query("SELECT u FROM Usuario u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%'))"
         + " OR LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Usuario> buscar(String q, Pageable pageable);
}
