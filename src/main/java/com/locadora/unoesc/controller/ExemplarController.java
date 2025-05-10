package com.locadora.unoesc.controller;

import com.locadora.unoesc.model.Exemplar;
import com.locadora.unoesc.model.Filme;
import com.locadora.unoesc.repository.ExemplarRepository;
import com.locadora.unoesc.repository.FilmeRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDate;

@RestController
@RequestMapping("/exemplares")
public class ExemplarController {

    private final ExemplarRepository exemplarRepository;
    private final FilmeRepository filmeRepository;

    public ExemplarController(ExemplarRepository exemplarRepository, FilmeRepository filmeRepository) {
        this.exemplarRepository = exemplarRepository;
        this.filmeRepository = filmeRepository;
    }

    @GetMapping
    public Object listar(HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) {
            return new ModelAndView("redirect:/login");
        }
        return exemplarRepository.findAll();
    }

    @PostMapping
    public Object salvar(@RequestBody Exemplar exemplar, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) {
            return new ModelAndView("redirect:/login");
        }

        Filme filme = filmeRepository.findById(exemplar.getFilme().getId())
                .orElseThrow(() -> new RuntimeException("Filme não encontrado"));

        if (!filme.isAtivo()) {
            throw new RuntimeException("Não é possível adicionar exemplar: o filme está inativo.");
        }

        exemplar.setDataCadastro(LocalDate.now());
        exemplar.setFilme(filme);

        Exemplar novoExemplar = exemplarRepository.save(exemplar);

        long totalExemplares = exemplarRepository.countByFilmeAndAtivoTrue(filme);
        filme.setExemplaresDisponiveis(totalExemplares);
        filmeRepository.save(filme);

        return novoExemplar;
    }

    @PutMapping("/{id}/inativar")
    public Object inativar(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) {
            return new ModelAndView("redirect:/login");
        }

        Exemplar exemplar = exemplarRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exemplar não encontrado"));

        if (!exemplar.isAtivo()) {
            throw new RuntimeException("Este exemplar já está inativo.");
        }

        exemplar.setAtivo(false);
        exemplarRepository.save(exemplar);

        Filme filme = exemplar.getFilme();
        long totalExemplaresAtivos = exemplarRepository.countByFilmeAndAtivoTrue(filme);
        filme.setExemplaresDisponiveis(totalExemplaresAtivos);
        filmeRepository.save(filme);

        return exemplar;
    }

    @PutMapping("/{id}/ativar")
    public Object ativar(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) {
            return new ModelAndView("redirect:/login");
        }

        Exemplar exemplar = exemplarRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exemplar não encontrado"));

        if (exemplar.isAtivo()) {
            throw new RuntimeException("Este exemplar já está ativo.");
        }

        exemplar.setAtivo(true);
        exemplarRepository.save(exemplar);

        Filme filme = exemplar.getFilme();
        long totalExemplaresAtivos = exemplarRepository.countByFilmeAndAtivoTrue(filme);
        filme.setExemplaresDisponiveis(totalExemplaresAtivos);
        filmeRepository.save(filme);

        return exemplar;
    }
}
