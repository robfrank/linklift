package it.robfrank.linklift.application.port.out;

import it.robfrank.linklift.application.domain.model.Note;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

public interface NoteRepository {
  Note save(@NonNull Note note);

  Note update(@NonNull Note note);

  void delete(@NonNull String noteId);

  Optional<Note> findById(@NonNull String noteId);

  List<Note> findByLinkIdAndUserId(@NonNull String linkId, @NonNull String userId);
}
