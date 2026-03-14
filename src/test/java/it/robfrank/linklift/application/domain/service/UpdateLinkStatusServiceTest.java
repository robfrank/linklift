package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.exception.LinkNotFoundException;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.domain.model.ReadStatus;
import it.robfrank.linklift.application.port.in.UpdateLinkStatusCommand;
import it.robfrank.linklift.application.port.out.LoadLinksPort;
import it.robfrank.linklift.application.port.out.UpdateLinkPort;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UpdateLinkStatusServiceTest {

  private LoadLinksPort loadLinksPort;
  private UpdateLinkPort updateLinkPort;
  private UpdateLinkStatusService service;

  private static final Link EXISTING_LINK = new Link(
    "link-123",
    "https://example.com",
    "Title",
    "Description",
    LocalDateTime.now(),
    "text/html",
    List.of(),
    ReadStatus.UNREAD,
    false,
    false
  );

  @BeforeEach
  void setUp() {
    loadLinksPort = Mockito.mock(LoadLinksPort.class);
    updateLinkPort = Mockito.mock(UpdateLinkPort.class);
    service = new UpdateLinkStatusService(loadLinksPort, updateLinkPort);

    when(loadLinksPort.getLinkById("link-123")).thenReturn(EXISTING_LINK);
    when(loadLinksPort.userOwnsLink("user-1", "link-123")).thenReturn(true);
    when(updateLinkPort.updateLink(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void shouldMarkLinkAsRead() {
    UpdateLinkStatusCommand command = new UpdateLinkStatusCommand("link-123", ReadStatus.READ, null, null, "user-1");

    Link result = service.updateLinkStatus(command);

    assertThat(result.readStatus()).isEqualTo(ReadStatus.READ);
    assertThat(result.archived()).isFalse();
    assertThat(result.favorited()).isFalse();
  }

  @Test
  void shouldArchiveLink() {
    UpdateLinkStatusCommand command = new UpdateLinkStatusCommand("link-123", null, true, null, "user-1");

    Link result = service.updateLinkStatus(command);

    assertThat(result.archived()).isTrue();
    assertThat(result.readStatus()).isEqualTo(ReadStatus.UNREAD);
  }

  @Test
  void shouldFavoriteLink() {
    UpdateLinkStatusCommand command = new UpdateLinkStatusCommand("link-123", null, null, true, "user-1");

    Link result = service.updateLinkStatus(command);

    assertThat(result.favorited()).isTrue();
  }

  @Test
  void shouldUpdateMultipleStatusFieldsAtOnce() {
    UpdateLinkStatusCommand command = new UpdateLinkStatusCommand("link-123", ReadStatus.READING, false, true, "user-1");

    Link result = service.updateLinkStatus(command);

    assertThat(result.readStatus()).isEqualTo(ReadStatus.READING);
    assertThat(result.archived()).isFalse();
    assertThat(result.favorited()).isTrue();
  }

  @Test
  void shouldPreserveExistingStatusWhenFieldsAreNull() {
    Link linkWithStatus = new Link(
      "link-123",
      "https://example.com",
      "Title",
      "Description",
      LocalDateTime.now(),
      "text/html",
      List.of(),
      ReadStatus.READ,
      true,
      true
    );
    when(loadLinksPort.getLinkById("link-123")).thenReturn(linkWithStatus);

    UpdateLinkStatusCommand command = new UpdateLinkStatusCommand("link-123", null, null, null, "user-1");
    Link result = service.updateLinkStatus(command);

    assertThat(result.readStatus()).isEqualTo(ReadStatus.READ);
    assertThat(result.archived()).isTrue();
    assertThat(result.favorited()).isTrue();
  }

  @Test
  void shouldThrowWhenLinkNotFound() {
    when(loadLinksPort.getLinkById("nonexistent")).thenThrow(new LinkNotFoundException("nonexistent"));

    UpdateLinkStatusCommand command = new UpdateLinkStatusCommand("nonexistent", ReadStatus.READ, null, null, "user-1");

    assertThatThrownBy(() -> service.updateLinkStatus(command)).isInstanceOf(LinkNotFoundException.class);
  }

  @Test
  void shouldThrowWhenUserDoesNotOwnLink() {
    when(loadLinksPort.userOwnsLink("other-user", "link-123")).thenReturn(false);

    UpdateLinkStatusCommand command = new UpdateLinkStatusCommand("link-123", ReadStatus.READ, null, null, "other-user");

    assertThatThrownBy(() -> service.updateLinkStatus(command)).isInstanceOf(LinkNotFoundException.class).hasMessageContaining("not owned by user");
  }
}
