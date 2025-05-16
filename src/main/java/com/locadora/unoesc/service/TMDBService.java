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

            int paginaAleatoria = (int) (Math.random() * 500 + 1);
            String url = baseUrl + "/discover/movie?language=pt-BR&sort_by=popularity.desc&page=" + paginaAleatoria;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            JsonNode json = mapper.readTree(response.getBody());

            JsonNode results = json.get("results");
            int totalFilmesNaPagina = results.size();
            if (totalFilmesNaPagina == 0) {
                return null;
            }

            int indiceAleatorio = (int) (Math.random() * totalFilmesNaPagina);
            JsonNode filme = results.get(indiceAleatorio);

            LocalDate dataLancamento = null;
            if (filme.has("release_date") && !filme.get("release_date").asText().isEmpty()) {
                dataLancamento = LocalDate.parse(filme.get("release_date").asText());
            }

            String posterPath = filme.has("poster_path") && !filme.get("poster_path").isNull()
                    ? filme.get("poster_path").asText()
                    : null;

            String titulo = filme.get("title").asText();
            String resumo = filme.has("overview") ? filme.get("overview").asText() : "";

            if (resumo == null || resumo.trim().isEmpty()) {
                System.out.println("Resumo vazio em pt-BR, tentando buscar em inglês...");

                int filmeId = filme.get("id").asInt();
                String urlIngles = baseUrl + "/movie/" + filmeId + "?language=en-US";

                ResponseEntity<String> responseIngles = restTemplate.exchange(urlIngles, HttpMethod.GET, entity,
                        String.class);
                JsonNode jsonIngles = mapper.readTree(responseIngles.getBody());

                resumo = jsonIngles.has("overview") ? jsonIngles.get("overview").asText() : "";
            }

            if (resumo == null || resumo.trim().isEmpty()) {
                System.out.println("Filme sem resumo mesmo em inglês, pulando: " + titulo);
                return null;
            }

            System.out.println("Filme buscado: " + titulo);

            return new TMDBFilmeDTO(
                    titulo,
                    resumo,
                    String.valueOf(filme.get("vote_average").asDouble()),
                    dataLancamento,
                    posterPath);

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
        public String posterPath;

        public TMDBFilmeDTO(String titulo, String resumo, String pontuacao, LocalDate lancamento, String posterPath) {
            this.titulo = titulo;
            this.resumo = resumo;
            this.pontuacao = pontuacao;
            this.lancamento = lancamento;
            this.posterPath = posterPath;
        }
    }

}
