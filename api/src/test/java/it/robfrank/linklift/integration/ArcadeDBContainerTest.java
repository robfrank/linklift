package it.robfrank.linklift.integration;

import com.arcadedb.database.Database;
import com.arcadedb.remote.RemoteDatabase;
import com.arcadedb.schema.Schema;
import it.robfrank.linklift.model.Link;
import it.robfrank.linklift.repository.LinkRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.*;

@Testcontainers
public class ArcadeDBContainerTest {
  @Container
  private static final GenericContainer arcadeDBContainer =
      new GenericContainer("arcadedb/arcadedb:24.10.1")
          .withDatabaseName("linklift")
          .withUsername("root")
          .withPassword("root");

  private static RemoteDatabase connection;
  private static LinkRepository linkRepository;

  @BeforeAll
  static void setup() {
    // Configurazione della connessione al container ArcadeDB
    connection = new RemoteDatabase(
        arcadeDBContainer.getHost(),
        arcadeDBContainer.getMappedPort(2480),
        "root",
        "root"
    );

    // Creazione del database se non esiste
    if (!connection.existsDatabase("linklift")) {
      connection.createDatabase("linklift");
    }

    // Inizializzazione del repository
    linkRepository = new LinkRepository(
        arcadeDBContainer.getHost(),
        arcadeDBContainer.getMappedPort(2480)
    );
  }

  @Test
  void testLinkPersistence() {
    // Preparazione di un link di test
    Link testLink = new Link(
        "https://example.com",
        "Test Title",
        "Test Description",
        System.currentTimeMillis(),
        "text/html"
    );

    // Salvataggio del link
    var savedVertex = linkRepository.saveLink(testLink);

    // Verifica
    assertThat(savedVertex).isNotNull();
    assertThat(savedVertex.get("url").toString()).isEqualTo(testLink.url());
    assertThat(savedVertex.get("title").toString()).isEqualTo(testLink.title());
  }

  @Test
  void testLinkUniqueness() {
    // Preparazione di un link di test
    Link testLink = new Link(
        "https://unique.com",
        "Unique Title",
        "Unique Description",
        System.currentTimeMillis(),
        "text/html"
    );

    // Primo salvataggio
    var firstSave = linkRepository.saveLink(testLink);

    // Secondo salvataggio dello stesso link
    var secondSave = linkRepository.saveLink(testLink);

    // Verifica che il secondo salvataggio restituisca lo stesso vertice
    assertThat(firstSave.getIdentity()).isEqualTo(secondSave.getIdentity());
  }

  @AfterAll
  static void tearDown() {
    if (connection != null) {
      connection.close();
    }
  }
}