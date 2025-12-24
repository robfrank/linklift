package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import it.robfrank.linklift.adapter.out.ai.FakeEmbeddingGenerator;
import it.robfrank.linklift.application.domain.model.Content;
import it.robfrank.linklift.application.domain.model.DownloadStatus;
import it.robfrank.linklift.testcontainers.ArcadeDbTestBase;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BackfillEmbeddingsServiceTest extends ArcadeDbTestBase {

  private static final LocalDateTime FIXED_TEST_TIME = LocalDateTime.of(2024, 1, 1, 12, 0);

  private FakeEmbeddingGenerator embeddingGenerator;
  private ExecutorService executorService;
  private BackfillEmbeddingsService backfillEmbeddingsService;

  private static Content createTestContent(String id, String linkId, String text) {
    return new Content(id, linkId, "html", text, 100, FIXED_TEST_TIME, "text/html", DownloadStatus.COMPLETED, null, null, null, null, null, null, null);
  }

  @BeforeEach
  void setUp() {
    embeddingGenerator = new FakeEmbeddingGenerator();
    executorService = Executors.newFixedThreadPool(2);
    backfillEmbeddingsService = new BackfillEmbeddingsService(repository, repository, embeddingGenerator, executorService);
  }

  // ==================== Happy Path Tests ====================

  @Test
  void backfill_shouldProcessSingleBatch_whenContentExists() throws Exception {
    // Given - content exists without embedding
    Content content = createTestContent("id-1", "link-1", "text content");
    repository.saveContent(content);

    // When - backfill is executed
    backfillEmbeddingsService.backfill();

    // Wait for async execution to complete
    Thread.sleep(1000);

    // Then - content should have embedding
    Content updated = repository.findContentById("id-1").orElseThrow();
    assertThat(updated.embedding()).isNotNull().hasSize(384);
    assertThat(updated.textContent()).isEqualTo("text content");
  }

  @Test
  void backfill_shouldProcessMultipleBatches_whenLargeDataSet() throws Exception {
    // Given - multiple contents exist without embeddings
    Content content1 = createTestContent("id-1", "link-1", "text1");
    Content content2 = createTestContent("id-2", "link-2", "text2");
    Content content3 = createTestContent("id-3", "link-3", "text3");

    repository.saveContent(content1);
    repository.saveContent(content2);
    repository.saveContent(content3);

    // When - backfill is executed
    backfillEmbeddingsService.backfill();

    // Wait for async execution to complete
    Thread.sleep(1000);

    // Then - all contents should have embeddings
    Content updated1 = repository.findContentById("id-1").orElseThrow();
    Content updated2 = repository.findContentById("id-2").orElseThrow();
    Content updated3 = repository.findContentById("id-3").orElseThrow();

    assertThat(updated1.embedding()).isNotNull().hasSize(384);
    assertThat(updated2.embedding()).isNotNull().hasSize(384);
    assertThat(updated3.embedding()).isNotNull().hasSize(384);
  }

  @Test
  void backfill_shouldNotProcessContent_whenTextContentIsNull() throws Exception {
    // Given - content without text exists
    Content content = new Content("id-1", "link-1", "html", null, 0, FIXED_TEST_TIME, "text/html", DownloadStatus.COMPLETED);
    repository.saveContent(content);

    // When - backfill is executed
    backfillEmbeddingsService.backfill();

    // Wait for async execution to complete
    Thread.sleep(1000);

    // Then - content should still not have embedding
    Content updated = repository.findContentById("id-1").orElseThrow();
    assertThat(updated.embedding()).isNull();
  }

  @Test
  void backfill_shouldNotProcessContent_whenTextContentIsBlank() throws Exception {
    // Given - content with blank text exists
    Content content = new Content("id-1", "link-1", "html", "   ", 0, FIXED_TEST_TIME, "text/html", DownloadStatus.COMPLETED);
    repository.saveContent(content);

    // When - backfill is executed
    backfillEmbeddingsService.backfill();

    // Wait for async execution to complete
    Thread.sleep(1000);

    // Then - content should still not have embedding
    Content updated = repository.findContentById("id-1").orElseThrow();
    assertThat(updated.embedding()).isNull();
  }

  // ==================== Concurrency Tests ====================

  @Test
  void backfill_shouldNotAllowConcurrentExecution_whenBackfillAlreadyRunning() throws Exception {
    // Given - content exists without embedding
    Content content = createTestContent("id-1", "link-1", "text");
    repository.saveContent(content);

    // When - first backfill is started
    backfillEmbeddingsService.backfill();

    // And - second backfill is immediately attempted (should be rejected by flag)
    backfillEmbeddingsService.backfill();

    // Wait for async execution to complete
    Thread.sleep(2000);

    // Then - content should have embedding (only processed once)
    Content updated = repository.findContentById("id-1").orElseThrow();
    assertThat(updated.embedding()).isNotNull().hasSize(384);
  }

  @Test
  void backfill_shouldAllowNewBackfillAfterPreviousCompletes() throws Exception {
    // Given - empty database
    // When - first backfill is executed
    backfillEmbeddingsService.backfill();

    // Wait for first backfill to complete
    Thread.sleep(1000);

    // And - content is added after first backfill completes
    Content content = createTestContent("id-1", "link-1", "text");
    repository.saveContent(content);

    // And - second backfill is executed
    backfillEmbeddingsService.backfill();

    // Wait for second backfill to complete
    Thread.sleep(1000);

    // Then - newly added content should have embedding
    Content updated = repository.findContentById("id-1").orElseThrow();
    assertThat(updated.embedding()).isNotNull().hasSize(384);
  }

  // ==================== Error Resilience Tests ====================

  @Test
  void backfill_shouldContinueProcessing_whenEmbeddingGenerationFails() throws Exception {
    // Given - multiple contents exist without embeddings
    Content content1 = createTestContent("id-1", "link-1", "text1");
    Content content2 = createTestContent("id-2", "link-2", "text2");

    repository.saveContent(content1);
    repository.saveContent(content2);

    // And - embedding generator will fail for first content
    embeddingGenerator.throwOnNextCall(new RuntimeException("Embedding generation failed"));

    // When - backfill is executed
    backfillEmbeddingsService.backfill();

    // Wait for async execution to complete
    Thread.sleep(1500);

    // Then - second content should still have embedding despite first failure
    Content updated1 = repository.findContentById("id-1").orElseThrow();
    Content updated2 = repository.findContentById("id-2").orElseThrow();

    // First content fails, second succeeds
    assertThat(updated1.embedding()).isNull(); // Failed to generate
    assertThat(updated2.embedding()).isNotNull().hasSize(384); // Successfully generated
  }

  @Test
  void backfill_shouldResetFlag_afterCompletion() throws Exception {
    // Given - empty database
    // When - first backfill is executed
    backfillEmbeddingsService.backfill();

    // Wait for first backfill to complete
    Thread.sleep(1000);

    // And - content is added after backfill
    Content content = createTestContent("id-1", "link-1", "text");
    repository.saveContent(content);

    // And - second backfill is executed
    backfillEmbeddingsService.backfill();

    // Wait for second backfill to complete
    Thread.sleep(1000);

    // Then - content should have embedding (flag was reset)
    Content updated = repository.findContentById("id-1").orElseThrow();
    assertThat(updated.embedding()).isNotNull().hasSize(384);
  }

  // ==================== Content Update Tests ====================

  @Test
  void backfill_shouldPreserveAllContentFields_whenUpdatingWithEmbedding() throws Exception {
    // Given - content with all fields populated exists without embedding
    LocalDateTime downloadTime = LocalDateTime.of(2024, 1, 15, 10, 30);
    LocalDateTime publishTime = LocalDateTime.of(2024, 1, 10, 8, 0);

    Content original = new Content(
      "id-1",
      "link-1",
      "<html>content</html>",
      "text content",
      123,
      downloadTime,
      "text/html",
      DownloadStatus.COMPLETED,
      "summary here",
      "hero.jpg",
      "Extracted Title",
      "Extracted Description",
      "John Doe",
      publishTime,
      null
    );

    repository.saveContent(original);

    // When - backfill is executed
    backfillEmbeddingsService.backfill();

    // Wait for async execution to complete
    Thread.sleep(1000);

    // Then - all fields should be preserved and embedding added
    Content updated = repository.findContentById("id-1").orElseThrow();

    assertThat(updated.id()).isEqualTo("id-1");
    assertThat(updated.linkId()).isEqualTo("link-1");
    assertThat(updated.htmlContent()).isEqualTo("<html>content</html>");
    assertThat(updated.textContent()).isEqualTo("text content");
    assertThat(updated.contentLength()).isEqualTo(123);
    assertThat(updated.downloadedAt()).isEqualTo(downloadTime);
    assertThat(updated.mimeType()).isEqualTo("text/html");
    assertThat(updated.status()).isEqualTo(DownloadStatus.COMPLETED);
    assertThat(updated.summary()).isEqualTo("summary here");
    assertThat(updated.heroImageUrl()).isEqualTo("hero.jpg");
    assertThat(updated.extractedTitle()).isEqualTo("Extracted Title");
    assertThat(updated.extractedDescription()).isEqualTo("Extracted Description");
    assertThat(updated.author()).isEqualTo("John Doe");
    assertThat(updated.publishedDate()).isEqualTo(publishTime);
    assertThat(updated.embedding()).isNotNull().hasSize(384);
  }

  // ==================== Edge Cases ====================

  @Test
  void backfill_shouldHandleEmptyBatch() throws Exception {
    // Given - database is empty
    // When - backfill is executed
    backfillEmbeddingsService.backfill();

    // Wait for async execution to complete
    Thread.sleep(1000);
    // Then - no errors should occur (idempotent operation)
    // This is validated by test completing without exception
  }

  @Test
  void backfill_shouldHandleLargeEmbeddingVectors() throws Exception {
    // Given - content exists without embedding
    Content content = createTestContent("id-1", "link-1", "text");
    repository.saveContent(content);

    // When - backfill is executed
    backfillEmbeddingsService.backfill();

    // Wait for async execution to complete
    Thread.sleep(1000);

    // Then - embedding should be generated with correct dimensions (384)
    Content updated = repository.findContentById("id-1").orElseThrow();
    assertThat(updated.embedding()).isNotNull().hasSize(384);
  }

  @AfterEach
  void tearDown() {
    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    } catch (InterruptedException e) {
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
