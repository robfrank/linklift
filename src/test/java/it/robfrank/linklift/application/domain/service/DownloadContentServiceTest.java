package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.event.ContentDownloadCompletedEvent;
import it.robfrank.linklift.application.domain.event.ContentDownloadFailedEvent;
import it.robfrank.linklift.application.domain.event.ContentDownloadStartedEvent;
import it.robfrank.linklift.application.domain.exception.ContentDownloadException;
import it.robfrank.linklift.application.domain.exception.ValidationException;
import it.robfrank.linklift.application.domain.model.Content;
import it.robfrank.linklift.application.domain.model.DownloadStatus;
import it.robfrank.linklift.application.port.in.DownloadContentCommand;
import it.robfrank.linklift.application.port.out.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DownloadContentServiceTest {

  @Mock
  private ContentDownloaderPort contentDownloader;

  @Mock
  private SaveContentPort saveContentPort;

  @Mock
  private DomainEventPublisher eventPublisher;

  @Mock
  private ContentExtractorPort contentExtractorPort; // New Mock

  @Mock
  private ContentSummarizerPort contentSummarizerPort; // New Mock

  @Mock
  private LoadLinksPort loadLinksPort;

  @Mock
  private EmbeddingGenerator embeddingGenerator;

  @Mock
  private ExecutorService executorService;

  private DownloadContentService downloadContentService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    downloadContentService = new DownloadContentService(
      contentDownloader,
      saveContentPort,
      eventPublisher,
      contentExtractorPort,
      contentSummarizerPort,
      loadLinksPort,
      embeddingGenerator,
      executorService
    );
  }

  @Test
  void downloadContentAsync_shouldPublishStartedEvent() {
    // Arrange
    DownloadContentCommand command = new DownloadContentCommand("link-123", "https://example.com");
    ContentDownloaderPort.DownloadedContent downloadedContent = new ContentDownloaderPort.DownloadedContent(
      "<html><body>Test</body></html>",
      "Test",
      "text/html",
      1024
    );

    when(contentDownloader.downloadContent("https://example.com")).thenReturn(CompletableFuture.completedFuture(downloadedContent));
    when(saveContentPort.saveContent(any(Content.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    downloadContentService.downloadContentAsync(command);

    // Wait a bit for async processing
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Assert
    ArgumentCaptor<ContentDownloadStartedEvent> startedEventCaptor = ArgumentCaptor.forClass(ContentDownloadStartedEvent.class);
    verify(eventPublisher, atLeastOnce()).publish(startedEventCaptor.capture());

    ContentDownloadStartedEvent startedEvent = startedEventCaptor
      .getAllValues()
      .stream()
      .filter(event -> event instanceof ContentDownloadStartedEvent)
      .map(event -> (ContentDownloadStartedEvent) event)
      .findFirst()
      .orElse(null);

    assertThat(startedEvent).isNotNull();
    assertThat(startedEvent.getLinkId()).isEqualTo("link-123");
    assertThat(startedEvent.getUrl()).isEqualTo("https://example.com");
  }

  @Test
  void downloadContentAsync_shouldSaveContentAndPublishCompletedEvent() {
    // Arrange
    DownloadContentCommand command = new DownloadContentCommand("link-123", "https://example.com");
    ContentDownloaderPort.DownloadedContent downloadedContent = new ContentDownloaderPort.DownloadedContent(
      "<html><body>Test Content</body></html>",
      "Test Content",
      "text/html",
      2048
    );

    when(contentDownloader.downloadContent("https://example.com")).thenReturn(CompletableFuture.completedFuture(downloadedContent));
    when(saveContentPort.saveContent(any(Content.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    downloadContentService.downloadContentAsync(command);

    // Wait for async processing
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Assert
    ArgumentCaptor<Content> contentCaptor = ArgumentCaptor.forClass(Content.class);
    verify(saveContentPort).saveContent(contentCaptor.capture());

    Content savedContent = contentCaptor.getValue();
    assertThat(savedContent.linkId()).isEqualTo("link-123");
    assertThat(savedContent.htmlContent()).isEqualTo("<html><body>Test Content</body></html>");
    assertThat(savedContent.textContent()).isEqualTo("Test Content");
    assertThat(savedContent.mimeType()).isEqualTo("text/html");
    assertThat(savedContent.contentLength()).isEqualTo(2048);
    assertThat(savedContent.status()).isEqualTo(DownloadStatus.COMPLETED);

    verify(saveContentPort).createHasContentEdge(eq("link-123"), eq(savedContent.id()));

    ArgumentCaptor<ContentDownloadCompletedEvent> completedEventCaptor = ArgumentCaptor.forClass(ContentDownloadCompletedEvent.class);
    verify(eventPublisher, atLeastOnce()).publish(completedEventCaptor.capture());

    ContentDownloadCompletedEvent completedEvent = completedEventCaptor
      .getAllValues()
      .stream()
      .filter(event -> event instanceof ContentDownloadCompletedEvent)
      .map(event -> (ContentDownloadCompletedEvent) event)
      .findFirst()
      .orElse(null);

    assertThat(completedEvent).isNotNull();
    assertThat(completedEvent.getContent().linkId()).isEqualTo("link-123");
  }

  @Test
  void downloadContentAsync_shouldHandleFailureAndPublishFailedEvent() {
    // Arrange
    DownloadContentCommand command = new DownloadContentCommand("link-123", "https://example.com");

    when(contentDownloader.downloadContent("https://example.com")).thenReturn(CompletableFuture.failedFuture(new ContentDownloadException("Download failed")));
    when(saveContentPort.saveContent(any(Content.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    downloadContentService.downloadContentAsync(command);

    // Wait for async processing (including retries: 3 attempts with 1s delay each)
    try {
      Thread.sleep(3500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Assert
    ArgumentCaptor<Content> contentCaptor = ArgumentCaptor.forClass(Content.class);
    verify(saveContentPort).saveContent(contentCaptor.capture());

    Content savedContent = contentCaptor.getValue();
    assertThat(savedContent.linkId()).isEqualTo("link-123");
    assertThat(savedContent.status()).isEqualTo(DownloadStatus.FAILED);

    ArgumentCaptor<ContentDownloadFailedEvent> failedEventCaptor = ArgumentCaptor.forClass(ContentDownloadFailedEvent.class);
    verify(eventPublisher, atLeastOnce()).publish(failedEventCaptor.capture());

    ContentDownloadFailedEvent failedEvent = failedEventCaptor
      .getAllValues()
      .stream()
      .filter(event -> event instanceof ContentDownloadFailedEvent)
      .map(event -> (ContentDownloadFailedEvent) event)
      .findFirst()
      .orElse(null);

    assertThat(failedEvent).isNotNull();
    assertThat(failedEvent.getLinkId()).isEqualTo("link-123");
    assertThat(failedEvent.getErrorMessage()).contains("Download failed");
  }

  @Test
  void downloadContentAsync_shouldRejectContentExceedingMaxSize() {
    // Arrange
    DownloadContentCommand command = new DownloadContentCommand("link-123", "https://example.com");
    // Create content larger than 10MB
    int largeSize = 11 * 1024 * 1024;
    ContentDownloaderPort.DownloadedContent downloadedContent = new ContentDownloaderPort.DownloadedContent(
      "x".repeat(largeSize),
      "Large content",
      "text/html",
      largeSize
    );

    when(contentDownloader.downloadContent("https://example.com")).thenReturn(CompletableFuture.completedFuture(downloadedContent));
    when(saveContentPort.saveContent(any(Content.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    downloadContentService.downloadContentAsync(command);

    // Wait for async processing
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Assert
    ArgumentCaptor<ContentDownloadFailedEvent> failedEventCaptor = ArgumentCaptor.forClass(ContentDownloadFailedEvent.class);
    verify(eventPublisher, atLeastOnce()).publish(failedEventCaptor.capture());

    ContentDownloadFailedEvent failedEvent = failedEventCaptor
      .getAllValues()
      .stream()
      .filter(event -> event instanceof ContentDownloadFailedEvent)
      .map(event -> (ContentDownloadFailedEvent) event)
      .findFirst()
      .orElse(null);

    assertThat(failedEvent).isNotNull();
    assertThat(failedEvent.getErrorMessage()).contains("exceeds maximum limit");
  }

  @Test
  void downloadContentAsync_shouldThrowValidationException_whenCommandIsNull() {
    // Act & Assert
    assertThatThrownBy(() -> downloadContentService.downloadContentAsync(null))
      .isInstanceOf(ValidationException.class)
      .hasMessageContaining("command cannot be null");

    verify(contentDownloader, never()).downloadContent(any());
    verify(eventPublisher, never()).publish(any());
  }

  @Test
  void downloadContentAsync_shouldThrowValidationException_whenLinkIdIsNull() {
    // Arrange
    DownloadContentCommand command = new DownloadContentCommand(null, "https://example.com");

    // Act & Assert
    assertThatThrownBy(() -> downloadContentService.downloadContentAsync(command))
      .isInstanceOf(ValidationException.class)
      .hasMessageContaining("linkId cannot be empty");

    verify(contentDownloader, never()).downloadContent(any());
    verify(eventPublisher, never()).publish(any());
  }

  @Test
  void downloadContentAsync_shouldThrowValidationException_whenLinkIdIsEmpty() {
    // Arrange
    DownloadContentCommand command = new DownloadContentCommand("", "https://example.com");

    // Act & Assert
    assertThatThrownBy(() -> downloadContentService.downloadContentAsync(command))
      .isInstanceOf(ValidationException.class)
      .hasMessageContaining("linkId cannot be empty");

    verify(contentDownloader, never()).downloadContent(any());
    verify(eventPublisher, never()).publish(any());
  }

  @Test
  void downloadContentAsync_shouldThrowValidationException_whenLinkIdIsBlank() {
    // Arrange
    DownloadContentCommand command = new DownloadContentCommand("   ", "https://example.com");

    // Act & Assert
    assertThatThrownBy(() -> downloadContentService.downloadContentAsync(command))
      .isInstanceOf(ValidationException.class)
      .hasMessageContaining("linkId cannot be empty");

    verify(contentDownloader, never()).downloadContent(any());
    verify(eventPublisher, never()).publish(any());
  }

  @Test
  void downloadContentAsync_shouldThrowValidationException_whenUrlIsNull() {
    // Arrange
    DownloadContentCommand command = new DownloadContentCommand("link-123", null);

    // Act & Assert
    assertThatThrownBy(() -> downloadContentService.downloadContentAsync(command))
      .isInstanceOf(ValidationException.class)
      .hasMessageContaining("url cannot be empty");

    verify(contentDownloader, never()).downloadContent(any());
    verify(eventPublisher, never()).publish(any());
  }

  @Test
  void downloadContentAsync_shouldThrowValidationException_whenUrlIsEmpty() {
    // Arrange
    DownloadContentCommand command = new DownloadContentCommand("link-123", "");

    // Act & Assert
    assertThatThrownBy(() -> downloadContentService.downloadContentAsync(command))
      .isInstanceOf(ValidationException.class)
      .hasMessageContaining("url cannot be empty");

    verify(contentDownloader, never()).downloadContent(any());
    verify(eventPublisher, never()).publish(any());
  }

  @Test
  void downloadContentAsync_shouldThrowValidationException_whenUrlIsBlank() {
    // Arrange
    DownloadContentCommand command = new DownloadContentCommand("link-123", "   ");

    // Act & Assert
    assertThatThrownBy(() -> downloadContentService.downloadContentAsync(command))
      .isInstanceOf(ValidationException.class)
      .hasMessageContaining("url cannot be empty");

    verify(contentDownloader, never()).downloadContent(any());
    verify(eventPublisher, never()).publish(any());
  }

  @Test
  void refreshContent_shouldThrowValidationException_whenLinkIdIsNull() {
    // Act & Assert
    assertThatThrownBy(() -> downloadContentService.refreshContent(null))
      .isInstanceOf(ValidationException.class)
      .hasMessageContaining("linkId cannot be empty");

    verify(loadLinksPort, never()).getLinkById(any());
  }

  @Test
  void refreshContent_shouldThrowValidationException_whenLinkIdIsEmpty() {
    // Act & Assert
    assertThatThrownBy(() -> downloadContentService.refreshContent("")).isInstanceOf(ValidationException.class).hasMessageContaining("linkId cannot be empty");

    verify(loadLinksPort, never()).getLinkById(any());
  }

  @Test
  void refreshContent_shouldThrowValidationException_whenLinkIdIsBlank() {
    // Act & Assert
    assertThatThrownBy(() -> downloadContentService.refreshContent("   "))
      .isInstanceOf(ValidationException.class)
      .hasMessageContaining("linkId cannot be empty");

    verify(loadLinksPort, never()).getLinkById(any());
  }
}
