package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.exception.AuthenticationException;
import it.robfrank.linklift.application.domain.exception.LinkNotFoundException;
import it.robfrank.linklift.application.domain.exception.TagNotFoundException;
import it.robfrank.linklift.application.domain.model.Tag;
import it.robfrank.linklift.application.port.in.AddTagToLinkCommand;
import it.robfrank.linklift.application.port.out.LoadLinksPort;
import it.robfrank.linklift.application.port.out.TagRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddTagToLinkServiceTest {

  @Mock
  private TagRepository tagRepository;

  @Mock
  private LoadLinksPort loadLinksPort;

  private AddTagToLinkService service;

  @BeforeEach
  void setUp() {
    service = new AddTagToLinkService(tagRepository, loadLinksPort);
  }

  private static Tag tag(String userId) {
    return new Tag("t1", "name", userId, LocalDateTime.now());
  }

  @Test
  void addTagToLink_addsWhenLinkAndTagOwned() {
    when(loadLinksPort.userOwnsLink("user-1", "link-1")).thenReturn(true);
    when(tagRepository.findById("t1")).thenReturn(Optional.of(tag("user-1")));

    service.addTagToLink(new AddTagToLinkCommand("link-1", "t1", "user-1"));

    verify(tagRepository).addTagToLink("link-1", "t1");
  }

  @Test
  void addTagToLink_throwsWhenLinkNotOwned() {
    when(loadLinksPort.userOwnsLink("user-1", "link-1")).thenReturn(false);

    assertThatThrownBy(() -> service.addTagToLink(new AddTagToLinkCommand("link-1", "t1", "user-1"))).isInstanceOf(LinkNotFoundException.class);
    verify(tagRepository, never()).addTagToLink(any(), any());
  }

  @Test
  void addTagToLink_throwsWhenTagNotFound() {
    when(loadLinksPort.userOwnsLink("user-1", "link-1")).thenReturn(true);
    when(tagRepository.findById("t1")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.addTagToLink(new AddTagToLinkCommand("link-1", "t1", "user-1"))).isInstanceOf(TagNotFoundException.class);
    verify(tagRepository, never()).addTagToLink(any(), any());
  }

  @Test
  void addTagToLink_throwsWhenTagNotOwned() {
    when(loadLinksPort.userOwnsLink("intruder", "link-1")).thenReturn(true);
    when(tagRepository.findById("t1")).thenReturn(Optional.of(tag("owner")));

    assertThatThrownBy(() -> service.addTagToLink(new AddTagToLinkCommand("link-1", "t1", "intruder"))).isInstanceOf(AuthenticationException.class);
    verify(tagRepository, never()).addTagToLink(any(), any());
  }
}
