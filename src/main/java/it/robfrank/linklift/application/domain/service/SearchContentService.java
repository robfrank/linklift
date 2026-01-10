package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.model.Content;
import it.robfrank.linklift.application.domain.validation.ValidationUtils;
import it.robfrank.linklift.application.port.in.SearchContentUseCase;
import it.robfrank.linklift.application.port.out.EmbeddingGenerator;
import it.robfrank.linklift.application.port.out.LoadContentPort;
import java.util.List;
import org.jspecify.annotations.NonNull;

public class SearchContentService implements SearchContentUseCase {

  private final LoadContentPort loadContentPort;
  private final EmbeddingGenerator embeddingGenerator;

  public SearchContentService(@NonNull LoadContentPort loadContentPort, @NonNull EmbeddingGenerator embeddingGenerator) {
    this.loadContentPort = loadContentPort;
    this.embeddingGenerator = embeddingGenerator;
  }

  @Override
  public @NonNull List<Content> search(@NonNull String query, int limit) {
    ValidationUtils.requireNotEmpty(query, "query");

    if (limit <= 0) {
      return List.of();
    }

    // Cap limit to a reasonable maximum for vector search if needed,
    // but for now let's just avoid negative/zero and very large values that might
    // crash the index
    int effectiveLimit = Math.min(limit, 1000);

    List<Float> queryVector = embeddingGenerator.generateEmbedding(query);
    return loadContentPort.findSimilar(queryVector, effectiveLimit);
  }
}
