package com.locadora.unoesc.repository;

import com.locadora.unoesc.model.Exemplar;
import com.locadora.unoesc.model.Locacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocacaoRepository extends JpaRepository<Locacao, Long> {
    List<Locacao> findByCpfAndDataDevolvidoIsNull(String cpf);

    boolean existsByExemplaresAndDataDevolvidoIsNull(Exemplar exemplar);
    boolean existsByExemplaresContaining(Exemplar exemplar);
    
}
