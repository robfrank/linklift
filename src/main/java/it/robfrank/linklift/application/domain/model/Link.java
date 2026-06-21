package it.robfrank.linklift.application.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record Link(
  @JsonProperty("id") @NonNull String id,
  @JsonProperty("url") @NonNull String url,
  @JsonProperty("title") @Nullable String title,
  @JsonProperty("description") @Nullable String description,
  @JsonProperty("extractedAt") @NonNull LocalDateTime extractedAt,
  @JsonProperty("contentType") @Nullable String contentType,
  @JsonProperty("extractedUrls") @NonNull List<String> extractedUrls,
  @JsonProperty("readStatus") @NonNull ReadStatus readStatus,
  @JsonProperty("archived") boolean archived,
  @JsonProperty("favorited") boolean favorited
) {
  public Link {
    extractedAt = extractedAt == null ? LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS) : extractedAt;
    extractedUrls = extractedUrls == null ? List.of() : List.copyOf(extractedUrls);
    readStatus = readStatus == null ? ReadStatus.UNREAD : readStatus;
  }
}
