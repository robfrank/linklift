package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.exception.ValidationException;
import it.robfrank.linklift.application.domain.model.Content;
import it.robfrank.linklift.application.domain.model.DownloadStatus;
import it.robfrank.linklift.application.port.out.EmbeddingGenerator;
import it.robfrank.linklift.application.port.out.LoadContentPort;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchContentServiceTest {

  private static final LocalDateTime FIXED_TEST_TIME = LocalDateTime.of(2024, 1, 1, 12, 0);

  @Mock
  private LoadContentPort loadContentPort;

  @Mock
  private EmbeddingGenerator embeddingGenerator;

  private SearchContentService searchContentService;

  private static Content createTestContent(String id, String linkId) {
    return new Content(id, linkId, null, "test content", null, FIXED_TEST_TIME, null, DownloadStatus.COMPLETED);
  }

  @BeforeEach
  void setUp() {
    searchContentService = new SearchContentService(loadContentPort, embeddingGenerator);
  }

  // ==================== Happy Path Tests ====================

  @Test
  void search_shouldReturnResults_whenValidQueryProvided() {
    // Arrange
    String query = "test query";
    int limit = 10;
    List<Float> queryVector = List.of(0.1f, 0.2f, 0.3f);
    Content resultContent = createTestContent("id-1", "link-1");
    List<Content> expectedResults = List.of(resultContent);

    when(embeddingGenerator.generateEmbedding(query)).thenReturn(queryVector);
    when(loadContentPort.findSimilar(queryVector, limit)).thenReturn(expectedResults);

    // Act
    List<Content> results = searchContentService.search(query, limit);

    // Assert
    assertThat(results).isEqualTo(expectedResults);
    verify(embeddingGenerator, times(1)).generateEmbedding(query);
    verify(loadContentPort, times(1)).findSimilar(queryVector, limit);
  }

  @Test
  void search_shouldReturnEmptyList_whenNoMatchesFound() {
    // Arrange
    String query = "no matches";
    int limit = 10;
    List<Float> queryVector = List.of(0.1f, 0.2f);

    when(embeddingGenerator.generateEmbedding(query)).thenReturn(queryVector);
    when(loadContentPort.findSimilar(queryVector, limit)).thenReturn(List.of());

    // Act
    List<Content> results = searchContentService.search(query, limit);

    // Assert
    assertThat(results).isEmpty();
    verify(embeddingGenerator, times(1)).generateEmbedding(query);
    verify(loadContentPort, times(1)).findSimilar(queryVector, limit);
  }

  @Test
  void search_shouldRespectLimitParameter() {
    // Arrange
    String query = "test";
    int limit = 5;
    List<Float> queryVector = List.of(0.1f);

    when(embeddingGenerator.generateEmbedding(query)).thenReturn(queryVector);
    when(loadContentPort.findSimilar(queryVector, limit)).thenReturn(List.of());

    // Act
    searchContentService.search(query, limit);

    // Assert
    verify(loadContentPort).findSimilar(queryVector, 5);
  }

  @Test
  void search_shouldReturnMultipleResults_whenProvidedByRepository() {
    // Arrange
    String query = "multi result query";
    int limit = 20;
    List<Float> queryVector = List.of(0.5f, 0.6f);

    Content result1 = createTestContent("id-1", "link-1");
    Content result2 = createTestContent("id-2", "link-2");
    Content result3 = createTestContent("id-3", "link-3");

    List<Content> expectedResults = List.of(result1, result2, result3);

    when(embeddingGenerator.generateEmbedding(query)).thenReturn(queryVector);
    when(loadContentPort.findSimilar(queryVector, limit)).thenReturn(expectedResults);

    // Act
    List<Content> results = searchContentService.search(query, limit);

    // Assert
    assertThat(results).hasSize(3).containsExactlyElementsOf(expectedResults);
  }

  // ==================== Validation Tests ====================

  @Test
  void search_shouldThrowValidationException_whenQueryIsNull() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> searchContentService.search(null, 10)).isInstanceOf(ValidationException.class);

    verify(embeddingGenerator, never()).generateEmbedding(any());
    verify(loadContentPort, never()).findSimilar(any(), anyInt());
  }

  @Test
  void search_shouldThrowValidationException_whenQueryIsEmpty() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> searchContentService.search("", 10)).isInstanceOf(ValidationException.class);

    verify(embeddingGenerator, never()).generateEmbedding(any());
    verify(loadContentPort, never()).findSimilar(any(), anyInt());
  }

  @Test
  void search_shouldThrowValidationException_whenQueryIsBlank() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> searchContentService.search("   ", 10)).isInstanceOf(ValidationException.class);

    verify(embeddingGenerator, never()).generateEmbedding(any());
    verify(loadContentPort, never()).findSimilar(any(), anyInt());
  }

  // ==================== Error Handling Tests ====================

  @Test
  void search_shouldPropagateEmbeddingGenerationError() {
    // Arrange
    String query = "test";
    RuntimeException embeddingException = new RuntimeException("Ollama service unavailable");

    when(embeddingGenerator.generateEmbedding(query)).thenThrow(embeddingException);

    // Act & Assert
    assertThatThrownBy(() -> searchContentService.search(query, 10)).isInstanceOf(RuntimeException.class).hasMessage("Ollama service unavailable");

    verify(embeddingGenerator, times(1)).generateEmbedding(query);
    verify(loadContentPort, never()).findSimilar(any(), anyInt());
  }

  @Test
  void search_shouldPropagateRepositoryError() {
    // Arrange
    String query = "test";
    List<Float> queryVector = List.of(0.1f);
    RuntimeException repositoryException = new RuntimeException("Database connection failed");

    when(embeddingGenerator.generateEmbedding(query)).thenReturn(queryVector);
    when(loadContentPort.findSimilar(queryVector, 10)).thenThrow(repositoryException);

    // Act & Assert
    assertThatThrownBy(() -> searchContentService.search(query, 10)).isInstanceOf(RuntimeException.class).hasMessage("Database connection failed");

    verify(embeddingGenerator, times(1)).generateEmbedding(query);
    verify(loadContentPort, times(1)).findSimilar(queryVector, 10);
  }

  // ==================== Edge Case Tests ====================

  @Test
  void search_shouldHandleZeroLimit() {
    // Arrange
    String query = "test";
    List<Float> queryVector = List.of(0.1f);

    when(embeddingGenerator.generateEmbedding(query)).thenReturn(queryVector);
    when(loadContentPort.findSimilar(queryVector, 0)).thenReturn(List.of());

    // Act
    List<Content> results = searchContentService.search(query, 0);

    // Assert
    assertThat(results).isEmpty();
    verify(loadContentPort).findSimilar(queryVector, 0);
  }

  @Test
  void search_shouldHandleNegativeLimit() {
    // Arrange
    String query = "test";
    List<Float> queryVector = List.of(0.1f);

    when(embeddingGenerator.generateEmbedding(query)).thenReturn(queryVector);
    when(loadContentPort.findSimilar(queryVector, -1)).thenReturn(List.of());

    // Act
    List<Content> results = searchContentService.search(query, -1);

    // Assert
    assertThat(results).isEmpty();
    verify(loadContentPort).findSimilar(queryVector, -1);
  }

  @Test
  void search_shouldHandleVeryLargeLimit() {
    // Arrange
    String query = "test";
    int largeLimit = Integer.MAX_VALUE;
    List<Float> queryVector = List.of(0.1f);

    when(embeddingGenerator.generateEmbedding(query)).thenReturn(queryVector);
    when(loadContentPort.findSimilar(queryVector, largeLimit)).thenReturn(List.of());

    // Act
    List<Content> results = searchContentService.search(query, largeLimit);

    // Assert
    assertThat(results).isEmpty();
    verify(loadContentPort).findSimilar(queryVector, largeLimit);
  }

  @Test
  void search_shouldHandleQueryWithSpecialCharacters() {
    // Arrange
    String query = "@#$%^&*()_+ test query!";
    List<Float> queryVector = List.of(0.1f, 0.2f);
    Content result = createTestContent("id-1", "link-1");

    when(embeddingGenerator.generateEmbedding(query)).thenReturn(queryVector);
    when(loadContentPort.findSimilar(queryVector, 10)).thenReturn(List.of(result));

    // Act
    List<Content> results = searchContentService.search(query, 10);

    // Assert
    assertThat(results).hasSize(1).contains(result);
    verify(embeddingGenerator, times(1)).generateEmbedding(query);
  }

  @Test
  void search_shouldHandleVeryLongQuery() {
    // Arrange
    String query = "a".repeat(1000);
    List<Float> queryVector = List.of(0.1f);

    when(embeddingGenerator.generateEmbedding(query)).thenReturn(queryVector);
    when(loadContentPort.findSimilar(queryVector, 10)).thenReturn(List.of());

    // Act
    List<Content> results = searchContentService.search(query, 10);

    // Assert
    assertThat(results).isEmpty();
    verify(embeddingGenerator, times(1)).generateEmbedding(query);
  }

  @Test
  void search_shouldHandleEmptyEmbeddingVector() {
    // Arrange
    String query = "test";
    List<Float> emptyVector = List.of();

    when(embeddingGenerator.generateEmbedding(query)).thenReturn(emptyVector);
    when(loadContentPort.findSimilar(emptyVector, 10)).thenReturn(List.of());

    // Act
    List<Content> results = searchContentService.search(query, 10);

    // Assert
    assertThat(results).isEmpty();
    verify(loadContentPort).findSimilar(emptyVector, 10);
  }
}
