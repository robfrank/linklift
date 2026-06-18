package it.robfrank.linklift.adapter.out.persistence;

import com.arcadedb.exception.ArcadeDBException;
import com.arcadedb.graph.Vertex;
import com.arcadedb.query.sql.executor.Result;
import com.arcadedb.remote.RemoteDatabase;
import it.robfrank.linklift.application.domain.exception.DatabaseException;
import it.robfrank.linklift.application.domain.model.Note;
import it.robfrank.linklift.application.port.out.NoteRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArcadeNoteRepository implements NoteRepository {

  private static final Logger logger = LoggerFactory.getLogger(ArcadeNoteRepository.class);
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final RemoteDatabase database;

  public ArcadeNoteRepository(RemoteDatabase database) {
    this.database = database;
  }

  @Override
  public Note save(@NonNull Note note) {
    try {
      database.transaction(() -> {
        database.command(
          "sql",
          """
          INSERT INTO Note SET
          id = ?,
          linkId = ?,
          userId = ?,
          content = ?,
          createdAt = ?,
          updatedAt = ?
          """,
          note.id(),
          note.linkId(),
          note.userId(),
          note.content(),
          note.createdAt().truncatedTo(ChronoUnit.SECONDS).format(FORMATTER),
          note.updatedAt() != null ? note.updatedAt().truncatedTo(ChronoUnit.SECONDS).format(FORMATTER) : null
        );
      });
      return note;
    } catch (ArcadeDBException e) {
      throw new DatabaseException("Failed to save note: " + note.id(), e);
    }
  }

  @Override
  public Note update(@NonNull Note note) {
    try {
      database.transaction(() -> {
        database.command(
          "sql",
          """
          UPDATE Note SET
          content = ?,
          updatedAt = ?
          WHERE id = ?
          """,
          note.content(),
          note.updatedAt() != null ? note.updatedAt().truncatedTo(ChronoUnit.SECONDS).format(FORMATTER) : null,
          note.id()
        );
      });
      return note;
    } catch (ArcadeDBException e) {
      throw new DatabaseException("Failed to update note: " + note.id(), e);
    }
  }

  @Override
  public void delete(@NonNull String noteId) {
    try {
      database.transaction(() -> {
        database.command("sql", "DELETE FROM Note WHERE id = ?", noteId);
      });
    } catch (ArcadeDBException e) {
      throw new DatabaseException("Failed to delete note: " + noteId, e);
    }
  }

  @Override
  public Optional<Note> findById(@NonNull String noteId) {
    try {
      return database.query("sql", "SELECT FROM Note WHERE id = ?", noteId).stream().findFirst().flatMap(Result::getVertex).map(this::toNote);
    } catch (ArcadeDBException e) {
      throw new DatabaseException("Failed to find note by id: " + noteId, e);
    }
  }

  @Override
  public List<Note> findByLinkIdAndUserId(@NonNull String linkId, @NonNull String userId) {
    try {
      return database
        .query("sql", "SELECT FROM Note WHERE linkId = ? AND userId = ? ORDER BY createdAt ASC", linkId, userId)
        .stream()
        .map(Result::getVertex)
        .flatMap(Optional::stream)
        .map(this::toNote)
        .toList();
    } catch (ArcadeDBException e) {
      throw new DatabaseException("Failed to find notes for link: " + linkId, e);
    }
  }

  private Note toNote(Vertex vertex) {
    LocalDateTime createdAt = vertex.getLocalDateTime("createdAt");
    if (createdAt == null) {
      logger.warn("Note {} is missing createdAt; substituting current time (possible data corruption)", vertex.getString("id"));
      createdAt = LocalDateTime.now();
    }
    LocalDateTime updatedAt = vertex.getLocalDateTime("updatedAt");
    return new Note(vertex.getString("id"), vertex.getString("linkId"), vertex.getString("userId"), vertex.getString("content"), createdAt, updatedAt);
  }
}
