package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.model.Content;
import it.robfrank.linklift.application.port.in.BackfillEmbeddingsUseCase;
import it.robfrank.linklift.application.port.out.EmbeddingGenerator;
import it.robfrank.linklift.application.port.out.LoadContentPort;
import it.robfrank.linklift.application.port.out.SaveContentPort;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackfillEmbeddingsService implements BackfillEmbeddingsUseCase {

  private static final Logger logger = LoggerFactory.getLogger(BackfillEmbeddingsService.class);

  private final LoadContentPort loadContentPort;
  private final SaveContentPort saveContentPort;
  private final EmbeddingGenerator embeddingGenerator;

  public BackfillEmbeddingsService(LoadContentPort loadContentPort, SaveContentPort saveContentPort, EmbeddingGenerator embeddingGenerator) {
    this.loadContentPort = loadContentPort;
    this.saveContentPort = saveContentPort;
    this.embeddingGenerator = embeddingGenerator;
  }

  @Override
  public void backfill() {
    logger.info("Starting embedding backfill process...");
    List<Content> contents = loadContentPort.findContentsWithoutEmbeddings(1000);
    logger.info("Found {} contents without embeddings", contents.size());

    int successCount = 0;
    int failCount = 0;

    for (Content content : contents) {
      String text = content.textContent();
      if (text != null && !text.isBlank()) {
        try {
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
          saveContentPort.saveContent(updatedContent);
          successCount++;
        } catch (Exception e) {
          logger.error("Failed to generate embedding for content id: {}", content.id(), e);
          failCount++;
        }
      }
    }

    logger.info("Backfill process finished. Success: {}, Failed: {}", successCount, failCount);
  }
}
