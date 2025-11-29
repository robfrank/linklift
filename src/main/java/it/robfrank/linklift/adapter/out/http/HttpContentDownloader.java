package it.robfrank.linklift.adapter.out.http;

import it.robfrank.linklift.application.domain.exception.ContentDownloadException;
import it.robfrank.linklift.application.port.out.ContentDownloaderPort;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpContentDownloader implements ContentDownloaderPort {

  private static final Logger logger = LoggerFactory.getLogger(HttpContentDownloader.class);

  private static final Duration TIMEOUT = Duration.ofSeconds(30);
  private static final int MAX_RETRIES = 3;

  private final HttpClient httpClient;

  public HttpContentDownloader(@NonNull HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  @Override
  public @NonNull CompletableFuture<DownloadedContent> downloadContent(@NonNull String url) {
    return downloadWithRetry(url, 0);
  }

  private CompletableFuture<DownloadedContent> downloadWithRetry(@NonNull String url, int attemptNumber) {
    if (attemptNumber >= MAX_RETRIES) {
      return CompletableFuture.failedFuture(new ContentDownloadException("Max retries exceeded for URL: " + url));
    }

    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(TIMEOUT).header("User-Agent", "LinkLift/1.0 (Content Extractor)").GET().build();

    return httpClient
      .sendAsync(request, HttpResponse.BodyHandlers.ofString())
      .thenCompose(response -> {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
          try {
            String htmlContent = response.body();
            String mimeType = response.headers().firstValue("Content-Type").orElse("text/html");

            // Extract text content using Jsoup
            Document doc = Jsoup.parse(htmlContent);
            String textContent = doc.body().text();

            int contentLength = htmlContent.getBytes().length;

            return CompletableFuture.completedFuture(new DownloadedContent(htmlContent, textContent, mimeType, contentLength));
          } catch (Exception e) {
            return CompletableFuture.failedFuture(new ContentDownloadException("Failed to parse content from URL: " + url, e));
          }
        } else if (response.statusCode() >= 500 && attemptNumber < MAX_RETRIES - 1) {
          // Retry on server errors
          logger.warn("Retrying download for {} due to server error {} (attempt {})", url, response.statusCode(), attemptNumber + 1);
          return downloadWithRetry(url, attemptNumber + 1);
        } else {
          return CompletableFuture.failedFuture(new ContentDownloadException("HTTP error " + response.statusCode() + " for URL: " + url));
        }
      })
      .exceptionally(throwable -> {
        if (throwable.getCause() instanceof IOException && attemptNumber < MAX_RETRIES - 1) {
          // Retry on network errors
          logger.warn("Retrying download for {} due to network error (attempt {})", url, attemptNumber + 1);
          try {
            return downloadWithRetry(url, attemptNumber + 1).join();
          } catch (Exception e) {
            throw new ContentDownloadException("Failed to download content after retries: " + url, e);
          }
        } else {
          throw new ContentDownloadException("Failed to download content from URL: " + url, throwable);
        }
      });
  }
}
