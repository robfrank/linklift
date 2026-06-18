package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.exception.LinkNotFoundException;
import it.robfrank.linklift.application.domain.exception.ValidationException;
import it.robfrank.linklift.application.domain.model.Note;
import it.robfrank.linklift.application.port.in.CreateNoteCommand;
import it.robfrank.linklift.application.port.out.LoadLinksPort;
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

  @Mock
  private LoadLinksPort loadLinksPort;

  private CreateNoteService service;

  @BeforeEach
  void setUp() {
    service = new CreateNoteService(noteRepository, loadLinksPort);
  }

  @Test
  void createNote_savesAndReturnsNote() {
    when(loadLinksPort.userOwnsLink("user-1", "link-1")).thenReturn(true);
    when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

    Note note = service.createNote(new CreateNoteCommand("link-1", "user-1", "my note"));

    assertThat(note.linkId()).isEqualTo("link-1");
    assertThat(note.userId()).isEqualTo("user-1");
    assertThat(note.content()).isEqualTo("my note");
    assertThat(note.id()).isNotBlank();
    verify(noteRepository).save(any(Note.class));
  }

  @Test
  void createNote_rejectsWhenUserDoesNotOwnLink() {
    when(loadLinksPort.userOwnsLink("user-1", "link-1")).thenReturn(false);

    assertThatThrownBy(() -> service.createNote(new CreateNoteCommand("link-1", "user-1", "my note"))).isInstanceOf(LinkNotFoundException.class);
    verify(noteRepository, never()).save(any());
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
