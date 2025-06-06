package it.robfrank.linklift.application.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record Link(
  @JsonProperty("id") String id,
  @JsonProperty("url") String url,
  @JsonProperty("title") String title,
  @JsonProperty("description") String description,
  @JsonProperty("extractedAt") LocalDateTime extractedAt,
  @JsonProperty("contentType") String contentType
) {
  public Link {
    extractedAt = extractedAt == null ? LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS) : extractedAt;
  }
}
