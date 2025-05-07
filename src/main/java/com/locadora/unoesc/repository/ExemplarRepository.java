package com.locadora.unoesc.repository;

import com.locadora.unoesc.model.Exemplar;
import org.springframework.data.jpa.repository.JpaRepository;
import com.locadora.unoesc.model.Filme;
import java.util.List;

public interface ExemplarRepository extends JpaRepository<Exemplar, Long> {
    long countByFilmeAndAtivoTrue(Filme filme);
    List<Exemplar> findByFilme(Filme filme);
}