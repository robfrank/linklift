package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.model.Content;
import it.robfrank.linklift.application.domain.model.DownloadStatus;
import it.robfrank.linklift.application.port.out.EmbeddingGenerator;
import it.robfrank.linklift.application.port.out.LoadContentPort;
import it.robfrank.linklift.application.port.out.SaveContentPort;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackfillEmbeddingsServiceTest {

  @Mock
  private LoadContentPort loadContentPort;

  @Mock
  private SaveContentPort saveContentPort;

  @Mock
  private EmbeddingGenerator embeddingGenerator;

  private ExecutorService executorService;
  private BackfillEmbeddingsService backfillEmbeddingsService;

  private static Content createTestContent(String id, String linkId, String text) {
    return new Content(id, linkId, "html", text, 100, LocalDateTime.now(), "text/html", DownloadStatus.COMPLETED, null, null, null, null, null, null, null);
  }

  @BeforeEach
  void setUp() {
    executorService = Executors.newFixedThreadPool(2);
    backfillEmbeddingsService = new BackfillEmbeddingsService(loadContentPort, saveContentPort, embeddingGenerator, executorService);
  }

  // ==================== Happy Path Tests ====================

  @Test
  void backfill_shouldProcessSingleBatch_whenContentExists() throws InterruptedException {
    // Arrange
    Content content = createTestContent("id-1", "link-1", "text content");
    List<Float> embedding = List.of(0.1f, 0.2f, 0.3f);

    when(loadContentPort.findContentsWithoutEmbeddings(100)).thenReturn(List.of(content)).thenReturn(List.of());
    when(embeddingGenerator.generateEmbedding("text content")).thenReturn(embedding);

    // Act
    backfillEmbeddingsService.backfill();

    // Wait for async execution
    Thread.sleep(500);

    // Assert
    verify(loadContentPort, times(2)).findContentsWithoutEmbeddings(100);
    verify(embeddingGenerator, times(1)).generateEmbedding("text content");
    verify(saveContentPort, times(1)).updateContent(argThat(c -> c.embedding() != null));
  }

  @Test
  void backfill_shouldProcessMultipleBatches_whenLargeDataSet() throws InterruptedException {
    // Arrange
    Content content1 = createTestContent("id-1", "link-1", "text1");
    Content content2 = createTestContent("id-2", "link-2", "text2");
    Content content3 = createTestContent("id-3", "link-3", "text3");

    List<Float> embedding = List.of(0.1f);

    when(loadContentPort.findContentsWithoutEmbeddings(100)).thenReturn(List.of(content1, content2)).thenReturn(List.of(content3)).thenReturn(List.of());
    when(embeddingGenerator.generateEmbedding(anyString())).thenReturn(embedding);

    // Act
    backfillEmbeddingsService.backfill();
    Thread.sleep(500);

    // Assert
    verify(loadContentPort, times(3)).findContentsWithoutEmbeddings(100);
    verify(embeddingGenerator, times(3)).generateEmbedding(anyString());
    verify(saveContentPort, times(3)).updateContent(any());
  }

  @Test
  void backfill_shouldNotProcessContent_whenTextContentIsNull() throws InterruptedException {
    // Arrange
    Content content = new Content("id-1", "link-1", "html", null, 0, LocalDateTime.now(), "text/html", DownloadStatus.COMPLETED);

    when(loadContentPort.findContentsWithoutEmbeddings(100)).thenReturn(List.of(content)).thenReturn(List.of());

    // Act
    backfillEmbeddingsService.backfill();
    Thread.sleep(500);

    // Assert
    verify(embeddingGenerator, never()).generateEmbedding(any());
    verify(saveContentPort, never()).updateContent(any());
  }

  @Test
  void backfill_shouldNotProcessContent_whenTextContentIsBlank() throws InterruptedException {
    // Arrange
    Content content = new Content("id-1", "link-1", "html", "   ", 0, LocalDateTime.now(), "text/html", DownloadStatus.COMPLETED);

    when(loadContentPort.findContentsWithoutEmbeddings(100)).thenReturn(List.of(content)).thenReturn(List.of());

    // Act
    backfillEmbeddingsService.backfill();
    Thread.sleep(500);

    // Assert
    verify(embeddingGenerator, never()).generateEmbedding(any());
    verify(saveContentPort, never()).updateContent(any());
  }

  // ==================== Concurrency Tests ====================

  @Test
  void backfill_shouldNotAllowConcurrentExecution_whenBackfillAlreadyRunning() throws InterruptedException {
    // Arrange
    CountDownLatch latch = new CountDownLatch(1);
    Content content = createTestContent("id-1", "link-1", "text");

    when(loadContentPort.findContentsWithoutEmbeddings(100)).thenAnswer(invocation -> {
      latch.countDown(); // Signal that we've started
      Thread.sleep(2000); // Simulate long-running operation
      return List.of();
    });

    // Act
    backfillEmbeddingsService.backfill(); // Start first backfill
    latch.await(); // Wait for it to start
    backfillEmbeddingsService.backfill(); // Try to start second backfill (should be rejected)

    // Wait for first backfill to complete
    Thread.sleep(2500);

    // Assert - loadContentPort should only be called once (not twice)
    verify(loadContentPort, times(1)).findContentsWithoutEmbeddings(100);
  }

  @Test
  void backfill_shouldAllowNewBackfillAfterPreviousCompletes() throws InterruptedException {
    // Arrange
    when(loadContentPort.findContentsWithoutEmbeddings(100)).thenReturn(List.of());

    // Act
    backfillEmbeddingsService.backfill();
    Thread.sleep(500);

    backfillEmbeddingsService.backfill();
    Thread.sleep(500);

    // Assert - should have called loadContentPort twice (once for each backfill)
    verify(loadContentPort, times(2)).findContentsWithoutEmbeddings(100);
  }

  // ==================== Error Resilience Tests ====================

  @Test
  void backfill_shouldContinueProcessing_whenEmbeddingGenerationFails() throws InterruptedException {
    // Arrange
    Content content1 = createTestContent("id-1", "link-1", "text1");
    Content content2 = createTestContent("id-2", "link-2", "text2");

    List<Float> embedding = List.of(0.1f);

    when(loadContentPort.findContentsWithoutEmbeddings(100)).thenReturn(List.of(content1, content2)).thenReturn(List.of());
    when(embeddingGenerator.generateEmbedding("text1")).thenThrow(new RuntimeException("Ollama error"));
    when(embeddingGenerator.generateEmbedding("text2")).thenReturn(embedding);

    // Act
    backfillEmbeddingsService.backfill();
    Thread.sleep(500);

    // Assert - should have tried to embed both contents
    verify(embeddingGenerator, times(2)).generateEmbedding(anyString());
    // Should have saved the successful one
    verify(saveContentPort, times(1)).updateContent(any());
  }

  @Test
  void backfill_shouldResetFlag_afterCompletion() throws InterruptedException {
    // Arrange
    when(loadContentPort.findContentsWithoutEmbeddings(100)).thenReturn(List.of());

    // Act
    backfillEmbeddingsService.backfill();
    Thread.sleep(500);

    // Flag should be reset, so this should succeed
    backfillEmbeddingsService.backfill();
    Thread.sleep(500);

    // Assert
    verify(loadContentPort, times(2)).findContentsWithoutEmbeddings(100);
  }

  // ==================== Content Update Tests ====================

  @Test
  void backfill_shouldPreserveAllContentFields_whenUpdatingWithEmbedding() throws InterruptedException {
    // Arrange
    LocalDateTime downloadTime = LocalDateTime.now();
    LocalDateTime publishTime = LocalDateTime.now();

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

    List<Float> embedding = List.of(0.1f, 0.2f, 0.3f);

    when(loadContentPort.findContentsWithoutEmbeddings(100)).thenReturn(List.of(original)).thenReturn(List.of());
    when(embeddingGenerator.generateEmbedding("text content")).thenReturn(embedding);

    ArgumentCaptor<Content> contentCaptor = ArgumentCaptor.forClass(Content.class);

    // Act
    backfillEmbeddingsService.backfill();
    Thread.sleep(500);

    // Assert
    verify(saveContentPort).updateContent(contentCaptor.capture());
    Content updated = contentCaptor.getValue();

    assertThat(updated.id()).isEqualTo("id-1");
    assertThat(updated.linkId()).isEqualTo("link-1");
    assertThat(updated.htmlContent()).isEqualTo("<html>content</html>");
    assertThat(updated.textContent()).isEqualTo("text content");
    assertThat(updated.contentLength()).isEqualTo(123);
    assertThat(updated.mimeType()).isEqualTo("text/html");
    assertThat(updated.status()).isEqualTo(DownloadStatus.COMPLETED);
    assertThat(updated.summary()).isEqualTo("summary here");
    assertThat(updated.heroImageUrl()).isEqualTo("hero.jpg");
    assertThat(updated.extractedTitle()).isEqualTo("Extracted Title");
    assertThat(updated.extractedDescription()).isEqualTo("Extracted Description");
    assertThat(updated.author()).isEqualTo("John Doe");
    assertThat(updated.embedding()).isEqualTo(embedding);
  }

  // ==================== Edge Cases ====================

  @Test
  void backfill_shouldHandleEmptyBatch() throws InterruptedException {
    // Arrange
    when(loadContentPort.findContentsWithoutEmbeddings(100)).thenReturn(List.of());

    // Act
    backfillEmbeddingsService.backfill();
    Thread.sleep(500);

    // Assert
    verify(embeddingGenerator, never()).generateEmbedding(any());
    verify(saveContentPort, never()).updateContent(any());
  }

  @Test
  void backfill_shouldHandleLargeEmbeddingVectors() throws InterruptedException {
    // Arrange
    Content content = createTestContent("id-1", "link-1", "text");

    // Create a large embedding vector (1024 dimensions)
    List<Float> largeEmbedding = new java.util.ArrayList<>();
    for (int i = 0; i < 1024; i++) {
      largeEmbedding.add((float) i / 1024.0f);
    }

    when(loadContentPort.findContentsWithoutEmbeddings(100)).thenReturn(List.of(content)).thenReturn(List.of());
    when(embeddingGenerator.generateEmbedding("text")).thenReturn(largeEmbedding);

    // Act
    backfillEmbeddingsService.backfill();
    Thread.sleep(500);

    // Assert
    ArgumentCaptor<Content> contentCaptor = ArgumentCaptor.forClass(Content.class);
    verify(saveContentPort).updateContent(contentCaptor.capture());
    assertThat(contentCaptor.getValue().embedding()).hasSize(1024);
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
