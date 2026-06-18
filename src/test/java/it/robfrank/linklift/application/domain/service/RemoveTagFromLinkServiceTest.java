package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.exception.AuthenticationException;
import it.robfrank.linklift.application.domain.exception.TagNotFoundException;
import it.robfrank.linklift.application.domain.model.Tag;
import it.robfrank.linklift.application.port.out.TagRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RemoveTagFromLinkServiceTest {

  @Mock
  private TagRepository tagRepository;

  private RemoveTagFromLinkService service;

  @BeforeEach
  void setUp() {
    service = new RemoveTagFromLinkService(tagRepository);
  }

  private static Tag tag(String userId) {
    return new Tag("t1", "name", userId, LocalDateTime.now());
  }

  @Test
  void removeTagFromLink_removesWhenTagOwned() {
    when(tagRepository.findById("t1")).thenReturn(Optional.of(tag("user-1")));

    service.removeTagFromLink("link-1", "t1", "user-1");

    verify(tagRepository).removeTagFromLink("link-1", "t1");
  }

  @Test
  void removeTagFromLink_throwsWhenTagNotFound() {
    when(tagRepository.findById("t1")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.removeTagFromLink("link-1", "t1", "user-1")).isInstanceOf(TagNotFoundException.class);
    verify(tagRepository, never()).removeTagFromLink(any(), any());
  }

  @Test
  void removeTagFromLink_throwsWhenTagNotOwned() {
    when(tagRepository.findById("t1")).thenReturn(Optional.of(tag("owner")));

    assertThatThrownBy(() -> service.removeTagFromLink("link-1", "t1", "intruder")).isInstanceOf(AuthenticationException.class);
    verify(tagRepository, never()).removeTagFromLink(any(), any());
  }
}
