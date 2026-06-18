package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.exception.ValidationException;
import it.robfrank.linklift.application.domain.model.Note;
import it.robfrank.linklift.application.port.out.NoteRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetNotesForLinkServiceTest {

  @Mock
  private NoteRepository noteRepository;

  private GetNotesForLinkService service;

  @BeforeEach
  void setUp() {
    service = new GetNotesForLinkService(noteRepository);
  }

  @Test
  void getNotesForLink_returnsUserScopedNotes() {
    List<Note> notes = List.of(new Note("n1", "link-1", "user-1", "content", LocalDateTime.now(), null));
    when(noteRepository.findByLinkIdAndUserId("link-1", "user-1")).thenReturn(notes);

    List<Note> result = service.getNotesForLink("link-1", "user-1");

    assertThat(result).isEqualTo(notes);
    verify(noteRepository).findByLinkIdAndUserId("link-1", "user-1");
  }

  @Test
  void getNotesForLink_rejectsBlankLinkId() {
    assertThatThrownBy(() -> service.getNotesForLink("", "user-1")).isInstanceOf(ValidationException.class);
    verifyNoInteractions(noteRepository);
  }
}
