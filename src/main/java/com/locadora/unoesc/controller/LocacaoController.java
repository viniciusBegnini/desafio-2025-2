package com.locadora.unoesc.controller;

import com.locadora.unoesc.model.Exemplar;
import com.locadora.unoesc.model.Filme;
import com.locadora.unoesc.model.Locacao;
import com.locadora.unoesc.repository.ExemplarRepository;
import com.locadora.unoesc.repository.FilmeRepository;
import com.locadora.unoesc.repository.LocacaoRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/locacoes")
public class LocacaoController {

    private final LocacaoRepository locacaoRepository;
    private final ExemplarRepository exemplarRepository;
    private final FilmeRepository filmeRepository;

    public LocacaoController(LocacaoRepository locacaoRepository,
            ExemplarRepository exemplarRepository,
            FilmeRepository filmeRepository) {
        this.locacaoRepository = locacaoRepository;
        this.exemplarRepository = exemplarRepository;
        this.filmeRepository = filmeRepository;
    }

    @GetMapping
    public Object listar(HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) {
            return new ModelAndView("redirect:/login");
        }
        return locacaoRepository.findAll();
    }

    @GetMapping("/cadastrar")
    public ModelAndView exibirFormularioLocacao(HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) {
            return new ModelAndView("redirect:/login");
        }

        ModelAndView mv = new ModelAndView("cadastroLocacao");
        mv.addObject("exemplares", exemplarRepository.findByAtivoTrue());
        return mv;
    }

    @GetMapping("/listar")
    public ModelAndView listarComFiltro(@RequestParam(required = false) String busca, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) {
            return new ModelAndView("redirect:/login");
        }

        List<Locacao> locacoes = locacaoRepository.findAll();

        if (busca != null && !busca.isBlank()) {
            String termo = busca.toLowerCase();
            locacoes = locacoes.stream().filter(l -> l.getNome().toLowerCase().contains(termo) ||
                    l.getCpf().contains(termo) ||
                    l.getEmail().toLowerCase().contains(termo) ||
                    l.getExemplares().stream().anyMatch(e -> e.getFilme().getTitulo().toLowerCase().contains(termo)))
                    .toList();
        }

        ModelAndView mv = new ModelAndView("locacoes");
        mv.addObject("locacoes", locacoes);
        return mv;
    }

    @PostMapping
    public Object salvarFormulario(
            @RequestParam String nome,
            @RequestParam String cpf,
            @RequestParam String email,
            @RequestParam String telefone,
            @RequestParam(value = "exemplares", required = false) List<Long> exemplaresIds,
            @RequestParam("dataDevolucao") String dataDevolucaoStr,
            HttpSession session) {

        if (session.getAttribute("usuarioLogado") == null) {
            return new ModelAndView("redirect:/login");
        }

        ModelAndView mv = new ModelAndView("cadastroLocacao");
        mv.addObject("exemplares", exemplarRepository.findByAtivoTrue());

        try {

            cpf = cpf.replaceAll("[^0-9]", "");
            telefone = telefone.replaceAll("[^0-9]", "");
            if (cpf.length() != 11) {
                mv.addObject("erro", "CPF inválido.");
                return mv;
            }

            if (telefone.length() < 10 || telefone.length() > 11) {
                mv.addObject("erro", "Número de telefone inválido.");
                return mv;
            }

            if (exemplaresIds == null || exemplaresIds.size() < 1 || exemplaresIds.size() > 3) {
                mv.addObject("erro", "Você deve selecionar entre 1 e 3 exemplares.");
                return mv;
            }

            LocalDate dataDevolucao = LocalDate.parse(dataDevolucaoStr);
            if (dataDevolucao.isBefore(LocalDate.now())) {
                mv.addObject("erro", "A data de devolução não pode ser anterior à data de hoje.");
                return mv;
            }

            List<Exemplar> exemplares = exemplarRepository.findAllById(exemplaresIds);
            for (Exemplar ex : exemplares) {
                if (!ex.isAtivo()) {
                    mv.addObject("erro", "Exemplar ID " + ex.getId() + " está inativo.");
                    return mv;
                }

                if (locacaoRepository.existsByExemplaresAndDataDevolvidoIsNull(ex)) {
                    mv.addObject("erro", "O exemplar ID " + ex.getId() + " já está em uma locação pendente.");
                    return mv;
                }
            }

            Locacao locacao = new Locacao();
            locacao.setNome(nome);
            locacao.setCpf(cpf);
            locacao.setEmail(email);
            locacao.setTelefone(telefone);
            locacao.setExemplares(exemplares);
            locacao.setDataLocacao(LocalDate.now());
            locacao.setDataDevolucao(dataDevolucao);
            locacao.setQrCode(gerarQRCode(locacao));

            locacaoRepository.save(locacao);

            for (Exemplar ex : exemplares) {
                Filme filme = ex.getFilme();
                if (filme != null) {
                    filme.setExemplaresDisponiveis(filme.getExemplaresDisponiveis() - 1);
                    filmeRepository.save(filme);
                }
            }

            ModelAndView sucesso = new ModelAndView("locacaoSucesso");
            sucesso.addObject("qrCode", locacao.getQrCode());
            return sucesso;

        } catch (Exception e) {
            mv.addObject("erro", "Erro ao salvar locação: " + e.getMessage());
            return mv;
        }
    }

    @PutMapping("/{id}/devolver")
    public Object devolver(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) {
            return new ModelAndView("redirect:/login");
        }

        Locacao locacao = locacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Locação não encontrada."));

        if (locacao.getDataDevolvido() != null) {
            throw new RuntimeException("Esta locação já foi devolvida.");
        }

        locacao.setDataDevolvido(LocalDate.now());
        locacaoRepository.save(locacao);

        for (Exemplar ex : locacao.getExemplares()) {
            Exemplar exemplarCompleto = exemplarRepository.findById(ex.getId())
                    .orElseThrow(() -> new RuntimeException(
                            "Exemplar com ID " + ex.getId() + " não encontrado ao atualizar contador."));
            Filme filme = exemplarCompleto.getFilme();
            if (filme != null) {
                filme.setExemplaresDisponiveis(filme.getExemplaresDisponiveis() + 1);
                filmeRepository.save(filme);
            }
        }
        return new ModelAndView("redirect:/locacoes/listar");
    }

    private String gerarQRCode(Locacao locacao) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String dados = "CPF: " + locacao.getCpf()
                    + ", Telefone: " + locacao.getTelefone()
                    + ", Data Locacao: " + locacao.getDataLocacao()
                    + ", Data Devolucao: " + locacao.getDataDevolucao();

            String url = "https://api.apgy.in/qr/"
                    + "?data=" + URLEncoder.encode(dados, StandardCharsets.UTF_8)
                    + "&size=300";

            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);

            if (response.getStatusCode() == HttpStatus.OK) {
                byte[] qrImage = response.getBody();
                return Base64.getEncoder().encodeToString(qrImage);
            } else {
                throw new RuntimeException("Erro ao gerar QRCode: " + response.getStatusCode());
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Falha ao gerar QRCode: " + e.getMessage());
        }
    }

    @GetMapping("/consulta-publica")
    public ModelAndView mostrarConsulta(@RequestParam(required = false) String cpf) {
        ModelAndView mv = new ModelAndView("consultaPublica");
        if (cpf != null && !cpf.trim().isEmpty()) {
            cpf = cpf.replaceAll("[^0-9]", "");
            if (cpf.length() != 11) {
                mv.addObject("locacoes", List.of());
                mv.addObject("erro", "CPF inválido. O CPF deve conter 11 números.");
                return mv;
            }
            List<Locacao> locacoes = locacaoRepository.findByCpfAndDataDevolvidoIsNull(cpf);
            mv.addObject("locacoes", locacoes);
        }
        return mv;
    }

}
