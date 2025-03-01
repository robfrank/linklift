package it.robfrank.linklift.application.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

public record Link(
    @JsonProperty("id") String id,
    @JsonProperty("url") String url,
    @JsonProperty("title") String title,
    @JsonProperty("description") String description,
    @JsonProperty("extractedAt") LocalDateTime extractedAt,
    @JsonProperty("contentType") String contentType
) {
  public Link {
    extractedAt = extractedAt == null ? LocalDateTime.now() : extractedAt;
  }
}
