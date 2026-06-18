package it.robfrank.linklift.application.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record Note(
  @JsonProperty("id") @NonNull String id,
  @JsonProperty("linkId") @NonNull String linkId,
  // Internal identifier; not exposed in API responses.
  @JsonIgnore @NonNull String userId,
  @JsonProperty("content") @NonNull String content,
  @JsonProperty("createdAt") @NonNull LocalDateTime createdAt,
  @JsonProperty("updatedAt") @Nullable LocalDateTime updatedAt
) {
  public Note {
    createdAt = createdAt == null ? LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS) : createdAt;
  }
}
