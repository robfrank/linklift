package it.robfrank.linklift.application.port.in;

import org.jspecify.annotations.NonNull;

public record DeleteContentCommand(@NonNull String linkId) {
  public DeleteContentCommand {
    if (linkId == null || linkId.isBlank()) {
      throw new IllegalArgumentException("LinkId cannot be null or empty");
    }
  }
}
