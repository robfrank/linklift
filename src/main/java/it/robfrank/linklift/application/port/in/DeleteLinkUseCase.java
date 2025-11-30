package it.robfrank.linklift.application.port.in;

import org.jspecify.annotations.NonNull;

public interface DeleteLinkUseCase {
  void deleteLink(@NonNull String id, @NonNull String userId);
}
