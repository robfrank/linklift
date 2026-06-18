package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.exception.NoteNotFoundException;
import it.robfrank.linklift.application.domain.exception.ValidationException;
import it.robfrank.linklift.application.domain.model.Note;
import it.robfrank.linklift.application.port.in.UpdateNoteCommand;
import it.robfrank.linklift.application.port.out.NoteRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateNoteServiceTest {

  @Mock
  private NoteRepository noteRepository;

  private UpdateNoteService service;

  @BeforeEach
  void setUp() {
    service = new UpdateNoteService(noteRepository);
  }

  @Test
  void updateNote_updatesWhenOwned() {
    Note existing = new Note("n1", "link-1", "user-1", "old", LocalDateTime.now(), null);
    when(noteRepository.findById("n1")).thenReturn(Optional.of(existing));
    when(noteRepository.update(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

    Note updated = service.updateNote(new UpdateNoteCommand("n1", "user-1", "new content"));

    assertThat(updated.id()).isEqualTo("n1");
    assertThat(updated.content()).isEqualTo("new content");
    verify(noteRepository).update(any(Note.class));
  }

  @Test
  void updateNote_throwsWhenNotFound() {
    when(noteRepository.findById("n1")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.updateNote(new UpdateNoteCommand("n1", "user-1", "x"))).isInstanceOf(NoteNotFoundException.class);
    verify(noteRepository, never()).update(any());
  }

  @Test
  void updateNote_throwsWhenNotOwned() {
    Note existing = new Note("n1", "link-1", "owner", "old", LocalDateTime.now(), null);
    when(noteRepository.findById("n1")).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> service.updateNote(new UpdateNoteCommand("n1", "intruder", "x"))).isInstanceOf(NoteNotFoundException.class);
    verify(noteRepository, never()).update(any());
  }

  @Test
  void updateNote_rejectsContentExceedingMaxLength() {
    String tooLong = "x".repeat(CreateNoteService.MAX_NOTE_CONTENT_LENGTH + 1);

    assertThatThrownBy(() -> service.updateNote(new UpdateNoteCommand("n1", "user-1", tooLong))).isInstanceOf(ValidationException.class);
    verifyNoInteractions(noteRepository);
  }
}
