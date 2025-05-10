package com.locadora.unoesc.controller;

import com.locadora.unoesc.model.Filme;
import com.locadora.unoesc.repository.FilmeRepository;
import com.locadora.unoesc.service.TMDBService;
import com.locadora.unoesc.service.TMDBService.TMDBFilmeDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

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

    @GetMapping("/cadastrar")
    public ModelAndView exibirFormulario(HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) {
            return new ModelAndView("redirect:/login");
        }
        return new ModelAndView("cadastroFilmes");
    }

    @GetMapping("/listar")
    public ModelAndView listarFilmesPage(HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) {
            return new ModelAndView("redirect:/login");
        }
        ModelAndView mv = new ModelAndView("filmes");
        mv.addObject("filmes", filmeRepository.findAll());
        return mv;
    }

    @PostMapping
    public Object salvar(@RequestBody Filme filme, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) {
            return new ModelAndView("redirect:/login");
        }

        Optional<Filme> existente = filmeRepository.findByTituloIgnoreCase(filme.getTitulo());
        if (existente.isPresent()) {
            throw new RuntimeException("Já existe um filme cadastrado com este título.");
        }

        return filmeRepository.save(filme);
    }

    @GetMapping("/aleatorio")
    public Object buscarFilmeAleatorio(HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) {
            return new ModelAndView("redirect:/login");
        }

        TMDBFilmeDTO dados = null;
        int tentativas = 0;

        while (tentativas < 10) {
            dados = tmdbService.buscarFilmeAleatorio();
            if (dados != null && filmeRepository.findByTituloIgnoreCase(dados.titulo).isEmpty()) {
                return dados;
            }
            tentativas++;
        }

        throw new RuntimeException("Não foi possível encontrar um filme único após várias tentativas.");
    }

    @GetMapping("/editar/{id}")
    public ModelAndView exibirFormularioEdicao(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) {
            return new ModelAndView("redirect:/login");
        }

        Filme filme = filmeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Filme não encontrado"));

        ModelAndView mv = new ModelAndView("editarFilme");
        mv.addObject("filme", filme);
        return mv;
    }

    @PostMapping("/editar/{id}")
    public ModelAndView salvarEdicao(@PathVariable Long id,
            @RequestParam boolean ativo,
            @RequestParam(required = false) Long exemplaresDisponiveis,
            HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) {
            return new ModelAndView("redirect:/login");
        }

        Filme filme = filmeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Filme não encontrado"));

        filme.setAtivo(ativo);
        filme.setExemplaresDisponiveis(exemplaresDisponiveis);

        filmeRepository.save(filme);
        return new ModelAndView("redirect:/filmes/listar");
    }
}
