package it.robfrank.linklift.testcontainers;

import com.arcadedb.Constants;
import com.arcadedb.remote.RemoteDatabase;
import com.arcadedb.remote.RemoteServer;
import it.robfrank.linklift.config.DatabaseInitializer;
import org.jspecify.annotations.NonNull;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Testcontainer for ArcadeDB that handles database lifecycle and schema initialization.
 * This container provides a real ArcadeDB instance for integration testing with vector search capabilities.
 */
public class ArcadeDbContainer extends GenericContainer<ArcadeDbContainer> {

  private static final String IMAGE = "arcadedata/arcadedb-headless:" + Constants.getRawVersion();
  private static final int ARCADE_PORT = 2480;
  private static final String DEFAULT_DATABASE = "linklift";
  private static final String ROOT_PASSWORD = "playwithdata";

  public ArcadeDbContainer() {
    super(IMAGE);
    withExposedPorts(ARCADE_PORT);
    withEnv("JAVA_OPTS", "-Darcadedb.server.rootPassword=" + ROOT_PASSWORD);
    // Wait for server to start (look for "ArcadeDB Server started" in logs)
    //    waitingFor(Wait.forLogMessage(".*ArcadeDB Server started in.*", 1).withStartupTimeout(java.time.Duration.ofSeconds(60)));
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
   * Creates and initializes a RemoteDatabase connection with vector search schema.
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
    //    db.transaction(() -> {
    //      // Create Content vertex type
    //      db.command("sql", "CREATE VERTEX TYPE Content IF NOT EXISTS");
    //
    //      // Create properties
    //      db.command("sql", "CREATE PROPERTY Content.id IF NOT EXISTS STRING (MANDATORY TRUE, NOTNULL TRUE)");
    //      db.command("sql", "CREATE PROPERTY Content.linkId IF NOT EXISTS STRING (MANDATORY TRUE, NOTNULL TRUE)");
    //      db.command("sql", "CREATE PROPERTY Content.htmlContent IF NOT EXISTS STRING");
    //      db.command("sql", "CREATE PROPERTY Content.textContent IF NOT EXISTS STRING");
    //      db.command("sql", "CREATE PROPERTY Content.contentLength IF NOT EXISTS INTEGER");
    //      db.command("sql", "CREATE PROPERTY Content.downloadedAt IF NOT EXISTS DATETIME_SECOND");
    //      db.command("sql", "CREATE PROPERTY Content.mimeType IF NOT EXISTS STRING");
    //      db.command("sql", "CREATE PROPERTY Content.status IF NOT EXISTS STRING");
    //      db.command("sql", "CREATE PROPERTY Content.summary IF NOT EXISTS STRING");
    //      db.command("sql", "CREATE PROPERTY Content.heroImageUrl IF NOT EXISTS STRING");
    //      db.command("sql", "CREATE PROPERTY Content.extractedTitle IF NOT EXISTS STRING");
    //      db.command("sql", "CREATE PROPERTY Content.extractedDescription IF NOT EXISTS STRING");
    //      db.command("sql", "CREATE PROPERTY Content.author IF NOT EXISTS STRING");
    //      db.command("sql", "CREATE PROPERTY Content.publishedDate IF NOT EXISTS DATETIME_SECOND");
    //      db.command("sql", "CREATE PROPERTY Content.embedding IF NOT EXISTS LIST OF FLOAT");
    //
    //      // Create indexes
    //      db.command("sql", "CREATE INDEX IF NOT EXISTS ON Content (id) UNIQUE");
    //      db.command("sql", "CREATE INDEX IF NOT EXISTS ON Content (linkId) UNIQUE");
    //
    //      // Create vector index for similarity search
    //      // Using LSM_VECTOR with COSINE similarity for 384-dimensional embeddings
    //      db.command(
    //          "sql",
    //          """
    //                CREATE INDEX IF NOT EXISTS ON Content(embedding) LSM_VECTOR METADATA {
    //                  "dimensions": 384,
    //                  "maxConnections": 16,
    //                  "beamWidth": 100,
    //                  "similarity": "COSINE"
    //                }
    //              """
    //      );
    //    });
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
    //    try {
    //      db.transaction(() -> {
    //        db.command("sqlscript", "DELETE VERTEX Content");
    //      });
    //    } catch (Exception e) {
    //      // Ignore errors during cleanup (e.g., if Content type doesn't exist yet)
    //    }
  }
}
