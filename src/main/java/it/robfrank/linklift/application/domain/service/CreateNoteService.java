package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.exception.LinkNotFoundException;
import it.robfrank.linklift.application.domain.model.Note;
import it.robfrank.linklift.application.domain.validation.ValidationUtils;
import it.robfrank.linklift.application.port.in.CreateNoteCommand;
import it.robfrank.linklift.application.port.in.CreateNoteUseCase;
import it.robfrank.linklift.application.port.out.LoadLinksPort;
import it.robfrank.linklift.application.port.out.NoteRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.jspecify.annotations.NonNull;

public class CreateNoteService implements CreateNoteUseCase {

  static final int MAX_NOTE_CONTENT_LENGTH = 10_000;

  private final NoteRepository noteRepository;
  private final LoadLinksPort loadLinksPort;

  public CreateNoteService(NoteRepository noteRepository, LoadLinksPort loadLinksPort) {
    this.noteRepository = noteRepository;
    this.loadLinksPort = loadLinksPort;
  }

  @Override
  public Note createNote(@NonNull CreateNoteCommand command) {
    ValidationUtils.requireNotEmpty(command.linkId(), "linkId");
    ValidationUtils.requireNotEmpty(command.userId(), "userId");
    ValidationUtils.requireNotEmpty(command.content(), "content");
    ValidationUtils.requireMaxLength(command.content(), MAX_NOTE_CONTENT_LENGTH, "content");

    // Only allow annotating a link the caller owns.
    if (!loadLinksPort.userOwnsLink(command.userId(), command.linkId())) {
      throw new LinkNotFoundException("Link not found or not owned by user");
    }

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
