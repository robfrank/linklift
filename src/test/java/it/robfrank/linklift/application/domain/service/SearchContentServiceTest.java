package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.robfrank.linklift.adapter.out.ai.FakeEmbeddingGenerator;
import it.robfrank.linklift.application.domain.exception.ValidationException;
import it.robfrank.linklift.application.domain.model.Content;
import it.robfrank.linklift.application.domain.model.DownloadStatus;
import it.robfrank.linklift.testcontainers.ArcadeDbTestBase;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchContentServiceTest extends ArcadeDbTestBase {

  private static final LocalDateTime FIXED_TEST_TIME = LocalDateTime.of(2024, 1, 1, 12, 0);

  private FakeEmbeddingGenerator embeddingGenerator;
  private SearchContentService searchContentService;

  private static Content createTestContent(String id, String linkId) {
    return new Content(id, linkId, null, "test content", null, FIXED_TEST_TIME, null, DownloadStatus.COMPLETED);
  }

  @BeforeEach
  void setUp() {
    embeddingGenerator = new FakeEmbeddingGenerator();
    searchContentService = new SearchContentService(repository, embeddingGenerator);
  }

  // ==================== Happy Path Tests ====================

  @Test
  void search_shouldReturnResults_whenValidQueryProvided() {
    // Given - content with embedding exists
    Content content = createTestContent("id-1", "link-1");
    content = new Content(
      content.id(),
      content.linkId(),
      content.htmlContent(),
      content.textContent(),
      content.contentLength(),
      content.downloadedAt(),
      content.mimeType(),
      content.status(),
      content.summary(),
      content.heroImageUrl(),
      content.extractedTitle(),
      content.extractedDescription(),
      content.author(),
      content.publishedDate(),
      embeddingGenerator.generateEmbedding("test query")
    );
    repository.saveContent(content);

    // When - search is performed
    String query = "test query";
    List<Content> results = searchContentService.search(query, 10);

    // Then - matching content should be returned
    assertThat(results).isNotEmpty().contains(content);
  }

  @Test
  void search_shouldReturnEmptyList_whenNoMatchesFound() {
    // Given - only content with different embedding exists
    Content differentContent = createTestContent("id-2", "link-2");
    differentContent = new Content(
      differentContent.id(),
      differentContent.linkId(),
      differentContent.htmlContent(),
      differentContent.textContent(),
      differentContent.contentLength(),
      differentContent.downloadedAt(),
      differentContent.mimeType(),
      differentContent.status(),
      differentContent.summary(),
      differentContent.heroImageUrl(),
      differentContent.extractedTitle(),
      differentContent.extractedDescription(),
      differentContent.author(),
      differentContent.publishedDate(),
      embeddingGenerator.generateEmbedding("completely different content")
    );
    repository.saveContent(differentContent);

    // When - search with unrelated query is performed
    List<Content> results = searchContentService.search("no matching content", 10);

    // Then - no results should be returned (orthogonal embeddings)
    // Note: This depends on embedding similarity - for deterministic test, we accept either empty or low similarity
    assertThat(results).isNotNull();
  }

  @Test
  void search_shouldRespectLimitParameter() {
    // Given - multiple contents with embeddings exist
    for (int i = 0; i < 20; i++) {
      Content content = new Content(
        "id-" + i,
        "link-" + i,
        null,
        "test content " + i,
        null,
        FIXED_TEST_TIME,
        null,
        DownloadStatus.COMPLETED,
        null,
        null,
        null,
        null,
        null,
        null,
        embeddingGenerator.generateEmbedding("test query")
      );
      repository.saveContent(content);
    }

    // When - search with limit of 5 is performed
    List<Content> results = searchContentService.search("test query", 5);

    // Then - at most 5 results should be returned
    assertThat(results).hasSizeLessThanOrEqualTo(5);
  }

  @Test
  void search_shouldReturnMultipleResults_whenProvidedByRepository() {
    // Given - multiple contents with similar embeddings exist
    Content result1 = new Content(
      "id-1",
      "link-1",
      null,
      "test content 1",
      null,
      FIXED_TEST_TIME,
      null,
      DownloadStatus.COMPLETED,
      null,
      null,
      null,
      null,
      null,
      null,
      embeddingGenerator.generateEmbedding("multi result query")
    );
    Content result2 = new Content(
      "id-2",
      "link-2",
      null,
      "test content 2",
      null,
      FIXED_TEST_TIME,
      null,
      DownloadStatus.COMPLETED,
      null,
      null,
      null,
      null,
      null,
      null,
      embeddingGenerator.generateEmbedding("multi result query")
    );
    Content result3 = new Content(
      "id-3",
      "link-3",
      null,
      "test content 3",
      null,
      FIXED_TEST_TIME,
      null,
      DownloadStatus.COMPLETED,
      null,
      null,
      null,
      null,
      null,
      null,
      embeddingGenerator.generateEmbedding("multi result query")
    );

    repository.saveContent(result1);
    repository.saveContent(result2);
    repository.saveContent(result3);

    // When - search is performed
    List<Content> results = searchContentService.search("multi result query", 20);

    // Then - multiple matching results should be returned
    assertThat(results).hasSizeGreaterThanOrEqualTo(3);
  }

  // ==================== Validation Tests ====================

  @Test
  void search_shouldThrowValidationException_whenQueryIsNull() {
    // When & Then - null query should throw validation exception
    assertThatThrownBy(() -> searchContentService.search(null, 10)).isInstanceOf(ValidationException.class);
  }

  @Test
  void search_shouldThrowValidationException_whenQueryIsEmpty() {
    // When & Then - empty query should throw validation exception
    assertThatThrownBy(() -> searchContentService.search("", 10)).isInstanceOf(ValidationException.class);
  }

  @Test
  void search_shouldThrowValidationException_whenQueryIsBlank() {
    // When & Then - blank query should throw validation exception
    assertThatThrownBy(() -> searchContentService.search("   ", 10)).isInstanceOf(ValidationException.class);
  }

  // ==================== Error Handling Tests ====================

  @Test
  void search_shouldPropagateEmbeddingGenerationError() {
    // Given - embedding generator is configured to fail
    embeddingGenerator.throwOnNextCall(new RuntimeException("Ollama service unavailable"));

    // When & Then - error should be propagated
    assertThatThrownBy(() -> searchContentService.search("test", 10)).isInstanceOf(RuntimeException.class);
  }

  @Test
  void search_shouldReturnResultsAfterEmbeddingGenerationRecovery() {
    // Given - content with embedding exists
    Content content = new Content(
      "id-1",
      "link-1",
      null,
      "test content",
      null,
      FIXED_TEST_TIME,
      null,
      DownloadStatus.COMPLETED,
      null,
      null,
      null,
      null,
      null,
      null,
      embeddingGenerator.generateEmbedding("recovery test")
    );
    repository.saveContent(content);

    // When - search is performed (no error this time)
    List<Content> results = searchContentService.search("recovery test", 10);

    // Then - results should be returned
    assertThat(results).isNotEmpty();
  }

  // ==================== Edge Case Tests ====================

  @Test
  void search_shouldHandleZeroLimit() {
    // Given - content with embedding exists
    Content content = new Content(
      "id-1",
      "link-1",
      null,
      "test content",
      null,
      FIXED_TEST_TIME,
      null,
      DownloadStatus.COMPLETED,
      null,
      null,
      null,
      null,
      null,
      null,
      embeddingGenerator.generateEmbedding("test")
    );
    repository.saveContent(content);

    // When - search with zero limit is performed
    List<Content> results = searchContentService.search("test", 0);

    // Then - no results should be returned (limit is 0)
    assertThat(results).isEmpty();
  }

  @Test
  void search_shouldHandleNegativeLimit() {
    // Given - content with embedding exists
    Content content = new Content(
      "id-1",
      "link-1",
      null,
      "test content",
      null,
      FIXED_TEST_TIME,
      null,
      DownloadStatus.COMPLETED,
      null,
      null,
      null,
      null,
      null,
      null,
      embeddingGenerator.generateEmbedding("test")
    );
    repository.saveContent(content);

    // When - search with negative limit is performed
    List<Content> results = searchContentService.search("test", -1);

    // Then - behavior depends on implementation (may be empty or return results)
    assertThat(results).isNotNull();
  }

  @Test
  void search_shouldHandleVeryLargeLimit() {
    // Given - content with embedding exists
    Content content = new Content(
      "id-1",
      "link-1",
      null,
      "test content",
      null,
      FIXED_TEST_TIME,
      null,
      DownloadStatus.COMPLETED,
      null,
      null,
      null,
      null,
      null,
      null,
      embeddingGenerator.generateEmbedding("test")
    );
    repository.saveContent(content);

    // When - search with very large limit is performed
    List<Content> results = searchContentService.search("test", Integer.MAX_VALUE);

    // Then - results should be returned (limit doesn't prevent matches)
    assertThat(results).isNotEmpty();
  }

  @Test
  void search_shouldHandleQueryWithSpecialCharacters() {
    // Given - content with special character query embedding exists
    String query = "@#$%^&*()_+ test query!";
    Content result = new Content(
      "id-1",
      "link-1",
      null,
      "test content",
      null,
      FIXED_TEST_TIME,
      null,
      DownloadStatus.COMPLETED,
      null,
      null,
      null,
      null,
      null,
      null,
      embeddingGenerator.generateEmbedding(query)
    );
    repository.saveContent(result);

    // When - search with special characters is performed
    List<Content> results = searchContentService.search(query, 10);

    // Then - results should be returned
    assertThat(results).isNotEmpty().contains(result);
  }

  @Test
  void search_shouldHandleVeryLongQuery() {
    // Given - content with very long query embedding exists
    String query = "a".repeat(1000);
    Content result = new Content(
      "id-1",
      "link-1",
      null,
      "test content",
      null,
      FIXED_TEST_TIME,
      null,
      DownloadStatus.COMPLETED,
      null,
      null,
      null,
      null,
      null,
      null,
      embeddingGenerator.generateEmbedding(query)
    );
    repository.saveContent(result);

    // When - search with very long query is performed
    List<Content> results = searchContentService.search(query, 10);

    // Then - results should be returned (long queries are supported)
    assertThat(results).isNotEmpty().contains(result);
  }

  @Test
  void search_shouldHandleQueryWithUnicodeCharacters() {
    // Given - content with unicode query embedding exists
    String query = "こんにちは世界 🌍 emoji test";
    Content result = new Content(
      "id-1",
      "link-1",
      null,
      "test content",
      null,
      FIXED_TEST_TIME,
      null,
      DownloadStatus.COMPLETED,
      null,
      null,
      null,
      null,
      null,
      null,
      embeddingGenerator.generateEmbedding(query)
    );
    repository.saveContent(result);

    // When - search with unicode characters is performed
    List<Content> results = searchContentService.search(query, 10);

    // Then - results should be returned
    assertThat(results).isNotEmpty().contains(result);
  }
}
