package it.robfrank.linklift.adapter.out.http;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import it.robfrank.linklift.application.domain.exception.ContentDownloadException;
import it.robfrank.linklift.application.port.out.ContentDownloaderPort;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class HttpContentDownloaderTest {

  @RegisterExtension
  static WireMockExtension wireMock = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private HttpContentDownloader httpContentDownloader;
  private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

  @BeforeEach
  void setUp() {
    WireMock.configureFor(wireMock.getPort());
    httpContentDownloader = new HttpContentDownloader(httpClient);
  }

  @Test
  void downloadContent_shouldReturnDownloadedContentOnSuccess() throws Exception {
    // Arrange
    String url = wireMock.baseUrl() + "/success";
    String htmlContent = "<html><body><p>Test content</p></body></html>";

    stubFor(get(urlEqualTo("/success")).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "text/html; charset=UTF-8").withBody(htmlContent)));

    // Act
    CompletableFuture<ContentDownloaderPort.DownloadedContent> future = httpContentDownloader.downloadContent(url);
    ContentDownloaderPort.DownloadedContent result = future.get();

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.htmlContent()).isEqualTo(htmlContent);
    assertThat(result.textContent()).contains("Test content");
    assertThat(result.mimeType()).isEqualTo("text/html; charset=UTF-8");
    assertThat(result.contentLength()).isGreaterThan(0);

    verify(getRequestedFor(urlEqualTo("/success")));
  }

  @Test
  void downloadContent_shouldExtractTextFromHtml() throws Exception {
    // Arrange
    String url = wireMock.baseUrl() + "/html";
    String htmlContent = "<html><head><title>Test</title></head><body><h1>Hello</h1><p>World</p></body></html>";

    stubFor(get(urlEqualTo("/html")).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "text/html").withBody(htmlContent)));

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
    String url = wireMock.baseUrl() + "/no-content-type";
    String htmlContent = "<html><body>Test</body></html>";

    stubFor(get(urlEqualTo("/no-content-type")).willReturn(aResponse().withStatus(200).withBody(htmlContent)));

    // Act
    CompletableFuture<ContentDownloaderPort.DownloadedContent> future = httpContentDownloader.downloadContent(url);
    ContentDownloaderPort.DownloadedContent result = future.get();

    // Assert
    assertThat(result.mimeType()).isEqualTo("text/html");
  }

  @Test
  void downloadContent_shouldFailOnClientError() {
    // Arrange
    String url = wireMock.baseUrl() + "/404";

    stubFor(get(urlEqualTo("/404")).willReturn(aResponse().withStatus(404)));

    // Act
    CompletableFuture<ContentDownloaderPort.DownloadedContent> future = httpContentDownloader.downloadContent(url);

    // Assert
    try {
      future.get();
      // Should not reach here
      assert false : "Expected exception was not thrown";
    } catch (ExecutionException e) {
      assertThat(e.getCause()).isInstanceOf(ContentDownloadException.class);
      // The outer message is what we see first
      assertThat(e.getCause().getMessage()).contains("Failed to download content from URL");
      // The inner message contains the status code
      assertThat(e.getCause().getCause().getMessage()).contains("HTTP error 404");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Test
  void downloadContent_shouldRetryOnServerError() throws Exception {
    // Arrange
    String url = wireMock.baseUrl() + "/retry";
    String htmlContent = "<html><body>Success</body></html>";

    // First call returns 503, second call returns 200
    stubFor(
      get(urlEqualTo("/retry"))
        .inScenario("Retry Scenario")
        .whenScenarioStateIs(Scenario.STARTED)
        .willReturn(aResponse().withStatus(503))
        .willSetStateTo("Succeeded")
    );

    stubFor(
      get(urlEqualTo("/retry"))
        .inScenario("Retry Scenario")
        .whenScenarioStateIs("Succeeded")
        .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "text/html").withBody(htmlContent))
    );

    // Act
    CompletableFuture<ContentDownloaderPort.DownloadedContent> future = httpContentDownloader.downloadContent(url);
    ContentDownloaderPort.DownloadedContent result = future.get();

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.textContent()).contains("Success");

    // Verify retry happened
    verify(2, getRequestedFor(urlEqualTo("/retry")));
  }

  @Test
  void downloadContent_shouldFailAfterMaxRetries() {
    // Arrange
    String url = wireMock.baseUrl() + "/fail-retries";

    stubFor(get(urlEqualTo("/fail-retries")).willReturn(aResponse().withStatus(503)));

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
    verify(3, getRequestedFor(urlEqualTo("/fail-retries")));
  }

  @Test
  void downloadContent_shouldHandleNetworkErrors() {
    // Arrange
    String url = wireMock.baseUrl() + "/network-error";

    stubFor(get(urlEqualTo("/network-error")).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

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
