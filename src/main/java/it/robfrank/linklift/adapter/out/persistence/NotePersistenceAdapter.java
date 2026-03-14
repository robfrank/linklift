package it.robfrank.linklift.adapter.out.persistence;

import it.robfrank.linklift.application.domain.model.Note;
import it.robfrank.linklift.application.port.out.NoteRepository;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

public class NotePersistenceAdapter implements NoteRepository {

  private final ArcadeNoteRepository noteRepository;

  public NotePersistenceAdapter(ArcadeNoteRepository noteRepository) {
    this.noteRepository = noteRepository;
  }

  @Override
  public Note save(@NonNull Note note) {
    return noteRepository.save(note);
  }

  @Override
  public Note update(@NonNull Note note) {
    return noteRepository.update(note);
  }

  @Override
  public void delete(@NonNull String noteId) {
    noteRepository.delete(noteId);
  }

  @Override
  public Optional<Note> findById(@NonNull String noteId) {
    return noteRepository.findById(noteId);
  }

  @Override
  public List<Note> findByLinkIdAndUserId(@NonNull String linkId, @NonNull String userId) {
    return noteRepository.findByLinkIdAndUserId(linkId, userId);
  }
}
