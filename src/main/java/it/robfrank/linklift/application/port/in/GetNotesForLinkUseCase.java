package it.robfrank.linklift.application.port.in;

import it.robfrank.linklift.application.domain.model.Note;
import java.util.List;
import org.jspecify.annotations.NonNull;

public interface GetNotesForLinkUseCase {
  List<Note> getNotesForLink(@NonNull String linkId, @NonNull String userId);
}
