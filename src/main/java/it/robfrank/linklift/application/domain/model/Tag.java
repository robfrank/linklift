package it.robfrank.linklift.application.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.jspecify.annotations.NonNull;

public record Tag(
  @JsonProperty("id") @NonNull String id,
  @JsonProperty("name") @NonNull String name,
  // Internal identifier; not exposed in API responses.
  @JsonIgnore @NonNull String userId,
  @JsonProperty("createdAt") @NonNull LocalDateTime createdAt
) {
  public Tag {
    createdAt = createdAt == null ? LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS) : createdAt;
    name = name == null ? name : name.toLowerCase().strip();
  }
}
