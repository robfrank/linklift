package it.robfrank.linklift.adapter.out.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.exception.ContentDownloadException;
import it.robfrank.linklift.application.port.out.ContentDownloaderPort;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class HttpContentDownloaderTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    @Mock
    private HttpHeaders httpHeaders;

    private HttpContentDownloader httpContentDownloader;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        httpContentDownloader = new HttpContentDownloader(httpClient);
    }

    @Test
    void downloadContent_shouldReturnDownloadedContentOnSuccess() throws Exception {
        // Arrange
        String url = "https://example.com";
        String htmlContent = "<html><body><p>Test content</p></body></html>";

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(htmlContent);
        when(httpResponse.headers()).thenReturn(httpHeaders);
        when(httpHeaders.firstValue("Content-Type")).thenReturn(Optional.of("text/html; charset=UTF-8"));

        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(httpResponse));

        // Act
        CompletableFuture<ContentDownloaderPort.DownloadedContent> future = httpContentDownloader.downloadContent(url);
        ContentDownloaderPort.DownloadedContent result = future.get();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.htmlContent()).isEqualTo(htmlContent);
        assertThat(result.textContent()).contains("Test content");
        assertThat(result.mimeType()).isEqualTo("text/html; charset=UTF-8");
        assertThat(result.contentLength()).isGreaterThan(0);

        verify(httpClient, times(1)).sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void downloadContent_shouldExtractTextFromHtml() throws Exception {
        // Arrange
        String url = "https://example.com";
        String htmlContent = "<html><head><title>Test</title></head><body><h1>Hello</h1><p>World</p></body></html>";

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(htmlContent);
        when(httpResponse.headers()).thenReturn(httpHeaders);
        when(httpHeaders.firstValue("Content-Type")).thenReturn(Optional.of("text/html"));

        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(httpResponse));

        // Act
        CompletableFuture<ContentDownloaderPort.DownloadedContent> future = httpContentDownloader.downloadContent(url);
        ContentDownloaderPort.DownloadedContent result = future.get();

        // Assert
        assertThat(result.textContent()).contains("Hello");
        assertThat(result.textContent()).contains("World");
        assertThat(result.textContent()).doesNotContain("<html>");
        assertThat(result.textContent()).doesNotContain("<body>");
    }

    @Test
    void downloadContent_shouldUseDefaultContentTypeWhenNotProvided() throws Exception {
        // Arrange
        String url = "https://example.com";
        String htmlContent = "<html><body>Test</body></html>";

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(htmlContent);
        when(httpResponse.headers()).thenReturn(httpHeaders);
        when(httpHeaders.firstValue("Content-Type")).thenReturn(Optional.empty());

        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(httpResponse));

        // Act
        CompletableFuture<ContentDownloaderPort.DownloadedContent> future = httpContentDownloader.downloadContent(url);
        ContentDownloaderPort.DownloadedContent result = future.get();

        // Assert
        assertThat(result.mimeType()).isEqualTo("text/html");
    }

    @Test
    void downloadContent_shouldFailOnClientError() {
        // Arrange
        String url = "https://example.com";

        when(httpResponse.statusCode()).thenReturn(404);

        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(httpResponse));

        // Act
        CompletableFuture<ContentDownloaderPort.DownloadedContent> future = httpContentDownloader.downloadContent(url);

        // Assert
        try {
            future.get();
            // Should not reach here
            assert false : "Expected exception was not thrown";
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(ContentDownloadException.class);
            String message = e.getCause().getMessage();
            // Could be either "HTTP error 404" from thenCompose or wrapped in exceptionally
            assertThat(message).satisfiesAnyOf(
                    msg -> assertThat(msg).contains("HTTP error 404"),
                    msg -> assertThat(msg).contains("Failed to download content from URL")
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void downloadContent_shouldRetryOnServerError() throws Exception {
        // Arrange
        String url = "https://example.com";
        String htmlContent = "<html><body>Success</body></html>";

        // Create two separate response mocks for first and second attempt
        HttpResponse<String> errorResponse = mock(HttpResponse.class);
        HttpResponse<String> successResponse = mock(HttpResponse.class);

        when(errorResponse.statusCode()).thenReturn(503);

        when(successResponse.statusCode()).thenReturn(200);
        when(successResponse.body()).thenReturn(htmlContent);
        when(successResponse.headers()).thenReturn(httpHeaders);
        when(httpHeaders.firstValue("Content-Type")).thenReturn(Optional.of("text/html"));

        // First call returns error, second call returns success
        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(errorResponse))
                .thenReturn(CompletableFuture.completedFuture(successResponse));

        // Act
        CompletableFuture<ContentDownloaderPort.DownloadedContent> future = httpContentDownloader.downloadContent(url);
        ContentDownloaderPort.DownloadedContent result = future.get();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.textContent()).contains("Success");

        // Verify retry happened
        verify(httpClient, times(2)).sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void downloadContent_shouldFailAfterMaxRetries() {
        // Arrange
        String url = "https://example.com";

        when(httpResponse.statusCode()).thenReturn(503);

        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(httpResponse));

        // Act
        CompletableFuture<ContentDownloaderPort.DownloadedContent> future = httpContentDownloader.downloadContent(url);

        // Assert
        try {
            future.get();
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(ContentDownloadException.class);
            assertThat(e.getCause().getMessage()).contains("Failed to download content from URL");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify all 3 attempts were made
        verify(httpClient, times(3)).sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void downloadContent_shouldHandleNetworkErrors() {
        // Arrange
        String url = "https://example.com";

        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.failedFuture(new IOException("Network error")));

        // Act
        CompletableFuture<ContentDownloaderPort.DownloadedContent> future = httpContentDownloader.downloadContent(url);

        // Assert
        try {
            future.get();
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(ContentDownloadException.class);
            assertThat(e.getCause().getMessage()).contains("Failed to download content after retries");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
