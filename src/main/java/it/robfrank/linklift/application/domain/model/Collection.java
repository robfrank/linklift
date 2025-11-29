package it.robfrank.linklift.application.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record Collection(
  @JsonProperty("id") @NonNull String id,
  @JsonProperty("name") @NonNull String name,
  @JsonProperty("description") @Nullable String description,
  @JsonProperty("userId") @NonNull String userId,
  @JsonProperty("query") @Nullable String query // For smart collections
) {
  public boolean isSmart() {
    return query != null && !query.isBlank();
  }
}
