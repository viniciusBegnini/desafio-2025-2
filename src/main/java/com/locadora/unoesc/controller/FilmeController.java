package com.locadora.unoesc.controller;

import com.locadora.unoesc.model.Filme;
import com.locadora.unoesc.repository.FilmeRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/filmes")
public class FilmeController {

    private final FilmeRepository filmeRepository;

    public FilmeController(FilmeRepository filmeRepository) {
        this.filmeRepository = filmeRepository;
    }

    @GetMapping
    public List<Filme> listar() {
        return filmeRepository.findAll();
    }

    @PostMapping
    public Filme salvar(@RequestBody Filme filme) {
        return filmeRepository.save(filme);
    }
}
