package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.event.ContentDownloadCompletedEvent;
import it.robfrank.linklift.application.domain.event.ContentDownloadFailedEvent;
import it.robfrank.linklift.application.domain.event.ContentDownloadStartedEvent;
import it.robfrank.linklift.application.domain.exception.ContentDownloadException;
import it.robfrank.linklift.application.domain.model.Content;
import it.robfrank.linklift.application.domain.model.DownloadStatus;
import it.robfrank.linklift.application.port.in.DownloadContentCommand;
import it.robfrank.linklift.application.port.in.DownloadContentUseCase;
import it.robfrank.linklift.application.port.out.ContentDownloaderPort;
import it.robfrank.linklift.application.port.out.ContentExtractorPort;
import it.robfrank.linklift.application.port.out.ContentSummarizerPort;
import it.robfrank.linklift.application.port.out.DomainEventPublisher;
import it.robfrank.linklift.application.port.out.SaveContentPort;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadContentService implements DownloadContentUseCase {

  private static final Logger logger = LoggerFactory.getLogger(DownloadContentService.class);

  private static final int MAX_CONTENT_SIZE = 10 * 1024 * 1024; // 10MB
  private static final int MAX_SUMMARY_LENGTH = 500;

  private final ContentDownloaderPort contentDownloader;
  private final SaveContentPort saveContentPort;
  private final DomainEventPublisher eventPublisher;
  private final ContentExtractorPort contentExtractor;
  private final ContentSummarizerPort contentSummarizer;

  public DownloadContentService(
    @NonNull ContentDownloaderPort contentDownloader,
    @NonNull SaveContentPort saveContentPort,
    @NonNull DomainEventPublisher eventPublisher,
    @NonNull ContentExtractorPort contentExtractor,
    @NonNull ContentSummarizerPort contentSummarizer
  ) {
    this.contentDownloader = contentDownloader;
    this.saveContentPort = saveContentPort;
    this.eventPublisher = eventPublisher;
    this.contentExtractor = contentExtractor;
    this.contentSummarizer = contentSummarizer;
  }

  @Override
  public void downloadContentAsync(@NonNull DownloadContentCommand command) {
    // Publish download started event
    eventPublisher.publish(new ContentDownloadStartedEvent(command.linkId(), command.url()));

    logger.info("Starting async content download for link: {}, url: {}", command.linkId(), command.url());

    // Start async download
    contentDownloader
      .downloadContent(command.url())
      .thenAccept(downloadedContent -> {
        try {
          // Validate content size
          if (downloadedContent.contentLength() > MAX_CONTENT_SIZE) {
            throw new ContentDownloadException("Content size exceeds maximum limit of " + MAX_CONTENT_SIZE + " bytes");
          }

          // Extract metadata
          String html = downloadedContent.htmlContent();
          ContentExtractorPort.ExtractedMetadata metadata = null;
          String summary = null;

          if (html != null && !html.isBlank()) {
            try {
              metadata = contentExtractor.extractMetadata(html, command.url());

              // Generate summary if text content is available
              if (metadata.textContent() != null && !metadata.textContent().isBlank()) {
                summary = contentSummarizer.generateSummary(metadata.textContent(), MAX_SUMMARY_LENGTH);
              } else if (downloadedContent.textContent() != null) {
                summary = contentSummarizer.generateSummary(downloadedContent.textContent(), MAX_SUMMARY_LENGTH);
              }
            } catch (Exception e) {
              logger.warn("Failed to extract metadata or generate summary for link: {}", command.linkId(), e);
            }
          }

          // Create Content entity
          String contentId = UUID.randomUUID().toString();

          String extractedTitle = metadata != null ? metadata.title() : null;
          String extractedDescription = metadata != null ? metadata.description() : null;
          String author = metadata != null ? metadata.author() : null;
          String heroImageUrl = metadata != null ? metadata.heroImageUrl() : null;
          LocalDateTime publishedDate = parseDate(metadata != null ? metadata.publishedDate() : null);

          Content content = new Content(
            contentId,
            command.linkId(),
            downloadedContent.htmlContent(),
            downloadedContent.textContent(),
            downloadedContent.contentLength(),
            LocalDateTime.now(),
            downloadedContent.mimeType(),
            DownloadStatus.COMPLETED,
            summary,
            heroImageUrl,
            extractedTitle,
            extractedDescription,
            author,
            publishedDate
          );

          // Save content
          Content savedContent = saveContentPort.saveContent(content);

          // Create HasContent edge
          saveContentPort.createHasContentEdge(command.linkId(), savedContent.id());

          // Publish success event
          eventPublisher.publish(new ContentDownloadCompletedEvent(savedContent));

          logger.info("Content download completed for link: {}", command.linkId());
        } catch (Exception e) {
          handleDownloadFailure(command, e);
        }
      })
      .exceptionally(throwable -> {
        handleDownloadFailure(command, throwable);
        return null;
      });
  }

  private LocalDateTime parseDate(String dateStr) {
    if (dateStr == null || dateStr.isBlank()) return null;
    try {
      // Try ISO_DATE_TIME first
      return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
    } catch (Exception e) {
      try {
        // Try ISO_LOCAL_DATE
        return java.time.LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
      } catch (Exception e2) {
        // Ignore parsing errors
        return null;
      }
    }
  }

  private void handleDownloadFailure(@NonNull DownloadContentCommand command, @NonNull Throwable throwable) {
    String errorMessage = throwable.getMessage() != null ? throwable.getMessage() : "Unknown error";
    logger.error("Content download failed for link: {}, url: {}", command.linkId(), command.url(), throwable);

    // Publish failure event
    eventPublisher.publish(new ContentDownloadFailedEvent(command.linkId(), command.url(), errorMessage));

    // Save failed content record
    try {
      String contentId = UUID.randomUUID().toString();
      Content failedContent = new Content(contentId, command.linkId(), null, null, null, LocalDateTime.now(), null, DownloadStatus.FAILED);
      saveContentPort.saveContent(failedContent);
    } catch (Exception e) {
      logger.error("Failed to save error content record for link: {}", command.linkId(), e);
    }
  }
}
