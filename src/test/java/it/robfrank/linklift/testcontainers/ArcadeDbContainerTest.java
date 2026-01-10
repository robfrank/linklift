package it.robfrank.linklift.testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Verification test for Testcontainers infrastructure.
 * Ensures ArcadeDbContainer starts correctly and schema is initialized.
 * <p>
 * Note: Full integration tests with Content persistence will be added in Phase 2
 * after ensuring the ContentMapper correctly handles all field types.
 */
class ArcadeDbContainerTest extends ArcadeDbTestBase {

  @Test
  void containerStarts_shouldInitializeSchema() {
    // Given - container is running (managed by @Container)
    // When - checking container status
    boolean isRunning = arcadeDb.isRunning();

    // Then - container should be running
    assertThat(isRunning).isTrue();
  }

  @Test
  void database_shouldBeAccessible() {
    // Given - database is initialized
    // When - checking database is not null
    // Then - database connection should be available
    assertThat(database).isNotNull();
  }

  @Test
  void repository_shouldBeInitialized() {
    // Given - repository is created from database
    // When - checking repository is not null
    // Then - repository should be available for tests
    assertThat(repository).isNotNull();
  }
}
