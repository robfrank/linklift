package it.robfrank.linklift.application.port.in;

import it.robfrank.linklift.application.domain.model.Note;
import org.jspecify.annotations.NonNull;

public interface UpdateNoteUseCase {
  Note updateNote(@NonNull UpdateNoteCommand command);
}
