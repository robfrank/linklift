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
import it.robfrank.linklift.application.port.out.DomainEventPublisher;
import it.robfrank.linklift.application.port.out.SaveContentPort;
import java.time.LocalDateTime;
import java.util.UUID;
import org.jspecify.annotations.NonNull;

public class DownloadContentService implements DownloadContentUseCase {

    private static final int MAX_CONTENT_SIZE = 10 * 1024 * 1024; // 10MB

    private final ContentDownloaderPort contentDownloader;
    private final SaveContentPort saveContentPort;
    private final DomainEventPublisher eventPublisher;

    public DownloadContentService(
        @NonNull ContentDownloaderPort contentDownloader,
        @NonNull SaveContentPort saveContentPort,
        @NonNull DomainEventPublisher eventPublisher
    ) {
        this.contentDownloader = contentDownloader;
        this.saveContentPort = saveContentPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void downloadContentAsync(@NonNull DownloadContentCommand command) {
        // Publish download started event
        eventPublisher.publish(new ContentDownloadStartedEvent(command.linkId(), command.url()));

        System.out.println("Starting async content download for link: " + command.linkId() + ", url: " + command.url());

        // Start async download
        contentDownloader
            .downloadContent(command.url())
            .thenAccept(downloadedContent -> {
                try {
                    // Validate content size
                    if (downloadedContent.contentLength() > MAX_CONTENT_SIZE) {
                        throw new ContentDownloadException("Content size exceeds maximum limit of " + MAX_CONTENT_SIZE + " bytes");
                    }

                    // Create Content entity
                    String contentId = UUID.randomUUID().toString();
                    Content content = new Content(
                        contentId,
                        command.linkId(),
                        downloadedContent.htmlContent(),
                        downloadedContent.textContent(),
                        downloadedContent.contentLength(),
                        LocalDateTime.now(),
                        downloadedContent.mimeType(),
                        DownloadStatus.COMPLETED
                    );

                    // Save content
                    Content savedContent = saveContentPort.saveContent(content);

                    // Create HasContent edge
                    saveContentPort.createHasContentEdge(command.linkId(), savedContent.id());

                    // Publish success event
                    eventPublisher.publish(new ContentDownloadCompletedEvent(savedContent));

                    System.out.println("Content download completed for link: " + command.linkId());
                } catch (Exception e) {
                    handleDownloadFailure(command, e);
                }
            })
            .exceptionally(throwable -> {
                handleDownloadFailure(command, throwable);
                return null;
            });
    }

    private void handleDownloadFailure(@NonNull DownloadContentCommand command, @NonNull Throwable throwable) {
        String errorMessage = throwable.getMessage() != null ? throwable.getMessage() : "Unknown error";
        System.err.println("Content download failed for link: " + command.linkId() + ", error: " + errorMessage);

        // Publish failure event
        eventPublisher.publish(new ContentDownloadFailedEvent(command.linkId(), command.url(), errorMessage));

        // Save failed content record
        try {
            String contentId = UUID.randomUUID().toString();
            Content failedContent = new Content(contentId, command.linkId(), null, null, null, LocalDateTime.now(), null, DownloadStatus.FAILED);
            saveContentPort.saveContent(failedContent);
        } catch (Exception e) {
            System.err.println("Failed to save error content record: " + e.getMessage());
        }
    }
}
