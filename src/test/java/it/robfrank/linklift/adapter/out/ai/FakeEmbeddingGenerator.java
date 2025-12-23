package it.robfrank.linklift.adapter.out.ai;

import it.robfrank.linklift.application.port.out.EmbeddingGenerator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Fake implementation of EmbeddingGenerator for testing.
 * Provides deterministic embeddings based on text hash, ensuring same text always produces same embedding.
 * This eliminates the need for mocks and provides consistent test data.
 */
public class FakeEmbeddingGenerator implements EmbeddingGenerator {

  private final Map<String, List<Float>> embeddingCache = new HashMap<>();
  private final Function<String, List<Float>> embeddingFunction;
  private @Nullable RuntimeException exceptionToThrow = null;

  /**
   * Creates FakeEmbeddingGenerator with default deterministic embedding function.
   * Generates 384-dimensional embeddings based on text hash.
   */
  public FakeEmbeddingGenerator() {
    this(text -> {
      // Generate deterministic embedding based on text hash
      int hash = text.hashCode();
      List<Float> embedding = new ArrayList<>(384);
      for (int i = 0; i < 384; i++) {
        // Use hash and index to generate values between -1 and 1
        // This ensures embeddings are deterministic and normalized
        float value = (float) Math.sin(hash + i) * 0.5f;
        embedding.add(value);
      }
      return embedding;
    });
  }

  /**
   * Creates FakeEmbeddingGenerator with custom embedding function.
   *
   * @param embeddingFunction function that generates embeddings from text
   */
  public FakeEmbeddingGenerator(@NonNull Function<String, List<Float>> embeddingFunction) {
    this.embeddingFunction = embeddingFunction;
  }

  @Override
  public @NonNull List<Float> generateEmbedding(@NonNull String text) {
    if (exceptionToThrow != null) {
      RuntimeException ex = exceptionToThrow;
      exceptionToThrow = null; // Only throw once
      throw ex;
    }

    return embeddingCache.computeIfAbsent(text, embeddingFunction);
  }

  /**
   * Configures the generator to throw an exception on the next call.
   * Useful for testing error handling.
   *
   * @param exception the exception to throw
   */
  public void throwOnNextCall(@NonNull RuntimeException exception) {
    this.exceptionToThrow = exception;
  }

  /**
   * Clears the embedding cache.
   * Useful for testing cache behavior.
   */
  public void clearCache() {
    embeddingCache.clear();
  }

  /**
   * Gets the current cache size.
   * Useful for verifying caching behavior.
   *
   * @return the number of cached embeddings
   */
  public int getCacheSize() {
    return embeddingCache.size();
  }
}
