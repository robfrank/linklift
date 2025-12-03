package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.exception.LinkNotFoundException;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.port.in.UpdateLinkCommand;
import it.robfrank.linklift.application.port.out.LoadLinksPort;
import it.robfrank.linklift.application.port.out.UpdateLinkPort;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class UpdateLinkServiceTest {

  @Mock
  private LoadLinksPort loadLinksPort;

  @Mock
  private UpdateLinkPort updateLinkPort;

  private UpdateLinkService updateLinkService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    updateLinkService = new UpdateLinkService(loadLinksPort, updateLinkPort);
  }

  @Test
  void updateLink_shouldUpdateLinkWithNewData_whenValidCommand() {
    // Arrange
    String linkId = "link-123";
    String userId = "user-123";
    Link existingLink = new Link(linkId, "https://example.com", "Old Title", "Old Description", LocalDateTime.now(), "text/html");
    UpdateLinkCommand command = new UpdateLinkCommand(linkId, "New Title", "New Description", userId);

    when(loadLinksPort.getLinkById(linkId)).thenReturn(existingLink);
    when(loadLinksPort.userOwnsLink(userId, linkId)).thenReturn(true);
    when(updateLinkPort.updateLink(any(Link.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    Link result = updateLinkService.updateLink(command);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.title()).isEqualTo("New Title");
    assertThat(result.description()).isEqualTo("New Description");
    assertThat(result.url()).isEqualTo(existingLink.url());
    assertThat(result.id()).isEqualTo(existingLink.id());

    // Verify interactions
    verify(loadLinksPort, times(1)).getLinkById(linkId);
    verify(loadLinksPort, times(1)).userOwnsLink(userId, linkId);
    verify(updateLinkPort, times(1)).updateLink(any(Link.class));
  }

  @Test
  void updateLink_shouldUpdateOnlyTitle_whenDescriptionIsNull() {
    // Arrange
    String linkId = "link-123";
    String userId = "user-123";
    Link existingLink = new Link(linkId, "https://example.com", "Old Title", "Old Description", LocalDateTime.now(), "text/html");
    UpdateLinkCommand command = new UpdateLinkCommand(linkId, "New Title", null, userId);

    when(loadLinksPort.getLinkById(linkId)).thenReturn(existingLink);
    when(loadLinksPort.userOwnsLink(userId, linkId)).thenReturn(true);
    when(updateLinkPort.updateLink(any(Link.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    Link result = updateLinkService.updateLink(command);

    // Assert
    assertThat(result.title()).isEqualTo("New Title");
    assertThat(result.description()).isEqualTo("Old Description");
  }

  @Test
  void updateLink_shouldUpdateOnlyDescription_whenTitleIsNull() {
    // Arrange
    String linkId = "link-123";
    String userId = "user-123";
    Link existingLink = new Link(linkId, "https://example.com", "Old Title", "Old Description", LocalDateTime.now(), "text/html");
    UpdateLinkCommand command = new UpdateLinkCommand(linkId, null, "New Description", userId);

    when(loadLinksPort.getLinkById(linkId)).thenReturn(existingLink);
    when(loadLinksPort.userOwnsLink(userId, linkId)).thenReturn(true);
    when(updateLinkPort.updateLink(any(Link.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    Link result = updateLinkService.updateLink(command);

    // Assert
    assertThat(result.title()).isEqualTo("Old Title");
    assertThat(result.description()).isEqualTo("New Description");
  }

  @Test
  void updateLink_shouldThrowException_whenUserDoesNotOwnLink() {
    // Arrange
    String linkId = "link-123";
    String userId = "user-123";
    Link existingLink = new Link(linkId, "https://example.com", "Old Title", "Old Description", LocalDateTime.now(), "text/html");
    UpdateLinkCommand command = new UpdateLinkCommand(linkId, "New Title", "New Description", userId);

    when(loadLinksPort.getLinkById(linkId)).thenReturn(existingLink);
    when(loadLinksPort.userOwnsLink(userId, linkId)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> updateLinkService.updateLink(command))
      .isInstanceOf(LinkNotFoundException.class)
      .hasMessageContaining("Link not found or not owned by user");

    verify(updateLinkPort, never()).updateLink(any(Link.class));
  }

  @Test
  void updateLink_shouldThrowException_whenLinkDoesNotExist() {
    // Arrange
    String linkId = "non-existent-link";
    String userId = "user-123";
    UpdateLinkCommand command = new UpdateLinkCommand(linkId, "New Title", "New Description", userId);

    when(loadLinksPort.getLinkById(linkId)).thenThrow(new LinkNotFoundException("Link not found"));

    // Act & Assert
    assertThatThrownBy(() -> updateLinkService.updateLink(command)).isInstanceOf(LinkNotFoundException.class).hasMessageContaining("Link not found");

    verify(loadLinksPort, never()).userOwnsLink(any(), any());
    verify(updateLinkPort, never()).updateLink(any(Link.class));
  }
}
