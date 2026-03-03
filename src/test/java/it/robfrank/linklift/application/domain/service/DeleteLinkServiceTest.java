package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.exception.LinkNotFoundException;
import it.robfrank.linklift.application.port.out.DeleteLinkPort;
import it.robfrank.linklift.application.port.out.LoadLinksPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteLinkServiceTest {

  @Mock
  private LoadLinksPort loadLinksPort;

  @Mock
  private DeleteLinkPort deleteLinkPort;

  private DeleteLinkService deleteLinkService;

  @BeforeEach
  void setUp() {
    deleteLinkService = new DeleteLinkService(loadLinksPort, deleteLinkPort);
  }

  @Test
  void deleteLink_shouldDeleteLink_whenUserOwnsLink() {
    // Arrange
    String linkId = "link-123";
    String userId = "user-123";

    when(loadLinksPort.userOwnsLink(userId, linkId)).thenReturn(true);

    // Act
    deleteLinkService.deleteLink(linkId, userId);

    // Assert
    verify(loadLinksPort, times(1)).userOwnsLink(userId, linkId);
    verify(deleteLinkPort, times(1)).deleteLink(linkId);
  }

  @Test
  void deleteLink_shouldThrowException_whenUserDoesNotOwnLink() {
    // Arrange
    String linkId = "link-123";
    String userId = "user-123";

    when(loadLinksPort.userOwnsLink(userId, linkId)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> deleteLinkService.deleteLink(linkId, userId))
      .isInstanceOf(LinkNotFoundException.class)
      .hasMessageContaining("Link not found or not owned by user");

    verify(loadLinksPort, times(1)).userOwnsLink(userId, linkId);
    verify(deleteLinkPort, never()).deleteLink(any());
  }

  @Test
  void deleteLink_shouldThrowException_whenLinkDoesNotExist() {
    // Arrange
    String linkId = "non-existent-link";
    String userId = "user-123";

    when(loadLinksPort.userOwnsLink(userId, linkId)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> deleteLinkService.deleteLink(linkId, userId)).isInstanceOf(LinkNotFoundException.class);

    verify(deleteLinkPort, never()).deleteLink(any());
  }
}
