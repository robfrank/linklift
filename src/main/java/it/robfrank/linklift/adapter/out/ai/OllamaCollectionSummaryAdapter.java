package it.robfrank.linklift.adapter.out.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.robfrank.linklift.application.domain.model.Collection;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.port.out.CollectionSummaryService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OllamaCollectionSummaryAdapter implements CollectionSummaryService {

  private static final Logger logger = LoggerFactory.getLogger(OllamaCollectionSummaryAdapter.class);
  private final HttpClient httpClient;
  private final String ollamaUrl;
  private final String modelName;
  private final ObjectMapper objectMapper;

  public OllamaCollectionSummaryAdapter(HttpClient httpClient, String ollamaUrl, String modelName) {
    this.httpClient = httpClient;
    this.ollamaUrl = ollamaUrl;
    this.modelName = modelName;
    this.objectMapper = new ObjectMapper();
  }

  @Override
  @NonNull
  public String generateCollectionSummary(@NonNull Collection collection, @NonNull List<Link> links) {
    try {
      String prompt = buildPrompt(collection, links);

      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("model", modelName);
      requestBody.put("prompt", prompt);
      requestBody.put("stream", false);

      String jsonBody = objectMapper.writeValueAsString(requestBody);

      HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(ollamaUrl + "/api/generate"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
        .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        throw new RuntimeException("Failed to generate summary: " + response.body());
      }

      var root = objectMapper.readTree(response.body());
      return root.get("response").asText();
    } catch (Exception e) {
      logger.error("Error generating collection summary", e);
      return "Failed to generate summary.";
    }
  }

  private String buildPrompt(Collection collection, List<Link> links) {
    String linksText = links.stream().map(l -> "- " + l.title() + ": " + l.description()).collect(Collectors.joining("\n"));

    return """
    Summarize the following collection of web links in a single concise sentence.
    Collection Name: %s
    Collection Description: %s

    Links:
    %s
    """.formatted(collection.name(), collection.description(), linksText);
  }
}
