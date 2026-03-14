package it.robfrank.linklift.application.port.in;

import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface RemoveTagFromLinkUseCase {
  void removeTagFromLink(@NonNull String linkId, @NonNull String tagId, @NonNull String userId);
}
