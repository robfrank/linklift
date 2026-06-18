package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.model.Content;
import it.robfrank.linklift.application.domain.model.DownloadStatus;
import it.robfrank.linklift.application.domain.model.Tag;
import it.robfrank.linklift.application.port.out.LoadContentPort;
import it.robfrank.linklift.application.port.out.TagRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SuggestTagsServiceTest {

  @Mock
  private TagRepository tagRepository;

  @Mock
  private LoadContentPort loadContentPort;

  private SuggestTagsService service;

  @BeforeEach
  void setUp() {
    service = new SuggestTagsService(tagRepository, loadContentPort);
  }

  private static Tag tag(String id, String name, String userId) {
    return new Tag(id, name, userId, LocalDateTime.now());
  }

  @Test
  void suggestTags_fallsBackToUserTags_whenContentHasNoEmbedding() {
    when(loadContentPort.findContentByLinkId("link-1")).thenReturn(Optional.empty());
    List<Tag> userTags = List.of(tag("t1", "java", "user-1"));
    when(tagRepository.findByUserId("user-1")).thenReturn(userTags);

    List<Tag> result = service.suggestTags("link-1", "user-1");

    assertThat(result).containsExactlyElementsOf(userTags);
    verify(loadContentPort, never()).findSimilar(anyList(), anyInt(), anyString());
  }

  @Test
  void suggestTags_suggestsUnseenUserOwnedTagsFromSimilarLinks() {
    Content content = new Content(
      "c1",
      "link-1",
      null,
      "text",
      null,
      LocalDateTime.now(),
      null,
      DownloadStatus.COMPLETED,
      null,
      null,
      null,
      null,
      null,
      null,
      new float[] { 0.1f, 0.2f, 0.3f }
    );
    when(loadContentPort.findContentByLinkId("link-1")).thenReturn(Optional.of(content));

    Content similar = new Content("c2", "link-2", null, "text2", null, LocalDateTime.now(), null, DownloadStatus.COMPLETED);
    when(loadContentPort.findSimilar(anyList(), anyInt(), eq("user-1"))).thenReturn(List.of(similar));

    Tag alreadyOnLink = tag("tA", "existing", "user-1");
    Tag suggestible = tag("tB", "suggest-me", "user-1");
    Tag anotherUsersTag = tag("tC", "theirs", "other-user");
    when(tagRepository.findTagsForLink("link-1")).thenReturn(List.of(alreadyOnLink));
    when(tagRepository.findTagsForLinks(List.of("link-2"))).thenReturn(List.of(alreadyOnLink, suggestible, anotherUsersTag));

    List<Tag> result = service.suggestTags("link-1", "user-1");

    // Excludes the tag already on link-1, excludes another user's tag, keeps the user's unseen tag.
    assertThat(result).containsExactly(suggestible);
  }
}
