package com.locadora.unoesc.repository;

import com.locadora.unoesc.model.Filme;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FilmeRepository extends JpaRepository<Filme, Long> {
}