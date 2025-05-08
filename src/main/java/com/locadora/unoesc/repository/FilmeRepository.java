package com.locadora.unoesc.repository;

import com.locadora.unoesc.model.Filme;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FilmeRepository extends JpaRepository<Filme, Long> {
    Optional<Filme> findByTituloIgnoreCase(String titulo);
}