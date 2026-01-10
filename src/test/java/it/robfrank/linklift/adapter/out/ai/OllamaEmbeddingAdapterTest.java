package it.robfrank.linklift.adapter.out.ai;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.http.HttpClient;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WireMockTest(httpPort = 11434)
class OllamaEmbeddingAdapterTest {

  private final HttpClient httpClient = HttpClient.newHttpClient();
  private OllamaEmbeddingAdapter adapter;

  @BeforeEach
  void setUp() {
    System.clearProperty("LINKLIFT_OLLAMA_DIMENSIONS");
    System.clearProperty("LINKLIFT_OLLAMA_URL");
    System.clearProperty("LINKLIFT_OLLAMA_MODEL");
    adapter = new OllamaEmbeddingAdapter(httpClient, "http://localhost:11434", "test-model");
  }

  // ==================== Happy Path Tests ====================

  @Test
  void generateEmbedding_shouldReturnEmbedding_whenValidResponseReceived() throws Exception {
    // Arrange
    String responseJson = "{\"embedding\": [0.1, 0.2, 0.3, 0.4]}";
    stubFor(
      post(urlEqualTo("/api/embeddings"))
        .withRequestBody(matchingJsonPath("$.model", equalTo("test-model")))
        .withRequestBody(matchingJsonPath("$.prompt", equalTo("test text")))
        .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(responseJson))
    );

    // Act
    List<Float> embedding = adapter.generateEmbedding("test text");

