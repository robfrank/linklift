package it.robfrank.linklift.application.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record Content(
  @JsonProperty("id") @NonNull String id,
  @JsonProperty("linkId") @NonNull String linkId,
  @JsonProperty("htmlContent") @Nullable String htmlContent,
  @JsonProperty("textContent") @Nullable String textContent,
  @JsonProperty("contentLength") @Nullable Integer contentLength,
  @JsonProperty("downloadedAt") @NonNull LocalDateTime downloadedAt,
  @JsonProperty("mimeType") @Nullable String mimeType,
  @JsonProperty("status") @NonNull DownloadStatus status,
  // Phase 1 Feature 1: Automated Content & Metadata Extraction
  @JsonProperty("summary") @Nullable String summary,
  @JsonProperty("heroImageUrl") @Nullable String heroImageUrl,
  @JsonProperty("extractedTitle") @Nullable String extractedTitle,
  @JsonProperty("extractedDescription") @Nullable String extractedDescription,
  @JsonProperty("author") @Nullable String author,
  @JsonProperty("publishedDate") @Nullable LocalDateTime publishedDate,
  @JsonProperty("embedding") @Nullable float[] embedding
) {
  public Content {
    downloadedAt = downloadedAt.truncatedTo(ChronoUnit.SECONDS);
    publishedDate = publishedDate != null ? publishedDate.truncatedTo(ChronoUnit.SECONDS) : null;
  }

  /**
   * Creates a Content instance with only the basic fields (for backward
   * compatibility).
   */
  public Content(
    @NonNull String id,
    @NonNull String linkId,
    @Nullable String htmlContent,
    @Nullable String textContent,
    @Nullable Integer contentLength,
    @NonNull LocalDateTime downloadedAt,
    @Nullable String mimeType,
    @NonNull DownloadStatus status
  ) {
    this(id, linkId, htmlContent, textContent, contentLength, downloadedAt, mimeType, status, null, null, null, null, null, null, new float[] {});
  }
}
