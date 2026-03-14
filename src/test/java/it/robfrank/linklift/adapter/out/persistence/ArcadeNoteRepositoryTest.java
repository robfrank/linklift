package it.robfrank.linklift.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.arcadedb.Constants;
import com.arcadedb.remote.RemoteDatabase;
import it.robfrank.linklift.application.domain.model.Note;
import it.robfrank.linklift.config.DatabaseInitializer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class ArcadeNoteRepositoryTest {

  @Container
  private static final GenericContainer arcadeDBContainer = new GenericContainer("arcadedata/arcadedb:" + Constants.getRawVersion())
    .withExposedPorts(2480)
    .withStartupTimeout(Duration.ofSeconds(90))
    .withEnv(
      "JAVA_OPTS",
      """
      -Darcadedb.dateImplementation=java.time.LocalDate
      -Darcadedb.dateTimeImplementation=java.time.LocalDateTime
      -Darcadedb.server.rootPassword=playwithdata
      -Darcadedb.server.plugins=Postgres:com.arcadedb.postgres.PostgresProtocolPlugin
      """
    )
    .waitingFor(Wait.forHttp("/api/v1/ready").forPort(2480).forStatusCode(204));

  private RemoteDatabase database;
  private ArcadeNoteRepository noteRepository;

  @BeforeAll
  static void setup() {
    new DatabaseInitializer(arcadeDBContainer.getHost(), arcadeDBContainer.getMappedPort(2480), "root", "playwithdata").initializeDatabase();
  }

  @BeforeEach
  void setUp() {
    database = new RemoteDatabase(arcadeDBContainer.getHost(), arcadeDBContainer.getMappedPort(2480), "linklift", "root", "playwithdata");
    noteRepository = new ArcadeNoteRepository(database);
    database.command("sql", "DELETE FROM Note");
  }

  @Test
  void shouldSaveAndFindNoteById() {
    Note note = new Note(UUID.randomUUID().toString(), "link-1", "user-1", "My first note", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), null);

    noteRepository.save(note);

    Optional<Note> found = noteRepository.findById(note.id());
    assertThat(found).isPresent();
    assertThat(found.get().content()).isEqualTo("My first note");
    assertThat(found.get().linkId()).isEqualTo("link-1");
    assertThat(found.get().userId()).isEqualTo("user-1");
  }

  @Test
  void shouldFindNotesByLinkIdAndUserId() {
    String linkId = "link-1";
    String userId = "user-1";

    Note n1 = new Note(UUID.randomUUID().toString(), linkId, userId, "Note 1", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), null);
    Note n2 = new Note(UUID.randomUUID().toString(), linkId, userId, "Note 2", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), null);
    Note n3 = new Note(UUID.randomUUID().toString(), "link-2", userId, "Other link note", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), null);

    noteRepository.save(n1);
    noteRepository.save(n2);
    noteRepository.save(n3);

    List<Note> notes = noteRepository.findByLinkIdAndUserId(linkId, userId);
    assertThat(notes).hasSize(2);
    assertThat(notes).extracting(Note::content).containsExactlyInAnyOrder("Note 1", "Note 2");
  }

  @Test
  void shouldUpdateNote() {
    Note note = new Note(UUID.randomUUID().toString(), "link-1", "user-1", "Original content", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), null);
    noteRepository.save(note);

    LocalDateTime updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    Note updated = new Note(note.id(), note.linkId(), note.userId(), "Updated content", note.createdAt(), updatedAt);
    noteRepository.update(updated);

    Optional<Note> found = noteRepository.findById(note.id());
    assertThat(found).isPresent();
    assertThat(found.get().content()).isEqualTo("Updated content");
  }

  @Test
  void shouldDeleteNote() {
    Note note = new Note(UUID.randomUUID().toString(), "link-1", "user-1", "To be deleted", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), null);
    noteRepository.save(note);

    noteRepository.delete(note.id());

    Optional<Note> found = noteRepository.findById(note.id());
    assertThat(found).isEmpty();
  }

  @Test
  void shouldReturnEmptyWhenNoteNotFound() {
    Optional<Note> found = noteRepository.findById("non-existent-id");
    assertThat(found).isEmpty();
  }

  @Test
  void shouldNotReturnNotesForDifferentUser() {
    Note note = new Note(UUID.randomUUID().toString(), "link-1", "user-1", "User 1 note", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), null);
    noteRepository.save(note);

    List<Note> notes = noteRepository.findByLinkIdAndUserId("link-1", "user-2");
    assertThat(notes).isEmpty();
  }
}
