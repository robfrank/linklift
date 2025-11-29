package it.robfrank.linklift.application.port.out;

import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.NonNull;

public interface ContentDownloaderPort {
  @NonNull
  CompletableFuture<DownloadedContent> downloadContent(@NonNull String url);

  record DownloadedContent(@NonNull String htmlContent, @NonNull String textContent, @NonNull String mimeType, int contentLength) {}
}
