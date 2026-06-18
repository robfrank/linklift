package it.robfrank.linklift.application.domain.exception;

public class NoteNotFoundException extends LinkLiftException {

  public NoteNotFoundException(String noteId) {
    super("Note not found with id: " + noteId, ErrorCode.NOTE_NOT_FOUND);
  }
}
