package it.robfrank.linklift.application.port.out;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Port for extracting metadata and content from web pages.
 * Part of Phase 1 Feature 1: Automated Content & Metadata Extraction.
 */
public interface ContentExtractorPort {
  /**
   * Extracts metadata and content from HTML.
   *
   * @param html the HTML content to extract from
   * @param url the original URL (for resolving relative URLs)
   * @return extracted metadata
   */
  ExtractedMetadata extractMetadata(@NonNull String html, @NonNull String url);

  /**
   * Represents extracted metadata from a web page.
   */
  record ExtractedMetadata(
    @Nullable String title,
    @Nullable String description,
    @Nullable String author,
    @Nullable String publishedDate,
    @Nullable String heroImageUrl,
    @Nullable String mainContent,
    @Nullable String textContent
  ) {}
}
