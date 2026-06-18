package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.model.Note;
import it.robfrank.linklift.application.domain.validation.ValidationUtils;
import it.robfrank.linklift.application.port.in.CreateNoteCommand;
import it.robfrank.linklift.application.port.in.CreateNoteUseCase;
import it.robfrank.linklift.application.port.out.NoteRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.jspecify.annotations.NonNull;

public class CreateNoteService implements CreateNoteUseCase {

  static final int MAX_NOTE_CONTENT_LENGTH = 10_000;

  private final NoteRepository noteRepository;

  public CreateNoteService(NoteRepository noteRepository) {
    this.noteRepository = noteRepository;
  }

  @Override
  public Note createNote(@NonNull CreateNoteCommand command) {
    ValidationUtils.requireNotEmpty(command.linkId(), "linkId");
    ValidationUtils.requireNotEmpty(command.userId(), "userId");
    ValidationUtils.requireNotEmpty(command.content(), "content");
    ValidationUtils.requireMaxLength(command.content(), MAX_NOTE_CONTENT_LENGTH, "content");

    Note note = new Note(
      UUID.randomUUID().toString(),
      command.linkId(),
      command.userId(),
      command.content(),
      LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
      null
    );

    return noteRepository.save(note);
  }
}
