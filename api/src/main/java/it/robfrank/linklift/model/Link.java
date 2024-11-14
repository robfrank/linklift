package it.robfrank.linklift.model;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record Link(
    @JsonProperty("url") String url,
    @JsonProperty("title") String title,
    @JsonProperty("description") String description,
    @JsonProperty("extractedAt") Long extractedAt,
    @JsonProperty("contentType") String contentType
) {
  public Link {
    extractedAt = extractedAt == null ? Instant.now().toEpochMilli() : extractedAt;
  }
}
