package it.robfrank.linklift.application.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    @JsonProperty("status") @NonNull DownloadStatus status
) {
    public Content {
        downloadedAt = downloadedAt.truncatedTo(ChronoUnit.SECONDS);
    }
}
