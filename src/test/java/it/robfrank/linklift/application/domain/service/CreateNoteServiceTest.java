package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.exception.ValidationException;
import it.robfrank.linklift.application.domain.model.Note;
import it.robfrank.linklift.application.port.in.CreateNoteCommand;
import it.robfrank.linklift.application.port.out.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateNoteServiceTest {

  @Mock
  private NoteRepository noteRepository;

  private CreateNoteService service;

  @BeforeEach
  void setUp() {
    service = new CreateNoteService(noteRepository);
  }

  @Test
  void createNote_savesAndReturnsNote() {
    when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

    Note note = service.createNote(new CreateNoteCommand("link-1", "user-1", "my note"));

    assertThat(note.linkId()).isEqualTo("link-1");
    assertThat(note.userId()).isEqualTo("user-1");
    assertThat(note.content()).isEqualTo("my note");
    assertThat(note.id()).isNotBlank();
    verify(noteRepository).save(any(Note.class));
  }

  @Test
  void createNote_rejectsBlankContent() {
    assertThatThrownBy(() -> service.createNote(new CreateNoteCommand("link-1", "user-1", ""))).isInstanceOf(ValidationException.class);
    verifyNoInteractions(noteRepository);
  }

  @Test
  void createNote_rejectsContentExceedingMaxLength() {
    String tooLong = "x".repeat(CreateNoteService.MAX_NOTE_CONTENT_LENGTH + 1);
    assertThatThrownBy(() -> service.createNote(new CreateNoteCommand("link-1", "user-1", tooLong))).isInstanceOf(ValidationException.class);
    verifyNoInteractions(noteRepository);
  }
}
