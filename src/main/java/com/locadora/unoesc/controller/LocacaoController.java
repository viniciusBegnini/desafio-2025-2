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

    @GetMapping
    public List<Locacao> listar() {
        return locacaoRepository.findAll();
    }

    @PostMapping
    public Locacao criar(@RequestBody Locacao locacao) {
        List<Exemplar> exemplares = locacao.getExemplares();

        if (exemplares == null || exemplares.isEmpty()) {
            throw new RuntimeException("É necessário selecionar pelo menos 1 exemplar.");
        }

        if (exemplares.size() > 3) {
            throw new RuntimeException("Não é permitido selecionar mais de 3 exemplares.");
        }

        for (Exemplar ex : exemplares) {
            Exemplar exemplarBanco = exemplarRepository.findById(ex.getId())
                    .orElseThrow(() -> new RuntimeException("Exemplar com ID " + ex.getId() + " não encontrado."));

            if (!exemplarBanco.isAtivo()) {
                throw new RuntimeException("Exemplar com ID " + ex.getId() + " está inativo.");
            }

            boolean locacaoPendente = locacaoRepository.existsByExemplaresAndDataDevolvidoIsNull(exemplarBanco);
            if (locacaoPendente) {
                throw new RuntimeException("Exemplar com ID " + ex.getId() + " já está locado e não foi devolvido.");
            }
        }

        locacao.setDataLocacao(LocalDate.now());
        locacao.setDataDevolucao(locacao.getDataLocacao().plusDays(7));

        String qrCodeBase64 = gerarQRCode(locacao);
        locacao.setQrCode(qrCodeBase64);

        Locacao novaLocacao = locacaoRepository.save(locacao);

        for (Exemplar ex : exemplares) {
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

    @PutMapping("/{id}/devolver")
    public Locacao devolver(@PathVariable Long id) {
        Locacao locacao = locacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Locação não encontrada."));

        if (locacao.getDataDevolvido() != null) {
            throw new RuntimeException("Esta locação já foi devolvida.");
        }

        locacao.setDataDevolvido(LocalDate.now());
        locacaoRepository.save(locacao);

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

    @GetMapping("/consultar-locacao/{cpf}")
    public List<Locacao> consultarLocacaoPorCpf(@PathVariable String cpf) {
        return locacaoRepository.findByCpfAndDataDevolvidoIsNull(cpf);
    }
}
