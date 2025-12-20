package it.robfrank.linklift.adapter.out.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.robfrank.linklift.application.port.out.EmbeddingGenerator;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OllamaEmbeddingAdapter implements EmbeddingGenerator {

  private static final Logger logger = LoggerFactory.getLogger(OllamaEmbeddingAdapter.class);

  private final String ollamaUrl;
  private final String model;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  public OllamaEmbeddingAdapter(@NonNull HttpClient httpClient, String ollamaUrl, String model) {
    this.httpClient = httpClient;
    this.ollamaUrl = ollamaUrl != null ? ollamaUrl : "http://localhost:11434";
    this.model = model != null ? model : "nomic-embed-text";
    this.objectMapper = new ObjectMapper();
  }

  @Override
  @NonNull
  public List<Float> generateEmbedding(@NonNull String text) {
    try {
      Map<String, String> requestBody = Map.of("model", model, "prompt", text);
      String body = objectMapper.writeValueAsString(requestBody);

      HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(ollamaUrl + "/api/embeddings"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        throw new RuntimeException("Failed to generate embedding: " + response.body());
      }

      Map<String, Object> responseBody = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
      Object embeddingObj = responseBody.get("embedding");
      if (!(embeddingObj instanceof List<?> list)) {
        throw new RuntimeException("Unexpected response format from Ollama: embedding field missing or not a list");
      }

      return list.stream().filter(Objects::nonNull).map(n -> ((Number) n).floatValue()).toList();
    } catch (IOException e) {
      logger.error("Error generating embedding via Ollama", e);
      throw new RuntimeException("Error generating embedding", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.error("Interrupted while generating embedding via Ollama", e);
      throw new RuntimeException("Interrupted while generating embedding", e);
    }
  }
}
