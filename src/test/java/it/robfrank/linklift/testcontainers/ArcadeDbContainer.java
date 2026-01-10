package it.robfrank.linklift.testcontainers;

import com.arcadedb.Constants;
import com.arcadedb.remote.RemoteDatabase;
import com.arcadedb.remote.RemoteServer;
import it.robfrank.linklift.config.DatabaseInitializer;
import org.jspecify.annotations.NonNull;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Testcontainer for ArcadeDB that handles database lifecycle and schema
 * initialization.
 * This container provides a real ArcadeDB instance for integration testing with
 * vector search capabilities.
 */
public class ArcadeDbContainer extends GenericContainer<ArcadeDbContainer> {

  private static final String IMAGE = "arcadedata/arcadedb-headless:" + Constants.getRawVersion();
  private static final int ARCADE_PORT = 2480;
  private static final String DEFAULT_DATABASE = "linklift";
  private static final String ROOT_PASSWORD = System.getProperty("arcadedb.server.rootPassword", "playwithdata");

  public ArcadeDbContainer() {
    super(IMAGE);
    withExposedPorts(ARCADE_PORT);
    withEnv("JAVA_OPTS", "-Darcadedb.server.rootPassword=" + ROOT_PASSWORD);
    waitingFor(Wait.forHttp("/api/v1/ready").forPort(2480).forStatusCode(204));
  }

  /**
   * Gets the HTTP URL for connecting to ArcadeDB.
   *
   * @return the HTTP URL (e.g., "http://localhost:12345")
   */
  public @NonNull String getHttpUrl() {
    return "http://" + getHost() + ":" + getMappedPort(ARCADE_PORT);
  }

  /**
   * Gets the root password for ArcadeDB.
   *
   * @return the root password
   */
  public @NonNull String getRootPassword() {
    return ROOT_PASSWORD;
  }

  /**
   * Gets the default database name.
   *
   * @return the database name
   */
  public @NonNull String getDatabaseName() {
    return DEFAULT_DATABASE;
  }

  /**
   * Creates and initializes a RemoteDatabase connection with vector search
   * schema.
   * This should be called in @BeforeEach to get a fresh database connection.
   *
   * @return initialized RemoteDatabase instance
   */
  public @NonNull RemoteDatabase createDatabase() {
    // Create database if it doesn't exist
    RemoteServer server = new RemoteServer(getHost(), getMappedPort(ARCADE_PORT), "root", getRootPassword());

    // Always drop and recreate database for clean state
    if (server.exists(getDatabaseName())) {
      server.drop(getDatabaseName());
    }
    server.create(getDatabaseName());

    // Connect to database
    RemoteDatabase db = new RemoteDatabase(getHost(), getMappedPort(ARCADE_PORT), getDatabaseName(), "root", getRootPassword());

    // Initialize schema with vector index
    initializeSchema(db);

    return db;
  }

  /**
   * Initializes the database schema with Content vertex type and vector index.
   *
   * @param db the database connection
   */
  private void initializeSchema(@NonNull RemoteDatabase db) {
    DatabaseInitializer databaseInitializer = new DatabaseInitializer(getHost(), getMappedPort(ARCADE_PORT), "root", getRootPassword());
    databaseInitializer.initializeSchema(db);
  }

  /**
   * Cleans up all content from the database.
   * Call this in @AfterEach to ensure test isolation.
   *
   * @param db the database connection
   */
  public void cleanDatabase(@NonNull RemoteDatabase db) {
    RemoteServer server = new RemoteServer(getHost(), getMappedPort(ARCADE_PORT), "root", getRootPassword());
    server.drop(DEFAULT_DATABASE);
    server.close();
  }
}
