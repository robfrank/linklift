package it.robfrank.linklift.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.arcadedb.Constants;
import com.arcadedb.remote.RemoteDatabase;
import com.github.dockerjava.api.command.CreateContainerCmd;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.domain.model.ReadStatus;
import it.robfrank.linklift.application.domain.model.Tag;
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
class ArcadeTagRepositoryTest {

  @Container
  @SuppressWarnings("resource")
  private static final GenericContainer<?> arcadeDBContainer = new GenericContainer<>("arcadedata/arcadedb:" + Constants.getRawVersion())
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
    .withCreateContainerCmdModifier(cmd -> ((CreateContainerCmd) cmd).withUser("root"))
    .waitingFor(Wait.forHttp("/api/v1/ready").forPort(2480).forStatusCode(204));

  private RemoteDatabase database;
  private ArcadeTagRepository tagRepository;
  private ArcadeLinkRepository linkRepository;

  @BeforeAll
  static void setup() {
    new DatabaseInitializer(arcadeDBContainer.getHost(), arcadeDBContainer.getMappedPort(2480), "root", "playwithdata").initializeDatabase();
  }

  @BeforeEach
  void setUp() {
    database = new RemoteDatabase(arcadeDBContainer.getHost(), arcadeDBContainer.getMappedPort(2480), "linklift", "root", "playwithdata");
    tagRepository = new ArcadeTagRepository(database);
    linkRepository = new ArcadeLinkRepository(database, new LinkMapper());
    database.command("sql", "DELETE FROM HasTag");
    database.command("sql", "DELETE FROM Tag");
    database.command("sql", "DELETE FROM Link");
  }

  private Tag makeTag(String name, String userId) {
    return new Tag(UUID.randomUUID().toString(), name, userId, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
  }

  private Link makeLink() {
    return new Link(
      UUID.randomUUID().toString(),
      "https://example.com/" + UUID.randomUUID(),
      "Test Link",
      "A description",
      LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
      "text/html",
      List.of(),
      ReadStatus.UNREAD,
      false,
      false
    );
  }

  @Test
  void shouldSaveAndFindTagById() {
    Tag tag = makeTag("java", "user-1");

    tagRepository.save(tag);

    Optional<Tag> found = tagRepository.findById(tag.id());
    assertThat(found).isPresent();
    assertThat(found.get().name()).isEqualTo("java");
    assertThat(found.get().userId()).isEqualTo("user-1");
  }

  @Test
  void shouldFindTagByNameAndUserId() {
    Tag tag = makeTag("python", "user-1");
    tagRepository.save(tag);

    Optional<Tag> found = tagRepository.findByNameAndUserId("python", "user-1");
    assertThat(found).isPresent();
    assertThat(found.get().id()).isEqualTo(tag.id());
  }

  @Test
  void shouldNotFindTagForDifferentUser() {
    Tag tag = makeTag("rust", "user-1");
    tagRepository.save(tag);

    Optional<Tag> found = tagRepository.findByNameAndUserId("rust", "user-2");
    assertThat(found).isEmpty();
  }

  @Test
  void shouldFindAllTagsByUserId() {
    tagRepository.save(makeTag("java", "user-1"));
    tagRepository.save(makeTag("python", "user-1"));
    tagRepository.save(makeTag("ruby", "user-2"));

    List<Tag> user1Tags = tagRepository.findByUserId("user-1");
    assertThat(user1Tags).hasSize(2);
    assertThat(user1Tags).extracting(Tag::name).containsExactlyInAnyOrder("java", "python");
  }

  @Test
  void shouldDeleteTag() {
    Tag tag = makeTag("deleteme", "user-1");
    tagRepository.save(tag);

    tagRepository.delete(tag.id());

    assertThat(tagRepository.findById(tag.id())).isEmpty();
  }

  @Test
  void shouldAddAndRemoveTagFromLink() {
    var link = makeLink();
    linkRepository.saveLink(link);

    Tag tag = makeTag("interesting", "user-1");
    tagRepository.save(tag);

    tagRepository.addTagToLink(link.id(), tag.id());
    List<Tag> linkTags = tagRepository.findTagsForLink(link.id());
    assertThat(linkTags).hasSize(1);
    assertThat(linkTags.getFirst().name()).isEqualTo("interesting");

    tagRepository.removeTagFromLink(link.id(), tag.id());
    assertThat(tagRepository.findTagsForLink(link.id())).isEmpty();
  }

  @Test
  void shouldNotDuplicateTagOnLink() {
    var link = makeLink();
    linkRepository.saveLink(link);

    Tag tag = makeTag("unique", "user-1");
    tagRepository.save(tag);

    tagRepository.addTagToLink(link.id(), tag.id());
    tagRepository.addTagToLink(link.id(), tag.id()); // Add twice

    List<Tag> linkTags = tagRepository.findTagsForLink(link.id());
    assertThat(linkTags).hasSize(1);
  }

  @Test
  void shouldFindMultipleTagsForLink() {
    var link = makeLink();
    linkRepository.saveLink(link);

    Tag t1 = makeTag("backend", "user-1");
    Tag t2 = makeTag("api", "user-1");
    tagRepository.save(t1);
    tagRepository.save(t2);

    tagRepository.addTagToLink(link.id(), t1.id());
    tagRepository.addTagToLink(link.id(), t2.id());

    List<Tag> linkTags = tagRepository.findTagsForLink(link.id());
    assertThat(linkTags).hasSize(2);
    assertThat(linkTags).extracting(Tag::name).containsExactlyInAnyOrder("backend", "api");
  }

  @Test
  void shouldDeleteTagAndRemoveEdges() {
    var link = makeLink();
    linkRepository.saveLink(link);

    Tag tag = makeTag("orphaned", "user-1");
    tagRepository.save(tag);
    tagRepository.addTagToLink(link.id(), tag.id());

    tagRepository.delete(tag.id());

    // Tag should be gone
    assertThat(tagRepository.findById(tag.id())).isEmpty();
    // Edges should be cleaned up
    assertThat(tagRepository.findTagsForLink(link.id())).isEmpty();
  }
}
