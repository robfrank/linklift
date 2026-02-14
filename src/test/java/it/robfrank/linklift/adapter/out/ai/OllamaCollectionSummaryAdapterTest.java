package it.robfrank.linklift.adapter.out.ai;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import it.robfrank.linklift.application.domain.model.Collection;
import it.robfrank.linklift.application.domain.model.Link;
import java.net.http.HttpClient;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class OllamaCollectionSummaryAdapterTest {

  @RegisterExtension
  static WireMockExtension wireMock = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private final HttpClient httpClient = HttpClient.newHttpClient();
  private OllamaCollectionSummaryAdapter adapter;

  @BeforeEach
  void setUp() {
    WireMock.configureFor(wireMock.getPort());
    adapter = new OllamaCollectionSummaryAdapter(httpClient, wireMock.baseUrl(), "test-model");
  }

  @Test
  void generateCollectionSummary_shouldReturnSummary() {
    // Arrange
    Collection collection = new Collection("col-1", "Tech News", "Latest in tech", "user-1", null, null);
    Link l1 = new Link("l1", "https://news.com/1", "Java 25 Released", "New features in Java 25", LocalDateTime.now(), "text/html", List.of());
    Link l2 = new Link("l2", "https://news.com/2", "ArcadeDB 25.12", "New vector search capabilities", LocalDateTime.now(), "text/html", List.of());

    String responseJson =
      """
      {
        "response": "This collection focuses on the latest updates in Java and ArcadeDB.",
        "done": true
      }
      """;

    stubFor(post(urlEqualTo("/api/generate")).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(responseJson)));

    // Act
    String summary = adapter.generateCollectionSummary(collection, List.of(l1, l2));

    // Assert
    assertThat(summary).isEqualTo("This collection focuses on the latest updates in Java and ArcadeDB.");
    verify(
      postRequestedFor(urlEqualTo("/api/generate"))
        .withRequestBody(matchingJsonPath("$.model", equalTo("test-model")))
        .withRequestBody(matchingJsonPath("$.prompt", containing("Java 25 Released")))
        .withRequestBody(matchingJsonPath("$.prompt", containing("ArcadeDB 25.12")))
    );
  }
}
