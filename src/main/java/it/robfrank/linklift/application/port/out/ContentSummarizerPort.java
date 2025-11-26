package it.robfrank.linklift.application.port.out;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Port for generating summaries from text content.
 * Part of Phase 1 Feature 1: Automated Content & Metadata Extraction.
 */
public interface ContentSummarizerPort {
  /**
   * Generates a summary from the given text content.
   *
   * @param textContent the full text content to summarize
   * @param maxLength maximum length of the summary in characters
   * @return a summary of the content, or null if summarization fails
   */
  @Nullable
  String generateSummary(@NonNull String textContent, int maxLength);
}
