package it.robfrank.linklift.application.port.in;

import org.jspecify.annotations.NonNull;

public interface DeleteContentUseCase {
  void deleteContent(@NonNull DeleteContentCommand command);
}