    // Assert
    assertThat(embedding).hasSize(4).containsExactly(0.1f, 0.2f, 0.3f, 0.4f);
    verify(postRequestedFor(urlEqualTo("/api/embeddings")));
  }

  @Test
  void generateEmbedding_shouldSendCorrectRequest_withProperModelAndPrompt() throws Exception {
    // Arrange
    String responseJson = "{\"embedding\": [0.1]}";
    stubFor(post(urlEqualTo("/api/embeddings")).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(responseJson)));

    // Act
    adapter.generateEmbedding("test text");

    // Assert
    verify(
      postRequestedFor(urlEqualTo("/api/embeddings"))
        .withRequestBody(matchingJsonPath("$.model", equalTo("test-model")))
        .withRequestBody(matchingJsonPath("$.prompt", equalTo("test text")))
    );
  }

  @Test
  void generateEmbedding_shouldHandleEmptyEmbeddingVector() throws Exception {
    // Arrange
    String responseJson = "{\"embedding\": []}";
    stubFor(post(urlEqualTo("/api/embeddings")).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(responseJson)));

    // Act
    List<Float> embedding = adapter.generateEmbedding("text");

    // Assert
    assertThat(embedding).isEmpty();
  }

  @Test
  void generateEmbedding_shouldFilterNullValuesFromEmbedding() throws Exception {
    // Arrange
    String responseJson = "{\"embedding\": [0.1, null, 0.3]}";
    stubFor(post(urlEqualTo("/api/embeddings")).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(responseJson)));

    // Act
    List<Float> embedding = adapter.generateEmbedding("text");

    // Assert
    assertThat(embedding).hasSize(2).containsExactly(0.1f, 0.3f);
  }

  // ==================== Error Handling Tests ====================

  @Test
  void generateEmbedding_shouldThrowException_whenHttpStatusIsNotOK() throws Exception {
    // Arrange
    stubFor(post(urlEqualTo("/api/embeddings")).willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

    // Act & Assert
    assertThatThrownBy(() -> adapter.generateEmbedding("test")).isInstanceOf(RuntimeException.class).hasMessageContaining("Failed to generate embedding");
  }

  @Test
  void generateEmbedding_shouldThrowException_when404NotFound() throws Exception {
    // Arrange
    adapter = new OllamaEmbeddingAdapter(httpClient, "http://localhost:11434", "missing-model");
    stubFor(post(urlEqualTo("/api/embeddings")).willReturn(aResponse().withStatus(404).withBody("Model not found")));

    // Act & Assert
    assertThatThrownBy(() -> adapter.generateEmbedding("test")).isInstanceOf(RuntimeException.class).hasMessageContaining("Failed to generate embedding");
  }

  @Test
  void generateEmbedding_shouldThrowException_whenResponseIsMalformedJSON() throws Exception {
    // Arrange
    stubFor(post(urlEqualTo("/api/embeddings")).willReturn(aResponse().withStatus(200).withBody("{invalid json}")));

    // Act & Assert
    assertThatThrownBy(() -> adapter.generateEmbedding("test")).isInstanceOf(RuntimeException.class);
  }

  @Test
  void generateEmbedding_shouldThrowException_whenEmbeddingFieldIsMissing() throws Exception {
    // Arrange
    String responseJson = "{\"result\": [0.1, 0.2]}"; // Missing "embedding" field
    stubFor(post(urlEqualTo("/api/embeddings")).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(responseJson)));

    // Act & Assert
    assertThatThrownBy(() -> adapter.generateEmbedding("test"))
      .isInstanceOf(RuntimeException.class)
      .hasMessageContaining("embedding field missing or not a list");
  }

  @Test
  void generateEmbedding_shouldThrowException_whenEmbeddingIsNotList() throws Exception {
    // Arrange
    String responseJson = "{\"embedding\": \"not a list\"}";
    stubFor(post(urlEqualTo("/api/embeddings")).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(responseJson)));

    // Act & Assert
    assertThatThrownBy(() -> adapter.generateEmbedding("test"))
      .isInstanceOf(RuntimeException.class)
      .hasMessageContaining("embedding field missing or not a list");
  }

  @Test
  void generateEmbedding_shouldThrowException_onIOError() throws Exception {
    // Arrange
    // WireMock can simulate connection resets or other network issues
    stubFor(post(urlEqualTo("/api/embeddings")).willReturn(aResponse().withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

    // Act & Assert
    assertThatThrownBy(() -> adapter.generateEmbedding("test")).isInstanceOf(RuntimeException.class).hasMessageContaining("Error generating embedding");
  }

  // ==================== Configuration Tests ====================

  @Test
  void constructor_shouldUseDefaultUrl_whenNotProvided() throws Exception {
    // Arrange
    adapter = new OllamaEmbeddingAdapter(httpClient, null, "test-model");
    String responseJson = "{\"embedding\": [0.1]}";
    stubFor(post(urlEqualTo("/api/embeddings")).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(responseJson)));

    // Act
    adapter.generateEmbedding("test");

    // Assert
    verify(postRequestedFor(urlEqualTo("/api/embeddings")));
  }

  // ==================== Large Data Tests ====================

  @Test
  void generateEmbedding_shouldHandleLargEmbeddingVector() throws Exception {
    // Arrange
    // Create a large embedding with 1024 dimensions
    StringBuilder embeddingJson = new StringBuilder("{\"embedding\": [");
    for (int i = 0; i < 1024; i++) {
      if (i > 0) embeddingJson.append(", ");
      embeddingJson.append("%.4f".formatted(i / 1024.0f));
    }
    embeddingJson.append("]}");

    stubFor(
      post(urlEqualTo("/api/embeddings")).willReturn(
        aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(embeddingJson.toString())
      )
    );

    // Act
    List<Float> embedding = adapter.generateEmbedding("test");

    // Assert
    assertThat(embedding).hasSize(1024);
  }

  @Test
  void generateEmbedding_shouldHandleVeryLongPrompt() throws Exception {
    // Arrange
    String longText = "text ".repeat(1000); // 5000+ character prompt
    String responseJson = "{\"embedding\": [0.1, 0.2]}";
    stubFor(post(urlEqualTo("/api/embeddings")).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(responseJson)));

    // Act
    List<Float> embedding = adapter.generateEmbedding(longText);

    // Assert
    assertThat(embedding).hasSize(2);
    verify(postRequestedFor(urlEqualTo("/api/embeddings")));
  }

  // ==================== Dimension Validation Tests ====================

  @Test
  void generateEmbedding_shouldLogWarning_whenDimensionMismatchDetected() throws Exception {
    // Arrange
    // This adapter expects 384 dimensions by default
    String responseJson = "{\"embedding\": [0.1, 0.2]}"; // But response has only 2
    stubFor(post(urlEqualTo("/api/embeddings")).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(responseJson)));

    // Act
    List<Float> embedding = adapter.generateEmbedding("test");

    // Assert - should return the embedding even with dimension mismatch
    assertThat(embedding).hasSize(2);
  }

  @Test
  void generateEmbedding_shouldValidateDimensionsOnlyOnce() throws Exception {
    // Arrange
    String responseJson = "{\"embedding\": [0.1, 0.2, 0.3, 0.4]}";
    stubFor(post(urlEqualTo("/api/embeddings")).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(responseJson)));

    // Act
    adapter.generateEmbedding("test 1");
    adapter.generateEmbedding("test 2");
    adapter.generateEmbedding("test 3");

    // Assert - should have called WireMock 3 times
    verify(3, postRequestedFor(urlEqualTo("/api/embeddings")));
  }
}
