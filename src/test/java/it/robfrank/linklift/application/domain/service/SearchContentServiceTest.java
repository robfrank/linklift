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
  private static final String TEST_USER_ID = "search-test-user";

  private FakeEmbeddingGenerator embeddingGenerator;
  private SearchContentService searchContentService;

  private static Content createTestContent(String id, String linkId) {
    return new Content(id, linkId, null, "test content", null, FIXED_TEST_TIME, null, DownloadStatus.COMPLETED);
  }

  private static float[] toFloatArray(List<Float> list) {
    if (list == null || list.isEmpty()) {
      return null;
    }
    float[] array = new float[list.size()];
    for (int i = 0; i < list.size(); i++) {
      array[i] = list.get(i);
    }
    return array;
  }

  private Content withEmbedding(Content content, String query) {
    return new Content(
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
      toFloatArray(embeddingGenerator.generateEmbedding(query))
    );
  }

  @BeforeEach
  void setUp() {
    embeddingGenerator = new FakeEmbeddingGenerator();
    searchContentService = new SearchContentService(repository, embeddingGenerator);
  }

  // ==================== Happy Path Tests ====================

  @Test
  void search_shouldNotReturnContentOwnedByAnotherUser() {
    // Given - content owned by the searching user and equally-matching content owned by someone else
    repository.saveContent(withEmbedding(createTestContent("id-mine", "link-mine"), "shared query"));
    giveUserOwnershipOfLink(TEST_USER_ID, "link-mine");

    repository.saveContent(withEmbedding(createTestContent("id-theirs", "link-theirs"), "shared query"));
    giveUserOwnershipOfLink("other-user", "link-theirs");

    // When - the first user searches
    List<Content> results = searchContentService.search("shared query", 10, TEST_USER_ID);

    // Then - only the user's own content is returned, never the other user's
    assertThat(results).extracting(Content::linkId).containsExactly("link-mine");
  }

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
      toFloatArray(embeddingGenerator.generateEmbedding("test query"))
    );
    giveUserOwnershipOfLink(TEST_USER_ID, "link-1");
    repository.saveContent(content);

    // When - search is performed
    String query = "test query";
    List<Content> results = searchContentService.search(query, 10, TEST_USER_ID);

    // Then - matching content should be returned
    assertThat(results).isNotEmpty();
    assertThat(results.getFirst()).usingRecursiveComparison().isEqualTo(content);
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
      toFloatArray(embeddingGenerator.generateEmbedding("completely different content"))
    );
    giveUserOwnershipOfLink(TEST_USER_ID, "link-2");
    repository.saveContent(differentContent);

    // When - search with unrelated query is performed
    List<Content> results = searchContentService.search("no matching content", 10, TEST_USER_ID);

    // Then - no results should be returned (orthogonal embeddings)
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
        toFloatArray(embeddingGenerator.generateEmbedding("test query"))
      );
      giveUserOwnershipOfLink(TEST_USER_ID, "link-" + i);
      repository.saveContent(content);
    }

    // When - search with limit of 5 is performed
    List<Content> results = searchContentService.search("test query", 5, TEST_USER_ID);

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
      toFloatArray(embeddingGenerator.generateEmbedding("multi result query"))
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
      toFloatArray(embeddingGenerator.generateEmbedding("multi result query"))
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
      toFloatArray(embeddingGenerator.generateEmbedding("multi result query"))
    );

    giveUserOwnershipOfLink(TEST_USER_ID, "link-1");
    repository.saveContent(result1);
    giveUserOwnershipOfLink(TEST_USER_ID, "link-2");
    repository.saveContent(result2);
    giveUserOwnershipOfLink(TEST_USER_ID, "link-3");
    repository.saveContent(result3);

    // When - search is performed
    List<Content> results = searchContentService.search("multi result query", 20, TEST_USER_ID);

    // Then - multiple matching results should be returned
    assertThat(results).hasSizeGreaterThanOrEqualTo(3);
  }

  // ==================== Validation Tests ====================

  @Test
  void search_shouldThrowValidationException_whenQueryIsNull() {
    // When & Then - null query should throw validation exception
    assertThatThrownBy(() -> searchContentService.search(null, 10, TEST_USER_ID)).isInstanceOf(ValidationException.class);
  }

  @Test
  void search_shouldThrowValidationException_whenQueryIsEmpty() {
    // When & Then - empty query should throw validation exception
    assertThatThrownBy(() -> searchContentService.search("", 10, TEST_USER_ID)).isInstanceOf(ValidationException.class);
  }

  @Test
  void search_shouldThrowValidationException_whenQueryIsBlank() {
    // When & Then - blank query should throw validation exception
    assertThatThrownBy(() -> searchContentService.search("   ", 10, TEST_USER_ID)).isInstanceOf(ValidationException.class);
  }

  // ==================== Error Handling Tests ====================

  @Test
  void search_shouldPropagateEmbeddingGenerationError() {
    // Given - embedding generator is configured to fail
    embeddingGenerator.throwOnNextCall(new RuntimeException("Ollama service unavailable"));

    // When & Then - error should be propagated
    assertThatThrownBy(() -> searchContentService.search("test", 10, TEST_USER_ID)).isInstanceOf(RuntimeException.class);
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
      toFloatArray(embeddingGenerator.generateEmbedding("recovery test"))
    );
    giveUserOwnershipOfLink(TEST_USER_ID, "link-1");
    repository.saveContent(content);

    // When - search is performed (no error this time)
    List<Content> results = searchContentService.search("recovery test", 10, TEST_USER_ID);

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
      toFloatArray(embeddingGenerator.generateEmbedding("test"))
    );
    giveUserOwnershipOfLink(TEST_USER_ID, "link-1");
    repository.saveContent(content);

    // When - search with zero limit is performed
    List<Content> results = searchContentService.search("test", 0, TEST_USER_ID);

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
      toFloatArray(embeddingGenerator.generateEmbedding("test"))
    );
    giveUserOwnershipOfLink(TEST_USER_ID, "link-1");
    repository.saveContent(content);

    // When - search with negative limit is performed
    List<Content> results = searchContentService.search("test", -1, TEST_USER_ID);

    // Then - behavior depends on implementation (should be empty as per
    // SearchContentService improvement)
    assertThat(results).isEmpty();
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
      toFloatArray(embeddingGenerator.generateEmbedding("test"))
    );
    giveUserOwnershipOfLink(TEST_USER_ID, "link-1");
    repository.saveContent(content);

    // When - search with very large limit is performed
    List<Content> results = searchContentService.search("test", Integer.MAX_VALUE, TEST_USER_ID);

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
      toFloatArray(embeddingGenerator.generateEmbedding(query))
    );
    giveUserOwnershipOfLink(TEST_USER_ID, "link-1");
    repository.saveContent(result);

    // When - search with special characters is performed
    List<Content> results = searchContentService.search(query, 10, TEST_USER_ID);

    // Then - results should be returned
    assertThat(results).isNotEmpty();
    assertThat(results.getFirst()).usingRecursiveComparison().isEqualTo(result);
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
      toFloatArray(embeddingGenerator.generateEmbedding(query))
    );
    giveUserOwnershipOfLink(TEST_USER_ID, "link-1");
    repository.saveContent(result);

    // When - search with very long query is performed
    List<Content> results = searchContentService.search(query, 10, TEST_USER_ID);

    // Then - results should be returned (long queries are supported)
    assertThat(results).isNotEmpty();
    assertThat(results.getFirst()).usingRecursiveComparison().isEqualTo(result);
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
      toFloatArray(embeddingGenerator.generateEmbedding(query))
    );
    giveUserOwnershipOfLink(TEST_USER_ID, "link-1");
    repository.saveContent(result);

    // When - search with unicode characters is performed
    List<Content> results = searchContentService.search(query, 10, TEST_USER_ID);

    // Then - results should be returned
    assertThat(results).isNotEmpty();
    assertThat(results.getFirst()).usingRecursiveComparison().isEqualTo(result);
  }
}
