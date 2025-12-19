package it.robfrank.linklift.adapter.out.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.robfrank.linklift.application.port.out.EmbeddingGenerator;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
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
  public @NonNull List<Float> generateEmbedding(@NonNull String text) {
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

      Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
      List<Double> embeddingDouble = (List<Double>) responseBody.get("embedding");

      return embeddingDouble.stream().map(Double::floatValue).toList();
    } catch (IOException | InterruptedException e) {
      logger.error("Error generating embedding via Ollama", e);
      throw new RuntimeException("Error generating embedding", e);
    }
  }
}
