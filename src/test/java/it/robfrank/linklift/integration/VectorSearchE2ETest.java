package it.robfrank.linklift.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.arcadedb.remote.RemoteDatabase;
import it.robfrank.linklift.adapter.out.ai.OllamaEmbeddingAdapter;
import it.robfrank.linklift.adapter.out.persistence.ArcadeContentRepository;
import it.robfrank.linklift.adapter.out.persistence.ContentPersistenceAdapter;
import it.robfrank.linklift.application.domain.model.Content;
import it.robfrank.linklift.application.domain.model.DownloadStatus;
import it.robfrank.linklift.application.domain.service.BackfillEmbeddingsService;
import it.robfrank.linklift.application.domain.service.SearchContentService;
import it.robfrank.linklift.testcontainers.ArcadeDbContainer;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * OPTIONAL End-to-End tests for vector search with REAL Ollama embeddings.
 * <p>
 * These tests validate the complete vector search workflow with actual semantic similarity
 * from Ollama's all-minilm:l6-v2 model (384 dimensions).
 * <p>
 * WARNING:
 * - Slow (~30-60s per test due to model loading)
 * - Large (~400MB Ollama image + 23MB model download)
 * - Optional (most testing covered by integration tests with fake embeddings)
 * <p>
 * Run with: mvn test -Pe2e-tests
 */
@Testcontainers
class VectorSearchE2ETest {

  private static final Logger logger = LoggerFactory.getLogger(VectorSearchE2ETest.class);
  private static final LocalDateTime FIXED_TEST_TIME = LocalDateTime.of(2024, 1, 1, 12, 0);
  private static final String OLLAMA_MODEL = "all-minilm:l6-v2";

  @Container
  private static final ArcadeDbContainer arcadeDb = new ArcadeDbContainer();

  @Container
  private static final GenericContainer<?> ollama = new GenericContainer<>(DockerImageName.parse("ollama/ollama:latest"))
    .withExposedPorts(11434)
    .waitingFor(Wait.forHttp("/").forPort(11434).forStatusCode(200));

  private RemoteDatabase database;
  private ContentPersistenceAdapter repository;
  private OllamaEmbeddingAdapter embeddingAdapter;
  private ExecutorService executorService;
  private BackfillEmbeddingsService backfillService;
  private SearchContentService searchService;

  @BeforeAll
  static void setUpOllama() throws Exception {
    logger.info("Pulling Ollama model '{}' - this may take several minutes on first run...", OLLAMA_MODEL);

    var result = ollama.execInContainer("ollama", "pull", OLLAMA_MODEL);

    if (result.getExitCode() != 0) {
      throw new RuntimeException("Failed to pull Ollama model: " + result.getStderr());
    }

    logger.info("Ollama model '{}' ready", OLLAMA_MODEL);
  }

  @BeforeEach
  void setUp() {
    // Set up database
    database = arcadeDb.createDatabase();
    ArcadeContentRepository arcadeRepo = new ArcadeContentRepository(database);
    repository = new ContentPersistenceAdapter(arcadeRepo);

    // Set up REAL Ollama embedding adapter
    String ollamaUrl = String.format("http://%s:%d", ollama.getHost(), ollama.getMappedPort(11434));

    embeddingAdapter = new OllamaEmbeddingAdapter(HttpClient.newHttpClient(), ollamaUrl, OLLAMA_MODEL);

    // Set up services with REAL embeddings
    executorService = Executors.newFixedThreadPool(2);
    backfillService = new BackfillEmbeddingsService(repository, repository, embeddingAdapter, executorService);

    searchService = new SearchContentService(repository, embeddingAdapter);
  }

