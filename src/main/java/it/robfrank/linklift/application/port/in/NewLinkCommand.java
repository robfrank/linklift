package it.robfrank.linklift.application.port.in;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record NewLinkCommand(@NonNull String url, @Nullable String title, @Nullable String description, @Nullable String userId) {
  /**
   * Creates a NewLinkCommand without user context (for backward compatibility).
   */
  public NewLinkCommand(@NonNull String url, @Nullable String title, @Nullable String description) {
    this(url, title, description, null);
  }
}
