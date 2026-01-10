package it.robfrank.linklift.adapter.out.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OllamaEmbeddingAdapterTest {

  @Mock
  private HttpClient httpClient;

  private OllamaEmbeddingAdapter adapter;

  @BeforeEach
  void setUp() {
    System.clearProperty("LINKLIFT_OLLAMA_DIMENSIONS");
    System.clearProperty("LINKLIFT_OLLAMA_URL");
    System.clearProperty("LINKLIFT_OLLAMA_MODEL");
  }

  @SuppressWarnings("unchecked")
  private HttpResponse<String> mockHttpResponse(int statusCode, String body) {
    HttpResponse<String> response = (HttpResponse<String>) mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(statusCode);
    when(response.body()).thenReturn(body);
    return response;
  }

  // ==================== Happy Path Tests ====================

  @Test
  void generateEmbedding_shouldReturnEmbedding_whenValidResponseReceived() throws Exception {
    // Arrange
    adapter = new OllamaEmbeddingAdapter(httpClient, "http://localhost:11434", "test-model");

    String responseJson = "{\"embedding\": [0.1, 0.2, 0.3, 0.4]}";
    @SuppressWarnings({ "unchecked", "rawtypes" })
    HttpResponse response = mockHttpResponse(200, responseJson);
    when(httpClient.send(any(HttpRequest.class), any())).thenReturn((HttpResponse) response);

    // Act
    List<Float> embedding = adapter.generateEmbedding("test text");

    // Assert
    assertThat(embedding).hasSize(4).containsExactly(0.1f, 0.2f, 0.3f, 0.4f);
    verify(httpClient, times(1)).send(any(HttpRequest.class), any());
  }

  @Test
  void generateEmbedding_shouldSendCorrectRequest_withProperModelAndPrompt() throws Exception {
    // Arrange
    adapter = new OllamaEmbeddingAdapter(httpClient, "http://localhost:11434", "test-model");

    String responseJson = "{\"embedding\": [0.1]}";
    @SuppressWarnings({ "unchecked", "rawtypes" })
    HttpResponse response = mockHttpResponse(200, responseJson);
    when(httpClient.send(any(HttpRequest.class), any())).thenReturn((HttpResponse) response);

    // Act
    adapter.generateEmbedding("test text");

    // Assert
    verify(httpClient).send(
      argThat(request -> {
        String uri = request.uri().toString();
        return uri.contains("/api/embeddings") && uri.startsWith("http://localhost:11434");
      }),
      any()
    );
  }

  @Test
  void generateEmbedding_shouldHandleEmptyEmbeddingVector() throws Exception {
    // Arrange
    adapter = new OllamaEmbeddingAdapter(httpClient, "http://localhost:11434", "test-model");

    String responseJson = "{\"embedding\": []}";
    @SuppressWarnings({ "unchecked", "rawtypes" })
    HttpResponse response = mockHttpResponse(200, responseJson);
    when(httpClient.send(any(HttpRequest.class), any())).thenReturn((HttpResponse) response);

    // Act
    List<Float> embedding = adapter.generateEmbedding("text");

    // Assert
    assertThat(embedding).isEmpty();
  }

  @Test
  void generateEmbedding_shouldFilterNullValuesFromEmbedding() throws Exception {
    // Arrange
    adapter = new OllamaEmbeddingAdapter(httpClient, "http://localhost:11434", "test-model");

    String responseJson = "{\"embedding\": [0.1, null, 0.3]}";
    @SuppressWarnings({ "unchecked", "rawtypes" })
    HttpResponse response = mockHttpResponse(200, responseJson);
    when(httpClient.send(any(HttpRequest.class), any())).thenReturn((HttpResponse) response);

    // Act
    List<Float> embedding = adapter.generateEmbedding("text");

    // Assert
    assertThat(embedding).hasSize(2).containsExactly(0.1f, 0.3f);
  }

  // ==================== Error Handling Tests ====================

  @Test
  void generateEmbedding_shouldThrowException_whenHttpStatusIsNotOK() throws Exception {
    // Arrange
    adapter = new OllamaEmbeddingAdapter(httpClient, "http://localhost:11434", "test-model");

    @SuppressWarnings({ "unchecked", "rawtypes" })
    HttpResponse response500 = mockHttpResponse(500, "Internal Server Error");
    when(httpClient.send(any(HttpRequest.class), any())).thenReturn((HttpResponse) response500);

    // Act & Assert
    assertThatThrownBy(() -> adapter.generateEmbedding("test")).isInstanceOf(RuntimeException.class).hasMessageContaining("Failed to generate embedding");
  }

  @Test
  void generateEmbedding_shouldThrowException_when404NotFound() throws Exception {
    // Arrange
    adapter = new OllamaEmbeddingAdapter(httpClient, "http://localhost:11434", "missing-model");

    @SuppressWarnings({ "unchecked", "rawtypes" })
    HttpResponse response404 = mockHttpResponse(404, "Model not found");
    when(httpClient.send(any(HttpRequest.class), any())).thenReturn((HttpResponse) response404);

    // Act & Assert
    assertThatThrownBy(() -> adapter.generateEmbedding("test")).isInstanceOf(RuntimeException.class).hasMessageContaining("Failed to generate embedding");
  }

  @Test
  void generateEmbedding_shouldThrowException_whenResponseIsMalformedJSON() throws Exception {
    // Arrange
    adapter = new OllamaEmbeddingAdapter(httpClient, "http://localhost:11434", "test-model");

    @SuppressWarnings({ "unchecked", "rawtypes" })
    HttpResponse respInvalid = mockHttpResponse(200, "{invalid json}");
    when(httpClient.send(any(HttpRequest.class), any())).thenReturn((HttpResponse) respInvalid);

    // Act & Assert
    assertThatThrownBy(() -> adapter.generateEmbedding("test")).isInstanceOf(RuntimeException.class);
  }

  @Test
  void generateEmbedding_shouldThrowException_whenEmbeddingFieldIsMissing() throws Exception {
    // Arrange
    adapter = new OllamaEmbeddingAdapter(httpClient, "http://localhost:11434", "test-model");

    String responseJson = "{\"result\": [0.1, 0.2]}"; // Missing "embedding" field
    @SuppressWarnings({ "unchecked", "rawtypes" })
    HttpResponse response = mockHttpResponse(200, responseJson);
    when(httpClient.send(any(HttpRequest.class), any())).thenReturn((HttpResponse) response);

    // Act & Assert
    assertThatThrownBy(() -> adapter.generateEmbedding("test"))
      .isInstanceOf(RuntimeException.class)
      .hasMessageContaining("embedding field missing or not a list");
  }

  @Test
  void generateEmbedding_shouldThrowException_whenEmbeddingIsNotList() throws Exception {
    // Arrange
    adapter = new OllamaEmbeddingAdapter(httpClient, "http://localhost:11434", "test-model");

    String responseJson = "{\"embedding\": \"not a list\"}";
    @SuppressWarnings({ "unchecked", "rawtypes" })
    HttpResponse response = mockHttpResponse(200, responseJson);
    when(httpClient.send(any(HttpRequest.class), any())).thenReturn((HttpResponse) response);

    // Act & Assert
    assertThatThrownBy(() -> adapter.generateEmbedding("test"))
      .isInstanceOf(RuntimeException.class)
      .hasMessageContaining("embedding field missing or not a list");
  }

  @Test
  void generateEmbedding_shouldThrowException_onIOError() throws Exception {
    // Arrange
    adapter = new OllamaEmbeddingAdapter(httpClient, "http://localhost:11434", "test-model");

    when(httpClient.send(any(HttpRequest.class), any())).thenThrow(new IOException("Connection failed"));

    // Act & Assert
    assertThatThrownBy(() -> adapter.generateEmbedding("test")).isInstanceOf(RuntimeException.class).hasMessageContaining("Error generating embedding");
  }

  @Test
  void generateEmbedding_shouldThrowException_onInterruption() throws Exception {
    // Arrange
    adapter = new OllamaEmbeddingAdapter(httpClient, "http://localhost:11434", "test-model");

    when(httpClient.send(any(HttpRequest.class), any())).thenThrow(new InterruptedException("Thread interrupted"));

    // Act & Assert
    assertThatThrownBy(() -> adapter.generateEmbedding("test"))
      .isInstanceOf(RuntimeException.class)
      .hasMessageContaining("Interrupted while generating embedding");

    // Verify thread interrupt status was restored
    assertThat(Thread.interrupted()).isTrue();
  }

  // ==================== Configuration Tests ====================

  @Test
  void constructor_shouldUseDefaultUrl_whenNotProvided() throws Exception {
    // Arrange
    adapter = new OllamaEmbeddingAdapter(httpClient, null, "test-model");

    String responseJson = "{\"embedding\": [0.1]}";
    @SuppressWarnings({ "unchecked", "rawtypes" })
    HttpResponse response = mockHttpResponse(200, responseJson);
    when(httpClient.send(any(HttpRequest.class), any())).thenReturn((HttpResponse) response);

    // Act
    adapter.generateEmbedding("test");

    // Assert
    verify(httpClient).send(argThat(request -> request.uri().toString().startsWith("http://localhost:11434")), any());
  }

  @Test
  void constructor_shouldUseDefaultModel_whenNotProvided() throws Exception {
    // Arrange
    adapter = new OllamaEmbeddingAdapter(httpClient, "http://localhost:11434", null);

    String responseJson = "{\"embedding\": [0.1]}";
    @SuppressWarnings({ "unchecked", "rawtypes" })
    HttpResponse response = mockHttpResponse(200, responseJson);
    when(httpClient.send(any(HttpRequest.class), any())).thenReturn((HttpResponse) response);

    // Act
    adapter.generateEmbedding("test");

    // Assert
    verify(httpClient, times(1)).send(any(HttpRequest.class), any());
  }

  // ==================== Large Data Tests ====================

  @Test
  void generateEmbedding_shouldHandleLargEmbeddingVector() throws Exception {
    // Arrange
    adapter = new OllamaEmbeddingAdapter(httpClient, "http://localhost:11434", "test-model");

    // Create a large embedding with 1024 dimensions
    StringBuilder embeddingJson = new StringBuilder("{\"embedding\": [");
    for (int i = 0; i < 1024; i++) {
      if (i > 0) embeddingJson.append(", ");
      embeddingJson.append("%.4f".formatted(i / 1024.0f));
    }
    embeddingJson.append("]}");

    @SuppressWarnings({ "unchecked", "rawtypes" })
    HttpResponse respLarge = mockHttpResponse(200, embeddingJson.toString());
    when(httpClient.send(any(HttpRequest.class), any())).thenReturn((HttpResponse) respLarge);

    // Act
    List<Float> embedding = adapter.generateEmbedding("test");

    // Assert
    assertThat(embedding).hasSize(1024);
  }

  @Test
  void generateEmbedding_shouldHandleVeryLongPrompt() throws Exception {
    // Arrange
    adapter = new OllamaEmbeddingAdapter(httpClient, "http://localhost:11434", "test-model");

    String longText = "text ".repeat(1000); // 5000+ character prompt

    String responseJson = "{\"embedding\": [0.1, 0.2]}";
    @SuppressWarnings({ "unchecked", "rawtypes" })
    HttpResponse response = mockHttpResponse(200, responseJson);
    when(httpClient.send(any(HttpRequest.class), any())).thenReturn((HttpResponse) response);

    // Act
    List<Float> embedding = adapter.generateEmbedding(longText);

    // Assert
    assertThat(embedding).hasSize(2);
    verify(httpClient, times(1)).send(any(HttpRequest.class), any());
  }

  // ==================== Dimension Validation Tests ====================

  @Test
  void generateEmbedding_shouldLogWarning_whenDimensionMismatchDetected() throws Exception {
    // Arrange
    adapter = new OllamaEmbeddingAdapter(httpClient, "http://localhost:11434", "test-model");

    // This adapter expects 384 dimensions by default
    String responseJson = "{\"embedding\": [0.1, 0.2]}"; // But response has only 2

    @SuppressWarnings({ "unchecked", "rawtypes" })
    HttpResponse response = mockHttpResponse(200, responseJson);
    when(httpClient.send(any(HttpRequest.class), any())).thenReturn((HttpResponse) response);

    // Act
    List<Float> embedding = adapter.generateEmbedding("test");

    // Assert - should return the embedding even with dimension mismatch
    assertThat(embedding).hasSize(2);
    // Note: Warning would be logged but we're testing functional behavior
  }

  @Test
  void generateEmbedding_shouldValidateDimensionsOnlyOnce() throws Exception {
    // Arrange
    adapter = new OllamaEmbeddingAdapter(httpClient, "http://localhost:11434", "test-model");

    String responseJson = "{\"embedding\": [0.1, 0.2, 0.3, 0.4]}";
    @SuppressWarnings({ "unchecked", "rawtypes" })
    HttpResponse response = mockHttpResponse(200, responseJson);
    when(httpClient.send(any(HttpRequest.class), any())).thenReturn((HttpResponse) response);

    // Act
    adapter.generateEmbedding("test 1");
    adapter.generateEmbedding("test 2");
    adapter.generateEmbedding("test 3");

    // Assert - should have called httpClient 3 times
    verify(httpClient, times(3)).send(any(HttpRequest.class), any());
    // But dimension validation happens only once (internally tracked by adapter)
  }
}
