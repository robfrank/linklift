package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.exception.NoteNotFoundException;
import it.robfrank.linklift.application.domain.model.Note;
import it.robfrank.linklift.application.domain.validation.ValidationUtils;
import it.robfrank.linklift.application.port.in.UpdateNoteCommand;
import it.robfrank.linklift.application.port.in.UpdateNoteUseCase;
import it.robfrank.linklift.application.port.out.NoteRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.jspecify.annotations.NonNull;

public class UpdateNoteService implements UpdateNoteUseCase {

  private final NoteRepository noteRepository;

  public UpdateNoteService(NoteRepository noteRepository) {
    this.noteRepository = noteRepository;
  }

  @Override
  public Note updateNote(@NonNull UpdateNoteCommand command) {
    ValidationUtils.requireNotEmpty(command.noteId(), "noteId");
    ValidationUtils.requireNotEmpty(command.userId(), "userId");
    ValidationUtils.requireNotEmpty(command.content(), "content");
    ValidationUtils.requireMaxLength(command.content(), CreateNoteService.MAX_NOTE_CONTENT_LENGTH, "content");

    Note existing = noteRepository.findById(command.noteId()).orElseThrow(() -> new NoteNotFoundException(command.noteId()));

    if (!existing.userId().equals(command.userId())) {
      throw new NoteNotFoundException(command.noteId());
    }

    Note updated = new Note(
      existing.id(),
      existing.linkId(),
      existing.userId(),
      command.content(),
      existing.createdAt(),
      LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
    );

    return noteRepository.update(updated);
  }
}
