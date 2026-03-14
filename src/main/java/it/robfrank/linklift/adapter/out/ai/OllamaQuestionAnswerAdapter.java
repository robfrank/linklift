package it.robfrank.linklift.adapter.out.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.robfrank.linklift.application.port.out.QuestionAnswerPort;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OllamaQuestionAnswerAdapter implements QuestionAnswerPort {

  private static final Logger logger = LoggerFactory.getLogger(OllamaQuestionAnswerAdapter.class);
  private static final int MAX_CONTEXT_CHARS = 4000;

  private final HttpClient httpClient;
  private final String ollamaUrl;
  private final String modelName;
  private final ObjectMapper objectMapper;

  public OllamaQuestionAnswerAdapter(HttpClient httpClient, String ollamaUrl, String modelName) {
    this.httpClient = httpClient;
    this.ollamaUrl = ollamaUrl;
    this.modelName = modelName;
    this.objectMapper = new ObjectMapper();
  }

  @Override
  @NonNull
  public String generateAnswer(@NonNull String question, @NonNull String context) {
    try {
      String truncatedContext = context.length() > MAX_CONTEXT_CHARS ? context.substring(0, MAX_CONTEXT_CHARS) + "..." : context;

      String prompt = buildPrompt(question, truncatedContext);

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
        logger.error("Ollama returned status {} for question answering", response.statusCode());
        return "I was unable to generate an answer at this time.";
      }

      var root = objectMapper.readTree(response.body());
      return root.get("response").asText();
    } catch (JsonProcessingException e) {
      logger.error("Error creating request for question answering", e);
      return "I was unable to generate an answer due to a request error.";
    } catch (IOException e) {
      logger.error("Error communicating with Ollama for question answering", e);
      return "I was unable to generate an answer due to a network error.";
    } catch (InterruptedException e) {
      logger.error("Interrupted while communicating with Ollama", e);
      Thread.currentThread().interrupt();
      return "I was unable to generate an answer due to a network error.";
    }
  }

  private String buildPrompt(String question, String context) {
    return """
    You are a helpful assistant that answers questions based on the user's saved links and their content.
    Answer the following question using only the information provided in the context below.
    If the context does not contain enough information to answer the question, say so clearly.
    Be concise and factual.

    Context:
    %s

    Question: %s

    Answer:""".formatted(context, question);
  }
}
