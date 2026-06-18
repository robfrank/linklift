package it.robfrank.linklift.testcontainers;

import com.arcadedb.remote.RemoteDatabase;
import it.robfrank.linklift.adapter.out.persistence.ArcadeContentRepository;
import it.robfrank.linklift.adapter.out.persistence.ContentPersistenceAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Abstract base class for database integration tests using Testcontainers.
 * Provides a shared ArcadeDB container and real production implementations
 * of ContentPersistenceAdapter and ArcadeContentRepository.
 * <p>
 * This replaces mock-based testing with real database integration to catch
 * SQL errors, schema issues, and vector index configuration problems.
 */
@Testcontainers
public abstract class ArcadeDbTestBase {

  /**
   * Shared ArcadeDB container for all tests in this class.
   * Using static container improves performance by reusing the same container.
   */
  @Container
  protected static final ArcadeDbContainer arcadeDb = new ArcadeDbContainer();

  /**
   * Real database connection (not mocked).
   */
  protected RemoteDatabase database;

  /**
   * Real ContentPersistenceAdapter using actual ArcadeDB (not mocked).
   * Tests validate real SQL queries, vector indexing, and database behavior.
   */
  protected ContentPersistenceAdapter repository;

  /**
   * Sets up database connection and real repository before each test.
   * Creates fresh database schema with vector index configuration.
   */
  @BeforeEach
  void setUpDatabase() {
    // Create fresh database connection and initialize schema
    database = arcadeDb.createDatabase();

    // Create REAL production adapter (not a mock!)
    ArcadeContentRepository arcadeRepo = new ArcadeContentRepository(database);
    repository = new ContentPersistenceAdapter(arcadeRepo);
  }

  /**
   * Cleans up database and closes connection after each test.
   * Ensures test isolation by removing all test data.
   */
  @AfterEach
  void tearDownDatabase() {
    // Clean up test data
    arcadeDb.cleanDatabase(database);

    // Close connection
    if (database != null) {
      database.close();
    }
  }

  /**
   * Creates (idempotently) a User and a Link, and an {@code OwnsLink} edge from the user to the
   * link, so that ownership-scoped queries (e.g. vector search) can find content under that link.
   * Mirrors the production {@code OwnsLink} direction (User -&gt; Link).
   */
  protected void giveUserOwnershipOfLink(String userId, String linkId) {
    database.transaction(() -> {
      boolean userExists = database.query("sql", "SELECT FROM User WHERE id = ?", userId).stream().findFirst().isPresent();
      if (!userExists) {
        database.command(
          "sql",
          "INSERT INTO User SET id = ?, username = ?, email = ?, passwordHash = 'test-hash', salt = 'test-salt', createdAt = sysdate(), isActive = true",
          userId,
          "user-" + userId,
          userId + "@test.local"
        );
      }
      database.command("sql", "INSERT INTO Link SET id = ?, url = ?", linkId, "https://test.local/" + linkId);
      database.command(
        "sql",
        "CREATE EDGE OwnsLink FROM (SELECT FROM User WHERE id = ?) TO (SELECT FROM Link WHERE id = ?) SET createdAt = sysdate(), accessLevel = 'OWNER'",
        userId,
        linkId
      );
    });
  }
}
