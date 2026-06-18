package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.exception.NoteNotFoundException;
import it.robfrank.linklift.application.domain.model.Note;
import it.robfrank.linklift.application.port.out.NoteRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteNoteServiceTest {

  @Mock
  private NoteRepository noteRepository;

  private DeleteNoteService service;

  @BeforeEach
  void setUp() {
    service = new DeleteNoteService(noteRepository);
  }

  @Test
  void deleteNote_deletesWhenOwned() {
    Note existing = new Note("n1", "link-1", "user-1", "content", LocalDateTime.now(), null);
    when(noteRepository.findById("n1")).thenReturn(Optional.of(existing));

    service.deleteNote("n1", "user-1");

    verify(noteRepository).delete("n1");
  }

  @Test
  void deleteNote_throwsWhenNotFound() {
    when(noteRepository.findById("n1")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.deleteNote("n1", "user-1")).isInstanceOf(NoteNotFoundException.class);
    verify(noteRepository, never()).delete(any());
  }

  @Test
  void deleteNote_throwsWhenNotOwned() {
    Note existing = new Note("n1", "link-1", "owner", "content", LocalDateTime.now(), null);
    when(noteRepository.findById("n1")).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> service.deleteNote("n1", "intruder")).isInstanceOf(NoteNotFoundException.class);
    verify(noteRepository, never()).delete(any());
  }
}
