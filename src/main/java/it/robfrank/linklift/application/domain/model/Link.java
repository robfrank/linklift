package it.robfrank.linklift.application.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record Link(
  @JsonProperty("id") @NonNull String id,
  @JsonProperty("url") @NonNull String url,
  @JsonProperty("title") @Nullable String title,
  @JsonProperty("description") @Nullable String description,
  @JsonProperty("extractedAt") @NonNull LocalDateTime extractedAt,
  @JsonProperty("contentType") @Nullable String contentType
) {
  public Link {
    extractedAt = extractedAt == null ? LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS) : extractedAt;
  }
}
