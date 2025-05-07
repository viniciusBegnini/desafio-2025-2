package com.locadora.unoesc.controller;

import com.locadora.unoesc.model.Filme;
import com.locadora.unoesc.repository.FilmeRepository;
import com.locadora.unoesc.service.TMDBService;
import com.locadora.unoesc.service.TMDBService.TMDBFilmeDTO;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/filmes")
public class FilmeController {

    private final FilmeRepository filmeRepository;
    private final TMDBService tmdbService;

    // Construtor com injeção de dependência
    public FilmeController(FilmeRepository filmeRepository, TMDBService tmdbService) {
        this.filmeRepository = filmeRepository;
        this.tmdbService = tmdbService;
    }

    // Listar filmes
    @GetMapping
    public List<Filme> listar() {
        return filmeRepository.findAll();
    }

    // Cadastrar novo filme com dados vindos da API
    @PostMapping
    public Filme salvar(@RequestBody Filme filme) {
        
        TMDBFilmeDTO dados = tmdbService.buscarFilmeAleatorio();

        if (dados != null) {
            filme.setTitulo(dados.titulo);
            filme.setResumo(dados.resumo);
            filme.setPontuacao(dados.pontuacao);
            filme.setLancamento(dados.lancamento);
        }

        return filmeRepository.save(filme);
    }

    // Buscar filme por ID
    @GetMapping("/{id}")
    public Filme buscarPorId(@PathVariable Long id) {
        return filmeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Filme não encontrado"));
    }
}