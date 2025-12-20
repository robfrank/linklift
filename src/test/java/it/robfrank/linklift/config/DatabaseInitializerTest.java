package it.robfrank.linklift.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.arcadedb.Constants;
import com.arcadedb.remote.RemoteDatabase;
import com.arcadedb.remote.RemoteSchema;
import com.arcadedb.schema.Type;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class DatabaseInitializerTest {

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

  @Test
  @DisplayName("initialize database schema")
  void initializeDatabaseSchema() {
    new DatabaseInitializer(arcadeDBContainer.getHost(), arcadeDBContainer.getMappedPort(2480), "root", "playwithdata").initializeDatabase();

    RemoteDatabase db = new RemoteDatabase(arcadeDBContainer.getHost(), arcadeDBContainer.getMappedPort(2480), "linklift", "root", "playwithdata");

    RemoteSchema schema = db.getSchema();
    assertThat(schema.existsType("Link")).isTrue();
    assertThat(schema.getType("Link").getProperty("extractedAt").getType()).isEqualTo(Type.DATETIME_SECOND);
    assertThat(schema.existsType("Content")).isTrue();
    assertThat(schema.getType("Content").getProperty("linkId").getType()).isEqualTo(Type.STRING);
  }
}
