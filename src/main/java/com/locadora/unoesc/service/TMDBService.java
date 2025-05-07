package com.locadora.unoesc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TMDBService {

    private final String apiKey = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJmNmZhODYwMjdjNjlkNzFlZmM3NzlhZTlhNzc3NmRhYSIsIm5iZiI6MTc0NjU3ODE5NS41MDgsInN1YiI6IjY4MWFhYjEzZjNlNGRlMWY4NWM2OTc3YSIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.Opn-SSbi5Q-sS2dDg7nTE0rYPgdhFrJIT62edk_vdtY";
    private final String baseUrl = "https://api.themoviedb.org/3";

    public TMDBFilmeDTO buscarFilmeAleatorio() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            ObjectMapper mapper = new ObjectMapper();

            int paginaAleatoria = (int) (Math.random() * 10 + 1);
            String url = baseUrl + "/movie/popular?language=pt-BR&page=" + paginaAleatoria;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            JsonNode json = mapper.readTree(response.getBody());

            JsonNode filme = json.get("results").get(0);

            LocalDate dataLancamento = null;
            if (filme.has("release_date") && !filme.get("release_date").asText().isEmpty()) {
                dataLancamento = LocalDate.parse(filme.get("release_date").asText());
            }

            System.out.println("Filme buscado: " + filme.get("title").asText());

            return new TMDBFilmeDTO(
                filme.get("title").asText(),
                filme.get("overview").asText(),
                String.valueOf(filme.get("vote_average").asDouble()),
                dataLancamento
            );

        } catch (Exception e) {
            System.out.println("Erro ao buscar filme da API: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static class TMDBFilmeDTO {
        public String titulo;
        public String resumo;
        public String pontuacao;
        public LocalDate lancamento;

        public TMDBFilmeDTO(String titulo, String resumo, String pontuacao, LocalDate lancamento) {
            this.titulo = titulo;
            this.resumo = resumo;
            this.pontuacao = pontuacao;
            this.lancamento = lancamento;
        }
    }
}
