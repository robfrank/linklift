package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.exception.NoteNotFoundException;
import it.robfrank.linklift.application.domain.validation.ValidationUtils;
import it.robfrank.linklift.application.port.in.DeleteNoteUseCase;
import it.robfrank.linklift.application.port.out.NoteRepository;
import org.jspecify.annotations.NonNull;

public class DeleteNoteService implements DeleteNoteUseCase {

  private final NoteRepository noteRepository;

  public DeleteNoteService(NoteRepository noteRepository) {
    this.noteRepository = noteRepository;
  }

  @Override
  public void deleteNote(@NonNull String noteId, @NonNull String userId) {
    ValidationUtils.requireNotEmpty(noteId, "noteId");
    ValidationUtils.requireNotEmpty(userId, "userId");

    var note = noteRepository.findById(noteId).orElseThrow(() -> new NoteNotFoundException(noteId));

    if (!note.userId().equals(userId)) {
      throw new NoteNotFoundException(noteId);
    }

    noteRepository.delete(noteId);
  }
}
