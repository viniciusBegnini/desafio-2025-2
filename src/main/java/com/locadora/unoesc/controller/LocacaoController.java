package com.locadora.unoesc.controller;

import com.locadora.unoesc.model.Exemplar;
import com.locadora.unoesc.model.Filme;
import com.locadora.unoesc.model.Locacao;
import com.locadora.unoesc.repository.ExemplarRepository;
import com.locadora.unoesc.repository.FilmeRepository;
import com.locadora.unoesc.repository.LocacaoRepository;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/locacoes")
public class LocacaoController {

    private final LocacaoRepository locacaoRepository;
    private final ExemplarRepository exemplarRepository;
    private final FilmeRepository filmeRepository;

    public LocacaoController(LocacaoRepository locacaoRepository, ExemplarRepository exemplarRepository, FilmeRepository filmeRepository) {
        this.locacaoRepository = locacaoRepository;
        this.exemplarRepository = exemplarRepository;
        this.filmeRepository = filmeRepository;
    }

    // 1️⃣ Listar locações
    @GetMapping
    public List<Locacao> listar() {
        return locacaoRepository.findAll();
    }

    // 2️⃣ Criar locação
    @PostMapping
    public Locacao criar(@RequestBody Locacao locacao) {
        List<Exemplar> exemplares = locacao.getExemplares();

        if (exemplares == null || exemplares.isEmpty()) {
            throw new RuntimeException("É necessário selecionar pelo menos 1 exemplar.");
        }

        if (exemplares.size() > 3) {
            throw new RuntimeException("Não é permitido selecionar mais de 3 exemplares.");
        }

        // Valida cada exemplar
        for (Exemplar ex : exemplares) {
            Exemplar exemplarBanco = exemplarRepository.findById(ex.getId())
                    .orElseThrow(() -> new RuntimeException("Exemplar com ID " + ex.getId() + " não encontrado."));

            if (!exemplarBanco.isAtivo()) {
                throw new RuntimeException("Exemplar com ID " + ex.getId() + " está inativo.");
            }
        }

        // Preenche datas automáticas
        locacao.setDataLocacao(LocalDate.now());
        locacao.setDataDevolucao(locacao.getDataLocacao().plusDays(7)); // Padrão: 7 dias de locação

        // Gera o QRCode
        String qrCodeBase64 = gerarQRCode(locacao);
        locacao.setQrCode(qrCodeBase64);

        // Salva a locação
        Locacao novaLocacao = locacaoRepository.save(locacao);

        // Atualiza exemplaresDisponiveis dos filmes
        for (Exemplar ex : exemplares) {
            // Buscamos o exemplar completo novamente do banco
            Exemplar exemplarCompleto = exemplarRepository.findById(ex.getId())
                    .orElseThrow(() -> new RuntimeException("Exemplar com ID " + ex.getId() + " não encontrado ao atualizar contador."));

            Filme filme = exemplarCompleto.getFilme();

            if (filme != null) {
                long totalAtivos = exemplarRepository.countByFilmeAndAtivoTrue(filme);
                filme.setExemplaresDisponiveis(totalAtivos - 1);
                filmeRepository.save(filme);
            } else {
                throw new RuntimeException("Exemplar ID " + ex.getId() + " não está associado a nenhum filme.");
            }
        }

        return novaLocacao;
    }

    // 3️⃣ Devolver locação
    @PutMapping("/{id}/devolver")
    public Locacao devolver(@PathVariable Long id) {
        Locacao locacao = locacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Locação não encontrada."));

        if (locacao.getDataDevolvido() != null) {
            throw new RuntimeException("Esta locação já foi devolvida.");
        }

        locacao.setDataDevolvido(LocalDate.now());
        locacaoRepository.save(locacao);

        // Atualiza exemplaresDisponiveis dos filmes
        for (Exemplar ex : locacao.getExemplares()) {
            Exemplar exemplarCompleto = exemplarRepository.findById(ex.getId())
                    .orElseThrow(() -> new RuntimeException("Exemplar com ID " + ex.getId() + " não encontrado ao atualizar contador."));
            Filme filme = exemplarCompleto.getFilme();
            if (filme != null) {
                long totalAtivos = exemplarRepository.countByFilmeAndAtivoTrue(filme);
                filme.setExemplaresDisponiveis(totalAtivos + 1);
                filmeRepository.save(filme);
            }
        }

        return locacao;
    }

    // 🔧 Método para gerar QRCode (GET com parâmetros)
    private String gerarQRCode(Locacao locacao) {
        try {
            RestTemplate restTemplate = new RestTemplate();
    
            String dados = "CPF: " + locacao.getCpf()
                    + ", Telefone: " + locacao.getTelefone()
                    + ", Data Locacao: " + locacao.getDataLocacao()
                    + ", Data Devolucao: " + locacao.getDataDevolucao();
    
            String url = "https://api.apgy.in/qr/"
                    + "?data=" + java.net.URLEncoder.encode(dados, java.nio.charset.StandardCharsets.UTF_8)
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
}
