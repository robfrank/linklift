package it.robfrank.linklift.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.arcadedb.Constants;
import com.arcadedb.remote.RemoteDatabase;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.domain.model.LinkPage;
import it.robfrank.linklift.application.domain.model.ReadStatus;
import it.robfrank.linklift.application.port.in.ListLinksQuery;
import it.robfrank.linklift.config.DatabaseInitializer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class ArcadeLinkRepositoryTest {

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
    .withCreateContainerCmdModifier(cmd -> ((com.github.dockerjava.api.command.CreateContainerCmd) cmd).withUser("root"))
    .waitingFor(Wait.forHttp("/api/v1/ready").forPort(2480).forStatusCode(204));

  private RemoteDatabase database;
  private ArcadeLinkRepository linkRepository;

  @BeforeAll
  static void setup() {
    new DatabaseInitializer(arcadeDBContainer.getHost(), arcadeDBContainer.getMappedPort(2480), "root", "playwithdata").initializeDatabase();
  }

  @BeforeEach
  void setUp() {
    database = new RemoteDatabase(arcadeDBContainer.getHost(), arcadeDBContainer.getMappedPort(2480), "linklift", "root", "playwithdata");
    linkRepository = new ArcadeLinkRepository(database, new LinkMapper());
  }

  @Test
  void shouldSaveLink() {
    Link testLink = new Link(
      UUID.randomUUID().toString(),
      "https://example2.com",
      "Test Title",
      "Test Description",
      LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
      "text/html",
      List.of(),
      ReadStatus.UNREAD,
      false,
      false
    );

    var savedLink = linkRepository.saveLink(testLink);

    assertThat(savedLink).isNotNull();
    assertThat(savedLink).isEqualTo(testLink);
  }

  @Test
  void shouldFindLinkByUrl() {
    Link testLink = new Link(
      UUID.randomUUID().toString(),
      "https://example3.com",
      "Test Title",
      "Test Description",
      LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
      "text/html",
      List.of(),
      ReadStatus.UNREAD,
      false,
      false
    );

    var savedLink = linkRepository.saveLink(testLink);
    var foundLink = linkRepository.findLinkByUrl("https://example3.com");

    assertThat(foundLink).isPresent();
    assertThat(foundLink.get()).isEqualTo(testLink);
  }

  @Test
  void shouldFindLinkByid() {
    Link testLink = new Link(
      UUID.randomUUID().toString(),
      "https://example4.com",
      "Test Title",
      "Test Description",
      LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
      "text/html",
      List.of(),
      ReadStatus.UNREAD,
      false,
      false
    );

    var savedLink = linkRepository.saveLink(testLink);
    var foundLink = linkRepository.findLinkById(testLink.id());

    assertThat(foundLink).isPresent();
    assertThat(foundLink.get()).isEqualTo(testLink);
  }

  @Test
  void findLinksWithPaginationForUser_appliesParameterizedStatusFilter() {
    String userId = UUID.randomUUID().toString();
    database.command(
      "sql",
      "INSERT INTO User SET id = ?, username = ?, email = ?, passwordHash = 'h', salt = 's', createdAt = sysdate(), isActive = true",
      userId,
      "u-" + userId,
      userId + "@test.local"
    );

    LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    Link readLink = new Link(UUID.randomUUID().toString(), "https://read.example", "Read", "d", now, "text/html", List.of(), ReadStatus.READ, false, false);
    Link unreadLink = new Link(
      UUID.randomUUID().toString(),
      "https://unread.example",
      "Unread",
      "d",
      now,
      "text/html",
      List.of(),
      ReadStatus.UNREAD,
      false,
      false
    );
    linkRepository.saveLinkForUser(readLink, userId);
    linkRepository.saveLinkForUser(unreadLink, userId);

    ListLinksQuery query = ListLinksQuery.forUserWithFiltersAndTag(0, 20, "extractedAt", "DESC", userId, ReadStatus.READ, null, null, null);
    LinkPage page = linkRepository.findLinksWithPaginationForUser(query, userId);

    assertThat(page.content()).extracting(Link::id).containsExactly(readLink.id());
    assertThat(page.totalElements()).isEqualTo(1);
  }
}
