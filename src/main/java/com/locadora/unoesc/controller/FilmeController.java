package com.locadora.unoesc.controller;

import com.locadora.unoesc.model.Filme;
import com.locadora.unoesc.repository.FilmeRepository;
import com.locadora.unoesc.service.TMDBService;
import com.locadora.unoesc.service.TMDBService.TMDBFilmeDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/filmes")
public class FilmeController {

    private final FilmeRepository filmeRepository;
    private final TMDBService tmdbService;

    public FilmeController(FilmeRepository filmeRepository, TMDBService tmdbService) {
        this.filmeRepository = filmeRepository;
        this.tmdbService = tmdbService;
    }

    @GetMapping
    public Object listar(HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) {
            return new ModelAndView("redirect:/login");
        }
        return filmeRepository.findAll();
    }

    @PostMapping
    public Object salvar(@RequestBody Filme filme, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) {
            return new ModelAndView("redirect:/login");
        }

        TMDBFilmeDTO dados = null;
        Optional<Filme> existente = Optional.empty();
        int tentativas = 0;

        while (tentativas < 10) {
            dados = tmdbService.buscarFilmeAleatorio();

            if (dados != null) {
                existente = filmeRepository.findByTituloIgnoreCase(dados.titulo);
                if (existente.isEmpty()) {
                    break;
                }
            }
            tentativas++;
        }

        if (dados == null) {
            throw new RuntimeException("Falha ao buscar filme da API.");
        }

        if (existente.isPresent()) {
            throw new RuntimeException("Não foi possível encontrar um filme único após várias tentativas.");
        }

        filme.setTitulo(dados.titulo);
        filme.setResumo(dados.resumo);
        filme.setPontuacao(dados.pontuacao);
        filme.setLancamento(dados.lancamento);

        return filmeRepository.save(filme);
    }

    @GetMapping("/{id}")
    public Object buscarPorId(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) {
            return new ModelAndView("redirect:/login");
        }

        return filmeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Filme não encontrado"));
    }
}
