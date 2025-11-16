package it.robfrank.linklift.application.port.in;

import org.jspecify.annotations.NonNull;

public interface DownloadContentUseCase {
  void downloadContentAsync(@NonNull DownloadContentCommand command);
}
