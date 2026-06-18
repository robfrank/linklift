package it.robfrank.linklift.application.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record AnswerSource(
  @JsonProperty("linkId") @NonNull String linkId,
  @JsonProperty("title") @NonNull String title,
  @JsonProperty("url") @NonNull String url,
  @JsonProperty("excerpt") @Nullable String excerpt
) {}
