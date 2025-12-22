package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.event.ContentDownloadCompletedEvent;
import it.robfrank.linklift.application.domain.event.ContentDownloadFailedEvent;
import it.robfrank.linklift.application.domain.event.ContentDownloadStartedEvent;
import it.robfrank.linklift.application.domain.exception.ContentDownloadException;
import it.robfrank.linklift.application.domain.model.Content;
import it.robfrank.linklift.application.domain.model.DownloadStatus;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.domain.validation.ValidationUtils;
import it.robfrank.linklift.application.port.in.DownloadContentCommand;
import it.robfrank.linklift.application.port.in.DownloadContentUseCase;
import it.robfrank.linklift.application.port.out.ContentDownloaderPort;
import it.robfrank.linklift.application.port.out.ContentExtractorPort;
import it.robfrank.linklift.application.port.out.ContentSummarizerPort;
import it.robfrank.linklift.application.port.out.DomainEventPublisher;
import it.robfrank.linklift.application.port.out.EmbeddingGenerator;
import it.robfrank.linklift.application.port.out.LoadLinksPort;
import it.robfrank.linklift.application.port.out.SaveContentPort;
import it.robfrank.linklift.application.port.out.SaveLinkPort;
import it.robfrank.linklift.application.port.out.UpdateLinkPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
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
  private final LoadLinksPort loadLinksPort;
  private final SaveLinkPort saveLinkPort;
  private final UpdateLinkPort updateLinkPort;
  private final EmbeddingGenerator embeddingGenerator;
  private final ExecutorService executorService;

  public DownloadContentService(
    @NonNull ContentDownloaderPort contentDownloader,
    @NonNull SaveContentPort saveContentPort,
    @NonNull DomainEventPublisher eventPublisher,
    @NonNull ContentExtractorPort contentExtractor,
    @NonNull ContentSummarizerPort contentSummarizer,
    @NonNull LoadLinksPort loadLinksPort,
    @NonNull SaveLinkPort saveLinkPort,
    @NonNull UpdateLinkPort updateLinkPort,
    @NonNull EmbeddingGenerator embeddingGenerator,
    @NonNull ExecutorService executorService
  ) {
    this.contentDownloader = contentDownloader;
    this.saveContentPort = saveContentPort;
    this.eventPublisher = eventPublisher;
    this.contentExtractor = contentExtractor;
    this.contentSummarizer = contentSummarizer;
    this.loadLinksPort = loadLinksPort;
    this.saveLinkPort = saveLinkPort;
    this.updateLinkPort = updateLinkPort;
    this.embeddingGenerator = embeddingGenerator;
    this.executorService = executorService;
  }

  @Override
  public void refreshContent(@NonNull String linkId) {
    ValidationUtils.requireNotEmpty(linkId, "linkId");
    Link link = loadLinksPort.getLinkById(linkId);
    if (link != null) {
      downloadContentAsync(new DownloadContentCommand(linkId, link.url()));
    }
  }

  @Override
  public void downloadContentAsync(@NonNull DownloadContentCommand command) {
    ValidationUtils.requireNotNull(command, "command");
    ValidationUtils.requireNotEmpty(command.linkId(), "linkId");
    ValidationUtils.requireNotEmpty(command.url(), "url");

    // Publish download started event
    String id = command.linkId();
    String url = command.url();

    eventPublisher.publish(new ContentDownloadStartedEvent(id, url));

    logger.info("Starting async content download for link: {}, url: {}", id, url);

    // Start async download with retries
    downloadWithRetry(url, 3)
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
              metadata = contentExtractor.extractMetadata(html, url);

              // Generate summary if text content is available
              if (metadata != null) {
                String textContent = metadata.textContent();
                if (textContent != null && !textContent.isBlank()) {
                  summary = contentSummarizer.generateSummary(textContent, MAX_SUMMARY_LENGTH);
                } else {
                  String downloadedText = downloadedContent.textContent();
                  if (downloadedText != null && !downloadedText.isBlank()) {
                    summary = contentSummarizer.generateSummary(downloadedText, MAX_SUMMARY_LENGTH);
                  }
                }
              }
            } catch (Exception e) {
              logger.error("Failed to extract metadata or generate summary for link: {}", id, e);
            }
          }

          // Create Content entity
          String contentId = UUID.randomUUID().toString();

          String extractedTitle = metadata != null ? metadata.title() : null;
          String extractedDescription = metadata != null ? metadata.description() : null;
          String author = metadata != null ? metadata.author() : null;
          String heroImageUrl = metadata != null ? metadata.heroImageUrl() : null;
          LocalDateTime publishedDate = parseDate(metadata != null ? metadata.publishedDate() : null);
          List<String> extractedUrls = metadata != null ? metadata.extractedUrls() : List.of();

          // Update link with extracted URLs if available
          if (extractedUrls != null && !extractedUrls.isEmpty()) {
            Link existingLink = loadLinksPort.getLinkById(id);
            if (existingLink != null) {
              Link updatedLink = new Link(
                existingLink.id(),
                existingLink.url(),
                existingLink.title(),
                existingLink.description(),
                existingLink.extractedAt(),
                existingLink.contentType(),
                extractedUrls
              );
              updateLinkPort.updateLink(updatedLink);
              saveLinkPort.syncLinkConnections(updatedLink);
            }
          }

          // Generate embedding
          List<Float> embedding = null;
          if (metadata != null) {
            String textForEmbedding = metadata.textContent();
            if (textForEmbedding != null && !textForEmbedding.isBlank()) {
              try {
                embedding = embeddingGenerator.generateEmbedding(textForEmbedding);
              } catch (Exception e) {
                logger.error("Failed to generate embedding for link: {}", id, e);
              }
            }
          }

          Content content = new Content(
            contentId,
            id,
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
            publishedDate,
            embedding
          );

          // Save content
          Content savedContent = saveContentPort.saveContent(content);

          // Create HasContent edge
          // Ensure savedContent is not null before accessing its id
          if (savedContent != null) {
            saveContentPort.createHasContentEdge(id, savedContent.id());
            // Publish success event
            eventPublisher.publish(new ContentDownloadCompletedEvent(savedContent));
          } else {
            logger.warn("Saved content was null for link: {}. Cannot create edge or publish completion event.", id);
          }

          logger.info("Content download completed for link: {}", id);
        } catch (Exception e) {
          handleDownloadFailure(command, e);
        }
      })
      .exceptionally(throwable -> {
        handleDownloadFailure(command, throwable);
        return null;
      });
  }

  private CompletableFuture<ContentDownloaderPort.DownloadedContent> downloadWithRetry(String url, int retries) {
    return contentDownloader
      .downloadContent(url)
      .handle((res, ex) -> {
        if (ex == null) {
          return CompletableFuture.completedFuture(res);
        } else {
          if (retries > 0) {
            logger.warn("Download failed for url: {}. Retrying... ({} attempts remaining)", url, retries);
            return CompletableFuture.runAsync(() -> {}, CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS)).thenCompose(v ->
              downloadWithRetry(url, retries - 1)
            );
          } else {
            return CompletableFuture.<ContentDownloaderPort.DownloadedContent>failedFuture(ex);
          }
        }
      })
      .thenCompose(Function.identity());
  }

  private LocalDateTime parseDate(String dateStr) {
    if (dateStr == null || dateStr.isBlank()) return null;
    try {
      // Try ISO_DATE_TIME first
      return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
    } catch (Exception e) {
      try {
        // Try ISO_LOCAL_DATE
        return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
      } catch (Exception e2) {
        // Ignore parsing errors
        logger.warn("Failed to parse date string '{}'.", dateStr);
        return null;
      }
    }
  }

  private void handleDownloadFailure(@NonNull DownloadContentCommand command, @NonNull Throwable throwable) {
    String errorMessage = throwable.getMessage() != null ? throwable.getMessage() : "Unknown error";
    String id = command.linkId();
    String url = command.url();
    logger.error("Content download failed for link: {}, url: {}", id, url, throwable);

    // Publish failure event
    eventPublisher.publish(new ContentDownloadFailedEvent(id, url, errorMessage));

    // Save failed content record
    try {
      String contentId = UUID.randomUUID().toString();
      Content failedContent = new Content(contentId, id, null, null, null, LocalDateTime.now(), null, DownloadStatus.FAILED);
      saveContentPort.saveContent(failedContent);
    } catch (Exception e) {
      logger.error("Failed to save error content record for link: {}", id, e);
    }
  }
}