  @AfterEach
  void tearDown() {
    // Clean up database
    arcadeDb.cleanDatabase(database);
    if (database != null) {
      database.close();
    }

    // Shutdown executor service
    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    } catch (InterruptedException e) {
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Test: endToEnd_realEmbeddings_shouldFindSimilarContent
   * <p>
   * Validates actual semantic similarity using real Ollama embeddings.
   * Saves AI-related content and cooking-related content, then searches for AI topics.
   * Should return AI content with high similarity, not cooking content.
   */
  @Test
  void endToEnd_realEmbeddings_shouldFindSimilarContent() {
    // Given - AI-related content
    Content aiContent1 = createTestContent(
      "ai-1",
      "link-ai-1",
      "Artificial intelligence and machine learning are transforming software development. " +
      "Deep learning models can now generate code and solve complex problems."
    );

    Content aiContent2 = createTestContent(
      "ai-2",
      "link-ai-2",
      "Neural networks and AI algorithms are revolutionizing data science. " + "Large language models demonstrate impressive reasoning capabilities."
    );

    // And - unrelated cooking content
    Content cookingContent = createTestContent(
      "cooking-1",
      "link-cooking-1",
      "Italian pasta recipes are delicious and easy to make. " + "Fresh tomatoes and basil create amazing flavors in traditional dishes."
    );

    repository.saveContent(aiContent1);
    repository.saveContent(aiContent2);
    repository.saveContent(cookingContent);

    // When - backfill generates REAL embeddings
    backfillService.backfill();

    // Wait for async backfill to complete
    await()
      .atMost(Duration.ofSeconds(60))
      .pollInterval(Duration.ofSeconds(2))
      .untilAsserted(() -> {
        Content updated1 = repository.findContentById("ai-1").orElseThrow();
        Content updated2 = repository.findContentById("ai-2").orElseThrow();
        Content updated3 = repository.findContentById("cooking-1").orElseThrow();

        assertThat(updated1.embedding()).isNotNull();
        assertThat(updated2.embedding()).isNotNull();
        assertThat(updated3.embedding()).isNotNull();
      });

    // And - search for AI-related content using REAL semantic similarity
    List<Content> results = searchService.search("artificial intelligence and AI", 10);

    // Then - should return AI content, not cooking content
    assertThat(results).isNotEmpty();
    assertThat(results.size()).isGreaterThanOrEqualTo(2);

    // Verify AI content is in top results
    List<String> resultIds = results.stream().map(Content::id).toList();
    assertThat(resultIds).contains("ai-1", "ai-2");

    // Verify cooking content is not in top results (or ranked much lower)
    if (resultIds.contains("cooking-1")) {
      int cookingIndex = resultIds.indexOf("cooking-1");
      assertThat(cookingIndex).as("Cooking content should rank lower than AI content").isGreaterThan(1);
    }

    logger.info("Search results for 'artificial intelligence and AI': {}", results.stream().map(Content::id).toList());
  }

  /**
   * Test: endToEnd_realEmbeddings_shouldValidateDimensions
   * <p>
   * Validates that real Ollama embeddings have correct dimensions.
   * This catches dimension mismatches between the model and database schema.
   */
  @Test
  void endToEnd_realEmbeddings_shouldValidateDimensions() {
    // Given - content exists
    Content content = createTestContent("test-1", "link-test-1", "This is a test content for validating embedding dimensions from real Ollama model.");

    repository.saveContent(content);

    // When - backfill generates REAL embeddings
    backfillService.backfill();

    // Wait for async backfill to complete
    await()
      .atMost(Duration.ofSeconds(60))
      .pollInterval(Duration.ofSeconds(2))
      .untilAsserted(() -> {
        Content updated = repository.findContentById("test-1").orElseThrow();
        assertThat(updated.embedding()).isNotNull();
      });

    // Then - embedding should have correct dimensions (384 for all-minilm:l6-v2)
    Content updated = repository.findContentById("test-1").orElseThrow();
    float[] embedding = updated.embedding();

    assertThat(embedding).isNotNull();
    assertThat(embedding.length).as("all-minilm:l6-v2 model should produce 384-dimensional embeddings").isEqualTo(384);

    // Verify all values are valid floats (not NaN, not Infinity)
    for (float value : embedding) {
      assertThat(Float.isFinite(value)).as("Embedding values should be finite").isTrue();
    }

    logger.info("Successfully validated embedding dimensions: {} dimensions", embedding.length);
  }

  private static Content createTestContent(String id, String linkId, String textContent) {
    return new Content(
      id,
      linkId,
      "html",
      textContent,
      textContent.length(),
      FIXED_TEST_TIME,
      "text/html",
      DownloadStatus.COMPLETED,
      null,
      null,
      null,
      null,
      null,
      null,
      null
    );
  }
}
