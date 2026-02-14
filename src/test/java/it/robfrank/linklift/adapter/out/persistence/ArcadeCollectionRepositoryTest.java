package it.robfrank.linklift.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.arcadedb.Constants;
import com.arcadedb.remote.RemoteDatabase;
import it.robfrank.linklift.application.domain.model.Collection;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.config.DatabaseInitializer;
import java.time.Duration;
import java.time.LocalDateTime;
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
class ArcadeCollectionRepositoryTest {

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
  private ArcadeCollectionRepository collectionRepository;
  private ArcadeLinkRepository linkRepository;

  @BeforeAll
  static void setup() {
    new DatabaseInitializer(arcadeDBContainer.getHost(), arcadeDBContainer.getMappedPort(2480), "root", "playwithdata").initializeDatabase();
  }

  @BeforeEach
  void setUp() {
    database = new RemoteDatabase(arcadeDBContainer.getHost(), arcadeDBContainer.getMappedPort(2480), "linklift", "root", "playwithdata");
    collectionRepository = new ArcadeCollectionRepository(database);
    linkRepository = new ArcadeLinkRepository(database, new LinkMapper());

    // Clean up
    database.command("sql", "DELETE FROM ContainsLink");
    database.command("sql", "DELETE FROM Collection");
    database.command("sql", "DELETE FROM Link");
  }

  @Test
  void shouldSaveAndFindCollection() {
    Collection collection = new Collection(UUID.randomUUID().toString(), "Test Collection", "Description", "user-1", null, "Summary");

    collectionRepository.save(collection);

    Optional<Collection> found = collectionRepository.findById(collection.id());
    assertThat(found).isPresent();
    assertThat(found.get().name()).isEqualTo("Test Collection");
    assertThat(found.get().summary()).isEqualTo("Summary");
  }

  @Test
  void shouldAddLinkToCollection() {
    Collection collection = new Collection(UUID.randomUUID().toString(), "Test Collection", "Description", "user-1", null, null);
    collectionRepository.save(collection);

    Link link = new Link(UUID.randomUUID().toString(), "https://example.com", "Title", "Desc", LocalDateTime.now(), "text/html", List.of());
    linkRepository.saveLink(link);

    collectionRepository.addLinkToCollection(collection.id(), link.id());

    List<Link> links = collectionRepository.getCollectionLinks(collection.id());
    assertThat(links).hasSize(1);
    assertThat(links.getFirst().id()).isEqualTo(link.id());
  }

  @Test
  void shouldRemoveLinkFromCollection() {
    Collection collection = new Collection(UUID.randomUUID().toString(), "Test Collection", "Description", "user-1", null, null);
    collectionRepository.save(collection);

    Link link = new Link(UUID.randomUUID().toString(), "https://example.com", "Title", "Desc", LocalDateTime.now(), "text/html", List.of());
    linkRepository.saveLink(link);

    collectionRepository.addLinkToCollection(collection.id(), link.id());
    collectionRepository.removeLinkFromCollection(collection.id(), link.id());

    List<Link> links = collectionRepository.getCollectionLinks(collection.id());
    assertThat(links).isEmpty();
  }

  @Test
  void shouldFindByUserId() {
    String userId = "user-123";
    Collection c1 = new Collection(UUID.randomUUID().toString(), "A Collection", "Desc", userId, null, null);
    Collection c2 = new Collection(UUID.randomUUID().toString(), "B Collection", "Desc", userId, null, null);
    collectionRepository.save(c1);
    collectionRepository.save(c2);

    List<Collection> found = collectionRepository.findByUserId(userId);
    assertThat(found).hasSize(2);
    assertThat(found).extracting(Collection::name).containsExactly("A Collection", "B Collection");
  }

  @Test
  void shouldMergeCollections() {
    Collection source = new Collection(UUID.randomUUID().toString(), "Source", "Desc", "user-1", null, null);
    Collection target = new Collection(UUID.randomUUID().toString(), "Target", "Desc", "user-1", null, null);
    collectionRepository.save(source);
    collectionRepository.save(target);

    Link l1 = new Link(UUID.randomUUID().toString(), "https://l1.com", "L1", "D1", LocalDateTime.now(), "text/html", List.of());
    Link l2 = new Link(UUID.randomUUID().toString(), "https://l2.com", "L2", "D2", LocalDateTime.now(), "text/html", List.of());
    linkRepository.saveLink(l1);
    linkRepository.saveLink(l2);

    collectionRepository.addLinkToCollection(source.id(), l1.id());
    collectionRepository.addLinkToCollection(target.id(), l2.id());

    // Act
    collectionRepository.mergeCollections(source.id(), target.id());

    // Assert
    assertThat(collectionRepository.findById(source.id())).isEmpty();
    List<Link> targetLinks = collectionRepository.getCollectionLinks(target.id());
    assertThat(targetLinks).hasSize(2);
    assertThat(targetLinks).extracting(Link::id).containsExactlyInAnyOrder(l1.id(), l2.id());
  }
}
