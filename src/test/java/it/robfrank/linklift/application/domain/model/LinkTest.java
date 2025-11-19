package it.robfrank.linklift.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class LinkTest {

  @Test
  void constructor_shouldSetAllPropertiesCorrectly() {
    // Arrange
    String id = "test-id";
    String url = "https://example.com";
    String title = "Example Title";
    String description = "Example Description";
    LocalDateTime extractedAt = LocalDateTime.now();
    String contentType = "text/html";

    // Act
    Link link = new Link(id, url, title, description, extractedAt, contentType, null, null, null);

    // Assert
    assertThat(link.id()).isEqualTo(id);
    assertThat(link.url()).isEqualTo(url);
    assertThat(link.title()).isEqualTo(title);
    assertThat(link.description()).isEqualTo(description);
    assertThat(link.extractedAt()).isEqualTo(extractedAt);
    assertThat(link.contentType()).isEqualTo(contentType);
  }

  @Test
  void constructor_shouldSetCurrentTimestampWhenExtractedAtIsNull() {
    // Arrange
    LocalDateTime before = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    // Act
    Link link = new Link("id", "url", "title", "description", null, "contentType", null, null, null);

    LocalDateTime after = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    // Assert
    assertThat(link.extractedAt()).isNotNull();
    LocalDateTime extractedAt = link.extractedAt().truncatedTo(ChronoUnit.SECONDS);
    assertThat(extractedAt).isAfterOrEqualTo(before);
    assertThat(extractedAt).isBeforeOrEqualTo(after);
  }

  @Test
  void equals_shouldReturnTrue_whenLinksHaveSameData() {
    // Arrange
    LocalDateTime timestamp = LocalDateTime.now();
    Link link1 = new Link("id", "url", "title", "description", timestamp, "contentType", null, null, null);
    Link link2 = new Link("id", "url", "title", "description", timestamp, "contentType", null, null, null);

    // Assert
    assertThat(link1).isEqualTo(link2);
    assertThat(link1.hashCode()).isEqualTo(link2.hashCode());
  }
}
