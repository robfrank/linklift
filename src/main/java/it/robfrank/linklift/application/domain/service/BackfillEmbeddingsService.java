package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.model.Content;
import it.robfrank.linklift.application.port.in.BackfillEmbeddingsUseCase;
import it.robfrank.linklift.application.port.out.EmbeddingGenerator;
import it.robfrank.linklift.application.port.out.LoadContentPort;
import it.robfrank.linklift.application.port.out.SaveContentPort;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackfillEmbeddingsService implements BackfillEmbeddingsUseCase {

  private static final Logger logger = LoggerFactory.getLogger(BackfillEmbeddingsService.class);
  private static final int BATCH_SIZE = 100;

  private final LoadContentPort loadContentPort;
  private final SaveContentPort saveContentPort;
  private final EmbeddingGenerator embeddingGenerator;
  private final ExecutorService executorService;
  private final AtomicBoolean isProcessing = new AtomicBoolean(false);

  public BackfillEmbeddingsService(
    LoadContentPort loadContentPort,
    SaveContentPort saveContentPort,
    EmbeddingGenerator embeddingGenerator,
    ExecutorService executorService
  ) {
    this.loadContentPort = loadContentPort;
    this.saveContentPort = saveContentPort;
    this.embeddingGenerator = embeddingGenerator;
    this.executorService = executorService;
  }

  @Override
  public void backfill() {
    if (isProcessing.compareAndSet(false, true)) {
      executorService.submit(this::runBackfill);
    } else {
      logger.warn("Backfill process already in progress.");
    }
  }

  private void runBackfill() {
    try {
      logger.info("Starting embedding backfill process...");
      int totalSuccess = 0;
      int totalFail = 0;

      while (true) {
        List<Content> contents = loadContentPort.findContentsWithoutEmbeddings(BATCH_SIZE);
        if (contents.isEmpty()) {
          break;
        }

        logger.info("Processing batch of {} contents", contents.size());
        for (Content content : contents) {
          try {
            String text = content.textContent();
            if (text != null && !text.isBlank()) {
              List<Float> embedding = embeddingGenerator.generateEmbedding(text);
              Content updatedContent = new Content(
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
                embedding
              );
              saveContentPort.updateContent(updatedContent);
              totalSuccess++;
            }
          } catch (Exception e) {
            logger.error("Failed to generate embedding for content id: {}", content.id(), e);
            totalFail++;
          }
        }
      }
      logger.info("Backfill process finished. Total success: {}, Total failed: {}", totalSuccess, totalFail);
    } finally {
      isProcessing.set(false);
    }
  }
}
