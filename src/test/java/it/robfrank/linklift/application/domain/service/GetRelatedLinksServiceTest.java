package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.exception.ValidationException;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.port.out.LoadContentPort;
import it.robfrank.linklift.application.port.out.LoadLinksPort;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetRelatedLinksServiceTest {

  @Mock
  private LoadLinksPort loadLinksPort;

  @Mock
  private LoadContentPort loadContentPort;

  private GetRelatedLinksService getRelatedLinksService;

  @BeforeEach
  void setUp() {
    getRelatedLinksService = new GetRelatedLinksService(loadLinksPort, loadContentPort);
  }

  @Test
  void getRelatedLinks_shouldReturnRelatedLinks_whenLinksExist() {
    // Arrange
    String linkId = "link-123";
    String userId = "user-123";
    List<Link> relatedLinks = Arrays.asList(
      new Link("link-2", "https://example.com/2", "Related 1", "Desc 1", LocalDateTime.now(), "text/html", List.of()),
      new Link("link-3", "https://example.com/3", "Related 2", "Desc 2", LocalDateTime.now(), "text/html", List.of()),
      new Link("link-4", "https://example.com/4", "Related 3", "Desc 3", LocalDateTime.now(), "text/html", List.of())
    );

    when(loadLinksPort.getRelatedLinks(linkId, userId)).thenReturn(relatedLinks);

    // Act
    List<Link> result = getRelatedLinksService.getRelatedLinks(linkId, userId);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).hasSize(3);
    assertThat(result).containsExactlyElementsOf(relatedLinks);

    verify(loadLinksPort, times(1)).getRelatedLinks(linkId, userId);
  }

  @Test
  void getRelatedLinks_shouldReturnEmptyList_whenNoRelatedLinksExist() {
    // Arrange
    String linkId = "link-123";
    String userId = "user-123";
    List<Link> emptyLinks = List.of();

    when(loadLinksPort.getRelatedLinks(linkId, userId)).thenReturn(emptyLinks);

    // Act
    List<Link> result = getRelatedLinksService.getRelatedLinks(linkId, userId);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();

    verify(loadLinksPort, times(1)).getRelatedLinks(linkId, userId);
  }

  @Test
  void getRelatedLinks_shouldCallPortWithCorrectParameters() {
    // Arrange
    String linkId = "link-123";
    String userId = "user-123";

    when(loadLinksPort.getRelatedLinks(linkId, userId)).thenReturn(List.of());

    // Act
    getRelatedLinksService.getRelatedLinks(linkId, userId);

    // Assert
    verify(loadLinksPort, times(1)).getRelatedLinks(linkId, userId);
  }

  @Test
  void getRelatedLinks_shouldThrowValidationException_whenLinkIdIsNull() {
    // Arrange
    String userId = "user-123";

    // Act & Assert
    assertThatThrownBy(() -> getRelatedLinksService.getRelatedLinks(null, userId))
      .isInstanceOf(ValidationException.class)
      .hasMessageContaining("linkId cannot be empty");

    verify(loadLinksPort, never()).getRelatedLinks(any(), any());
  }

  @Test
  void getRelatedLinks_shouldThrowValidationException_whenLinkIdIsEmpty() {
    // Arrange
    String userId = "user-123";

    // Act & Assert
    assertThatThrownBy(() -> getRelatedLinksService.getRelatedLinks("", userId))
      .isInstanceOf(ValidationException.class)
      .hasMessageContaining("linkId cannot be empty");

    verify(loadLinksPort, never()).getRelatedLinks(any(), any());
  }

  @Test
  void getRelatedLinks_shouldThrowValidationException_whenLinkIdIsBlank() {
    // Arrange
    String userId = "user-123";

    // Act & Assert
    assertThatThrownBy(() -> getRelatedLinksService.getRelatedLinks("   ", userId))
      .isInstanceOf(ValidationException.class)
      .hasMessageContaining("linkId cannot be empty");

    verify(loadLinksPort, never()).getRelatedLinks(any(), any());
  }

  @Test
  void getRelatedLinks_shouldThrowValidationException_whenUserIdIsNull() {
    // Arrange
    String linkId = "link-123";

    // Act & Assert
    assertThatThrownBy(() -> getRelatedLinksService.getRelatedLinks(linkId, null))
      .isInstanceOf(ValidationException.class)
      .hasMessageContaining("userId cannot be empty");

    verify(loadLinksPort, never()).getRelatedLinks(any(), any());
  }

  @Test
  void getRelatedLinks_shouldThrowValidationException_whenUserIdIsEmpty() {
    // Arrange
    String linkId = "link-123";

    // Act & Assert
    assertThatThrownBy(() -> getRelatedLinksService.getRelatedLinks(linkId, ""))
      .isInstanceOf(ValidationException.class)
      .hasMessageContaining("userId cannot be empty");

    verify(loadLinksPort, never()).getRelatedLinks(any(), any());
  }

  @Test
  void getRelatedLinks_shouldThrowValidationException_whenUserIdIsBlank() {
    // Arrange
    String linkId = "link-123";

    // Act & Assert
    assertThatThrownBy(() -> getRelatedLinksService.getRelatedLinks(linkId, "   "))
      .isInstanceOf(ValidationException.class)
      .hasMessageContaining("userId cannot be empty");

    verify(loadLinksPort, never()).getRelatedLinks(any(), any());
  }
}
