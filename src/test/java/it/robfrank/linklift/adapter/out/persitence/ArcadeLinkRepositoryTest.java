package it.robfrank.linklift.adapter.out.persitence;

import com.arcadedb.remote.RemoteDatabase;
import com.arcadedb.remote.RemoteServer;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.config.DatabaseConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class ArcadeLinkRepositoryTest {

  @Container
  private static final GenericContainer arcadeDBContainer =
      new GenericContainer("arcadedata/arcadedb:25.2.1").withExposedPorts(2480)
          .withStartupTimeout(Duration.ofSeconds(90)).withEnv("JAVA_OPTS", """
              -Darcadedb.server.rootPassword=playwithdata
              -Darcadedb.server.plugins=Postgres:com.arcadedb.postgres.PostgresProtocolPlugin
              """)
          .waitingFor(Wait.forHttp("/api/v1/ready").forPort(2480).forStatusCode(204));

  private RemoteDatabase       database;
  private ArcadeLinkRepository linkRepository;

  @BeforeAll
  static void setup() {
    RemoteServer server = new RemoteServer(
        arcadeDBContainer.getHost(),
        arcadeDBContainer.getMappedPort(2480),
        "root",
        "playwithdata");

    if (!server.exists("linklift")) {
      server.create("linklift");
    }
    RemoteDatabase database = new RemoteDatabase(
        arcadeDBContainer.getHost(),
        arcadeDBContainer.getMappedPort(2480),
        "linklift",
        "root",
        "playwithdata"
    );
    DatabaseConfig.initializeSchema(database);

  }

  @BeforeEach
  void setUp() {
    database = new RemoteDatabase(
        arcadeDBContainer.getHost(),
        arcadeDBContainer.getMappedPort(2480),
        "linklift",
        "root",
        "playwithdata"
    );
    linkRepository = new ArcadeLinkRepository(
        database,
        new LinkMapper()
    );

  }

  @Test
  void testLinkPersistence() {
    Link testLink = new Link(UUID.randomUUID().toString(),
        "https://example.com",
        "Test Title",
        "Test Description",
        LocalDateTime.now(),
        "text/html"
    );

    var savedLink = linkRepository.saveLink(testLink);

    assertThat(savedLink).isNotNull();
    assertThat(savedLink).isEqualTo(testLink);
  }

}