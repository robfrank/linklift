package it.robfrank.linklift.application.domain.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import it.robfrank.linklift.application.domain.event.LinkCreatedEvent;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.port.out.SaveLinkPort;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LinkContentExtractorServiceTest {

  @RegisterExtension
  static WireMockExtension wireMock = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private LinkContentExtractorService linkContentExtractorService;

  @Mock
  private SaveLinkPort saveLinkPort;

  private ExecutorService testExecutorService;

  @BeforeEach
  void setUp() {
    WireMock.configureFor(wireMock.getPort());
    testExecutorService = Executors.newSingleThreadExecutor();
    linkContentExtractorService = new LinkContentExtractorService(testExecutorService, saveLinkPort);
  }

  @Test
  @DisplayName("should extract content and update link when LinkCreatedEvent is received")
  void shouldExtractContentAndUpdateLink() throws Exception {
    // Given
    String url = wireMock.baseUrl() + "/test.html";
    Link originalLink = new Link("id-123", url, "Original Title", "Original Description", LocalDateTime.now(), "text/html", List.of());
    LinkCreatedEvent event = new LinkCreatedEvent(originalLink, "user-1");

    String html =
      """
      <html>
          <head>
              <title>Extracted Title</title>
              <meta property="og:image" content="http://example.com/og.jpg">
          </head>
          <body>
              <article>
                  <p>This is the main content of the article.</p>
              </article>
          </body>
      </html>
      """;

    stubFor(get(urlEqualTo("/test.html")).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "text/html").withBody(html)));

    // When
    linkContentExtractorService.handle(event);

    // Then
    testExecutorService.shutdown();
    testExecutorService.awaitTermination(5, TimeUnit.SECONDS);

    ArgumentCaptor<Link> linkCaptor = ArgumentCaptor.forClass(Link.class);
    verify(saveLinkPort).save(linkCaptor.capture(), eq("user-1"));

    Link capturedLink = linkCaptor.getValue();
    assertThat(capturedLink.id()).isEqualTo(originalLink.id());
    assertThat(capturedLink.url()).isEqualTo(originalLink.url());

    // Verify that WireMock was called
    WireMock.verify(getRequestedFor(urlEqualTo("/test.html")));
  }

  @Test
  @DisplayName("should handle extraction failure gracefully")
  void shouldHandleExtractionFailure() throws Exception {
    // Given
    String url = wireMock.baseUrl() + "/fail.html";
    Link originalLink = new Link("id-123", url, "Original Title", "Original Description", LocalDateTime.now(), "text/html", List.of());
    LinkCreatedEvent event = new LinkCreatedEvent(originalLink, "user-1");

    stubFor(get(urlEqualTo("/fail.html")).willReturn(aResponse().withStatus(500)));

    // When
    linkContentExtractorService.handle(event);

    // Then
    testExecutorService.shutdown();
    testExecutorService.awaitTermination(5, TimeUnit.SECONDS);

    // Should NOT have called save since it failed (or at least handle the error)
    // Looking at the code, it logs error and doesn't call saveLinkPort.save if
    // IOException occurs.
    verify(saveLinkPort, never()).save(any(), anyString());

    WireMock.verify(getRequestedFor(urlEqualTo("/fail.html")));
  }

  @Test
  @DisplayName("should use extracted title and description when missing in original link")
  void shouldExtractTitleAndDescriptionWhenMissing() throws Exception {
    // Given
    String url = wireMock.baseUrl() + "/fill.html";
    // Title and description are null/empty
    Link originalLink = new Link("id-empty", url, null, "", LocalDateTime.now(), "text/html", List.of());
    LinkCreatedEvent event = new LinkCreatedEvent(originalLink, "user-1");

    String html =
      """
      <html>
          <head>
              <title>Extracted Title</title>
              <meta name="description" content="Extracted Description">
          </head>
          <body>
          </body>
      </html>
      """;

    stubFor(get(urlEqualTo("/fill.html")).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "text/html").withBody(html)));

    // When
    linkContentExtractorService.handle(event);

    // Then
    testExecutorService.shutdown();
    testExecutorService.awaitTermination(5, TimeUnit.SECONDS);

    ArgumentCaptor<Link> linkCaptor = ArgumentCaptor.forClass(Link.class);
    verify(saveLinkPort).save(linkCaptor.capture(), eq("user-1"));

    Link capturedLink = linkCaptor.getValue();
    assertThat(capturedLink.title()).isEqualTo("Extracted Title");
    assertThat(capturedLink.description()).isEqualTo("Extracted Description");
  }
}
