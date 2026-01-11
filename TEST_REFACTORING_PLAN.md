# Test Refactoring Plan: From Mocks to Stubs/Integration Tests

## Executive Summary

Refactor vector search tests to replace Mockito mocks with:

1. **Real production implementations with Testcontainers** for database operations
2. **Fake implementations** for external services (e.g., FakeEmbeddingGenerator)
3. **Real HTTP clients with WireMock** for HTTP adapter testing

**Decision:** Use Testcontainers + real production code instead of in-memory stubs for better coverage and lower maintenance (see PHASE1_COMPARISON.md for analysis)

---

## Decision Summary: Testcontainers Approach

After analyzing both approaches (see `PHASE1_COMPARISON.md`), we chose **Testcontainers + Real Production Code** for Phase 1:

### Why Testcontainers?

1. ✅ **Lower implementation cost**: 5 hours vs 10 hours for in-memory stubs
2. ✅ **Better bug detection**: Catches SQL errors, schema issues, vector index config problems
3. ✅ **Less maintenance**: ~50 lines of container code vs ~200 lines of stub code
4. ✅ **Reuses production code**: Tests use actual `ContentPersistenceAdapter`, `ArcadeContentRepository`, `ContentMapper`
5. ✅ **Higher confidence**: Tests validate real database behavior, not stub approximations

### What We're Testing

- **Phase 1+2**: Service tests with Testcontainers + ArcadeDB (32 tests, ~30s)
  - `BackfillEmbeddingsServiceTest` - real database operations
  - `SearchContentServiceTest` - real vector search with LSM_TREE index
- **Phase 4**: HTTP adapter tests with WireMock (15 tests, ~500ms)
  - `OllamaEmbeddingAdapterTest` - real HTTP client, mocked responses
- **Phase 3** (optional): E2E tests with real Ollama (2 tests, ~120s)
  - Full system validation with actual embeddings

### Trade-offs Accepted

- ⚠️ **Slower tests**: 30s vs 2s for mocks (acceptable for quality gain)
- ⚠️ **Docker required**: Standard tooling in 2024
- ⚠️ **Container startup overhead**: Mitigated by sharing single container

---

## Goals

- ✅ Improve test readability and expressiveness
- ✅ Reduce coupling to internal implementation details
- ✅ Catch more integration issues earlier
- ✅ Make tests more maintainable (less brittle to refactoring)
- ✅ Ensure tests validate actual behavior, not just mock interactions

---

## Current State Analysis

### BackfillEmbeddingsServiceTest (11 tests)

**Current approach:** Mockito mocks for all dependencies

- `LoadContentPort` (mocked)
- `SaveContentPort` (mocked)
- `EmbeddingGenerator` (mocked)

**Problems:**

- Tests verify mock interactions rather than actual behavior
- Brittle - breaks when method signatures change
- Doesn't validate actual data flow
- Thread.sleep() for async verification is non-deterministic

### SearchContentServiceTest (21 tests)

**Current approach:** Mockito mocks for all dependencies

- `LoadContentPort` (mocked)
- `EmbeddingGenerator` (mocked)

**Problems:**

- Over-mocking simple query operations
- Tests don't validate actual search behavior
- No verification of vector similarity calculations

### OllamaEmbeddingAdapterTest (15 tests)

**Current approach:** Mockito mocks for HttpClient

**Problems:**

- Doesn't test actual HTTP interaction
- Mock setup is complex and error-prone
- No validation of real JSON serialization/deserialization

---

## Refactoring Strategy

### Phase 1: Setup Testcontainers Infrastructure (Priority: HIGH)

**Rationale:** Use real production implementations (`ContentPersistenceAdapter`, `ArcadeContentRepository`) with Testcontainers instead of in-memory stubs for:

- ✅ Lower implementation cost (5h vs 10h)
- ✅ Better bug detection (SQL errors, schema issues, vector index config)
- ✅ Less code to maintain (reuses production code)
- ✅ Higher confidence in test results

See `PHASE1_COMPARISON.md` for detailed analysis.

#### 1.1 Create ArcadeDB Testcontainer

Create a reusable Testcontainer for ArcadeDB that handles database lifecycle and schema initialization.

**File:** `src/test/java/it/robfrank/linklift/testcontainers/ArcadeDbContainer.java`

```java
package it.robfrank.linklift.testcontainers;

import com.arcadedb.remote.RemoteDatabase;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class ArcadeDbContainer extends GenericContainer<ArcadeDbContainer> {

  private static final String IMAGE = "arcadedata/arcadedb:23.11.1";
  private static final int ARCADE_PORT = 2480;
  private static final String DEFAULT_DATABASE = "linklift";

  public ArcadeDbContainer() {
    super(IMAGE);
    withExposedPorts(ARCADE_PORT);
    withEnv("ARCADEDB_ROOT_PASSWORD", "test");
    waitingFor(Wait.forHttp("/api/v1/server").forPort(ARCADE_PORT).withBasicCredentials("root", "test"));
  }

  public String getHttpUrl() {
    return "http://" + getHost() + ":" + getMappedPort(ARCADE_PORT);
  }

  public String getRootPassword() {
    return "test";
  }

  public String getDatabaseName() {
    return DEFAULT_DATABASE;
  }

  /**
   * Creates and initializes a RemoteDatabase connection with vector search schema.
   * This should be called in @BeforeEach to get a fresh database connection.
   */
  public RemoteDatabase createDatabase() {
    RemoteDatabase db = new RemoteDatabase(getHttpUrl(), getDatabaseName(), "root", getRootPassword());

    // Initialize schema with vector index
    db.transaction(() -> {
      // Create Content vertex type
      db.command("sql", "CREATE VERTEX TYPE Content IF NOT EXISTS");

      // Create properties
      db.command("sql", "CREATE PROPERTY Content.id STRING IF NOT EXISTS");
      db.command("sql", "CREATE PROPERTY Content.linkId STRING IF NOT EXISTS");
      db.command("sql", "CREATE PROPERTY Content.textContent STRING IF NOT EXISTS");
      db.command("sql", "CREATE PROPERTY Content.embedding ARRAY IF NOT EXISTS");

      // Create vector index for similarity search
      db.command(
        "sql",
        """
            CREATE VECTOR INDEX Content[embedding]
            IF NOT EXISTS LSM_TREE METRIC COSINE DIMENSIONS 384
        """
      );

      // Create regular index on id
      db.command("sql", "CREATE INDEX Content.id IF NOT EXISTS UNIQUE");
    });

    return db;
  }

  /**
   * Cleans up all content from the database.
   * Call this in @AfterEach to ensure test isolation.
   */
  public void cleanDatabase(RemoteDatabase db) {
    db.transaction(() -> {
      db.command("sql", "DELETE VERTEX Content");
    });
  }
}

```

**Benefits:**

- Encapsulates ArcadeDB setup and schema initialization
- Reusable across all test classes
- Ensures vector index is properly configured
- Handles database cleanup for test isolation

#### 1.2 Create Base Test Class for Database Tests

Create an abstract base class that manages the Testcontainer lifecycle.

**File:** `src/test/java/it/robfrank/linklift/testcontainers/ArcadeDbTestBase.java`

```java
package it.robfrank.linklift.testcontainers;

import com.arcadedb.remote.RemoteDatabase;
import it.robfrank.linklift.adapter.out.persistence.ArcadeContentRepository;
import it.robfrank.linklift.adapter.out.persistence.ContentPersistenceAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class ArcadeDbTestBase {

  @Container
  protected static final ArcadeDbContainer arcadeDb = new ArcadeDbContainer();

  protected RemoteDatabase database;
  protected ContentPersistenceAdapter repository;

  @BeforeEach
  void setUpDatabase() {
    // Create fresh database connection and initialize schema
    database = arcadeDb.createDatabase();

    // Create REAL production adapter (not a mock!)
    ArcadeContentRepository arcadeRepo = new ArcadeContentRepository(database);
    repository = new ContentPersistenceAdapter(arcadeRepo);
  }

  @AfterEach
  void tearDownDatabase() {
    // Clean up test data
    arcadeDb.cleanDatabase(database);

    // Close connection
    if (database != null) {
      database.close();
    }
  }
}

```

**Benefits:**

- Single container shared across all tests (faster execution)
- Automatic database cleanup between tests
- Uses REAL production `ContentPersistenceAdapter` and `ArcadeContentRepository`
- Tests the actual SQL queries, mapping, and vector indexing

#### 1.3 Create FakeEmbeddingGenerator

Replace mocked `EmbeddingGenerator` with a deterministic fake implementation. This is still needed since we don't want tests calling real Ollama service.

**File:** `src/test/java/it/robfrank/linklift/adapter/out/ai/FakeEmbeddingGenerator.java`

```java
public class FakeEmbeddingGenerator implements EmbeddingGenerator {

  private final Map<String, List<Float>> embeddingCache = new HashMap<>();
  private final Function<String, List<Float>> embeddingFunction;
  private RuntimeException exceptionToThrow = null;

  // Simple constructor for deterministic embeddings
  public FakeEmbeddingGenerator() {
    this(text -> {
      // Generate deterministic embedding based on text hash
      int hash = text.hashCode();
      List<Float> embedding = new ArrayList<>();
      for (int i = 0; i < 384; i++) {
        embedding.add((float) ((hash + i) % 1000) / 1000.0f);
      }
      return embedding;
    });
  }

  // Constructor with custom embedding function
  public FakeEmbeddingGenerator(Function<String, List<Float>> embeddingFunction) {
    this.embeddingFunction = embeddingFunction;
  }

  @Override
  public List<Float> generateEmbedding(String text) {
    if (exceptionToThrow != null) {
      RuntimeException ex = exceptionToThrow;
      exceptionToThrow = null; // Only throw once
      throw ex;
    }

    return embeddingCache.computeIfAbsent(text, embeddingFunction);
  }

  // Test helpers
  public void throwOnNextCall(RuntimeException exception) {
    this.exceptionToThrow = exception;
  }

  public void clearCache() {
    embeddingCache.clear();
  }

  public int getCacheSize() {
    return embeddingCache.size();
  }
}

```

**Benefits:**

- Deterministic embeddings (same text always produces same embedding)
- Tests can verify embedding caching behavior
- Can inject failures for error testing
- No mock setup/verification code

---

### Phase 2: Refactor Service Tests to Use Testcontainers (Priority: HIGH)

#### 2.1 Refactor BackfillEmbeddingsServiceTest with Testcontainers

**Before (with mocks):**

```java
@ExtendWith(MockitoExtension.class)
class BackfillEmbeddingsServiceTest {

  @Mock
  private LoadContentPort loadContentPort;

  @Mock
  private SaveContentPort saveContentPort;

  @Mock
  private EmbeddingGenerator embeddingGenerator;

  @Test
  void backfill_shouldProcessSingleBatch_whenContentExists() throws InterruptedException {
    Content content = createTestContent("id-1", "link-1", "text content");
    List<Float> embedding = List.of(0.1f, 0.2f, 0.3f);

    when(loadContentPort.findContentsWithoutEmbeddings(100)).thenReturn(List.of(content)).thenReturn(List.of());
    when(embeddingGenerator.generateEmbedding("text content")).thenReturn(embedding);

    backfillEmbeddingsService.backfill();
    Thread.sleep(1000);

    verify(loadContentPort, times(2)).findContentsWithoutEmbeddings(100);
    verify(embeddingGenerator, times(1)).generateEmbedding("text content");
    verify(saveContentPort, times(1)).updateContent(argThat(c -> c.embedding() != null));
  }
}

```

**After (with Testcontainers and real implementations):**

```java
class BackfillEmbeddingsServiceTest extends ArcadeDbTestBase {

  private FakeEmbeddingGenerator embeddingGenerator;
  private BackfillEmbeddingsService backfillEmbeddingsService;
  private ExecutorService executorService;

  @BeforeEach
  void setUp() {
    super.setUpDatabase(); // Creates real database and repository

    embeddingGenerator = new FakeEmbeddingGenerator();
    executorService = Executors.newFixedThreadPool(2);
    backfillEmbeddingsService = new BackfillEmbeddingsService(
      repository, // Real ContentPersistenceAdapter
      repository, // LoadContentPort and SaveContentPort
      embeddingGenerator,
      executorService
    );
  }

  @Test
  void backfill_shouldProcessSingleBatch_whenContentExists() throws InterruptedException {
    // Given - content without embedding in REAL database
    Content content = createTestContent("id-1", "link-1", "text content");
    repository.saveContent(content);

    // When - backfill runs
    backfillEmbeddingsService.backfill();
    Thread.sleep(1000);

    // Then - content has embedding in database
    Content updated = repository.findContentById("id-1").orElseThrow();
    assertThat(updated.embedding()).isNotNull();
    assertThat(updated.embedding()).hasSize(384);

    // And - no more content without embeddings
    List<Content> remaining = repository.findContentsWithoutEmbeddings(100);
    assertThat(remaining).isEmpty();
  }

  @AfterEach
  void tearDown() {
    super.tearDownDatabase(); // Cleans database
    executorService.shutdown();
  }
}

```

**Key Improvements:**

- Uses REAL `ContentPersistenceAdapter` and `ArcadeContentRepository` (not mocks!)
- Tests against REAL ArcadeDB with actual vector index
- Validates actual database state, not mock interactions
- Catches SQL errors, schema issues, and mapping bugs
- No `verify()` statements - tests behavior, not interactions
- Tests read like specifications (Given/When/Then)

#### 2.2 Refactor SearchContentServiceTest with Testcontainers

**Before (with mocks):**

```java
@ExtendWith(MockitoExtension.class)
class SearchContentServiceTest {

  @Mock
  private LoadContentPort loadContentPort;

  @Mock
  private EmbeddingGenerator embeddingGenerator;

  @Test
  void search_shouldReturnResults_whenValidQueryProvided() {
    String query = "test query";
    List<Float> queryVector = List.of(0.1f, 0.2f, 0.3f);
    Content resultContent = createTestContent("id-1", "link-1");

    when(embeddingGenerator.generateEmbedding(query)).thenReturn(queryVector);
    when(loadContentPort.findSimilar(queryVector, 10)).thenReturn(List.of(resultContent));

    List<Content> results = searchContentService.search(query, 10);

    assertThat(results).containsExactly(resultContent);
    verify(embeddingGenerator, times(1)).generateEmbedding(query);
    verify(loadContentPort, times(1)).findSimilar(queryVector, 10);
  }
}

```

**After (with Testcontainers and real implementations):**

```java
class SearchContentServiceTest extends ArcadeDbTestBase {

  private FakeEmbeddingGenerator embeddingGenerator;
  private SearchContentService searchContentService;

  @BeforeEach
  void setUp() {
    super.setUpDatabase(); // Creates real database and repository

    embeddingGenerator = new FakeEmbeddingGenerator();
    searchContentService = new SearchContentService(
      repository, // Real ContentPersistenceAdapter
      embeddingGenerator
    );
  }

  @Test
  void search_shouldReturnSimilarContent_usingRealVectorIndex() {
    // Given - content in REAL database with actual embeddings
    Content ml1 = createTestContent("id-1", "link-1", "machine learning tutorial");
    Content ml2 = createTestContent("id-2", "link-2", "deep learning guide");
    Content cooking = createTestContent("id-3", "link-3", "pasta cooking recipes");

    // Generate real embeddings using FakeEmbeddingGenerator
    ml1 = ml1.withEmbedding(embeddingGenerator.generateEmbedding(ml1.textContent()));
    ml2 = ml2.withEmbedding(embeddingGenerator.generateEmbedding(ml2.textContent()));
    cooking = cooking.withEmbedding(embeddingGenerator.generateEmbedding(cooking.textContent()));

    // Save to REAL database with vector index
    repository.saveContent(ml1);
    repository.saveContent(ml2);
    repository.saveContent(cooking);

    // When - searching using REAL ArcadeDB vector search
    List<Content> results = searchContentService.search("machine learning basics", 2);

    // Then - ArcadeDB's LSM_TREE index returns most similar content
    assertThat(results).hasSize(2);
    assertThat(results).extracting(Content::id).containsExactlyInAnyOrder("id-1", "id-2").doesNotContain("id-3");
  }
}

```

**Key Improvements:**

- Tests REAL ArcadeDB vector search with LSM_TREE index
- Validates actual cosine similarity ranking
- Uses real SQL: `SELECT FROM Content WHERE embedding VECTOR KNN [?, ?]`
- Catches vector index configuration errors
- Tests actual embedding similarity, not mocked results
- More confident that search actually works in production

---

### Phase 3: End-to-End Integration Tests (Priority: MEDIUM)

Create comprehensive E2E tests that validate the entire vector search workflow with real external services.

#### 3.1 Create Full-Stack Vector Search Integration Test

**Purpose:** Optional E2E tests with real Ollama service for full system validation.

**Note:** Phase 1 and 2 already provide integration testing with Testcontainers and real ArcadeDB. Phase 3 adds OPTIONAL E2E tests with real Ollama for complete system validation, but these are slower and require Ollama container.

**File:** `src/test/java/it/robfrank/linklift/integration/VectorSearchE2ETest.java`

```java
@Testcontainers
class VectorSearchE2ETest {

  @Container
  static ArcadeDbContainer arcadeDb = new ArcadeDbContainer();

  @Container
  static GenericContainer<?> ollama = new GenericContainer<>("ollama/ollama:latest")
    .withExposedPorts(11434)
    .withCommand("serve")
    .waitingFor(Wait.forHttp("/api/tags").forPort(11434));

  private ContentPersistenceAdapter repository;
  private OllamaEmbeddingAdapter embeddingAdapter;
  private BackfillEmbeddingsService backfillService;
  private SearchContentService searchService;
  private RemoteDatabase database;

  @BeforeAll
  static void pullModel() {
    // Pull embedding model into Ollama container
    // This is expensive - only do it once
    String ollamaUrl = "http://" + ollama.getHost() + ":" + ollama.getMappedPort(11434);
    // Run: curl $ollamaUrl/api/pull -d '{"name":"nomic-embed-text"}'
  }

  @BeforeEach
  void setUp() {
    database = arcadeDb.createDatabase();
    ArcadeContentRepository arcadeRepo = new ArcadeContentRepository(database);
    repository = new ContentPersistenceAdapter(arcadeRepo);

    // REAL OllamaEmbeddingAdapter (not fake!)
    String ollamaUrl = "http://" + ollama.getHost() + ":" + ollama.getMappedPort(11434);
    HttpClient httpClient = HttpClient.newHttpClient();
    embeddingAdapter = new OllamaEmbeddingAdapter(httpClient, ollamaUrl, "nomic-embed-text");

    ExecutorService executor = Executors.newFixedThreadPool(2);
    backfillService = new BackfillEmbeddingsService(repository, repository, embeddingAdapter, executor);
    searchService = new SearchContentService(repository, embeddingAdapter);
  }

  @Test
  void endToEnd_realEmbeddings_shouldFindSimilarContent() {
    // Given - content saved with REAL Ollama embeddings
    Content ai1 = createTestContent("id-1", "link-1", "Machine learning is a subset of artificial intelligence");
    Content ai2 = createTestContent("id-2", "link-2", "Neural networks power deep learning systems");
    Content cooking = createTestContent("id-3", "link-3", "Pasta recipes require fresh ingredients");

    repository.saveContent(ai1);
    repository.saveContent(ai2);
    repository.saveContent(cooking);

    // When - backfill generates REAL embeddings via Ollama
    backfillService.backfill();

    // Wait for completion
    await().atMost(60, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> repository.findContentsWithoutEmbeddings(100).isEmpty());

    // When - searching with REAL query embedding
    List<Content> results = searchService.search("artificial intelligence and AI", 2);

    // Then - REAL vector search returns AI content, not cooking
    assertThat(results).hasSize(2);
    assertThat(results).extracting(Content::id).containsExactlyInAnyOrder("id-1", "id-2");
  }

  @Test
  void endToEnd_realEmbeddings_shouldValidateDimensions() {
    // Given - content
    Content content = createTestContent("id-1", "link-1", "Test content");
    repository.saveContent(content);

    // When - backfill with REAL Ollama
    backfillService.backfill();

    await().atMost(30, TimeUnit.SECONDS).until(() -> repository.findContentById("id-1").map(c -> c.embedding() != null).orElse(false));

    // Then - REAL embedding has correct dimensions
    Content retrieved = repository.findContentById("id-1").orElseThrow();
    assertThat(retrieved.embedding()).hasSize(384); // Configured dimension size (ArcadeDbContainer uses 384)
  }

  @AfterEach
  void tearDown() {
    arcadeDb.cleanDatabase(database);
    database.close();
  }
}

```

**Benefits:**

- Tests with REAL Ollama embeddings (not fake)
- Validates actual semantic similarity
- Catches dimension mismatches with real model
- Full system validation

**Tradeoffs:**

- Slow (~60s per test due to Ollama model loading)
- Requires Docker and pulls ~400MB Ollama image
- Use sparingly - most testing covered by Phase 1/2

---

### Phase 4: HTTP Integration Tests with WireMock (Priority: MEDIUM)

#### 4.1 Refactor OllamaEmbeddingAdapterTest

**Before (with HttpClient mock):**

```java
@Test
void generateEmbedding_shouldReturnEmbedding_whenValidResponseReceived() throws Exception {
  adapter = new OllamaEmbeddingAdapter(httpClient, "http://localhost:11434", "test-model");

  String responseJson = "{\"embedding\": [0.1, 0.2, 0.3, 0.4]}";
  HttpResponse response = mockHttpResponse(200, responseJson);
  when(httpClient.send(any(HttpRequest.class), any())).thenReturn(response);

  List<Float> embedding = adapter.generateEmbedding("test text");

  assertThat(embedding).hasSize(4).containsExactly(0.1f, 0.2f, 0.3f, 0.4f);
}

```

**After (with WireMock):**

```java
@ExtendWith(MockitoExtension.class)
class OllamaEmbeddingAdapterTest {

  @RegisterExtension
  static WireMockExtension wireMock = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private OllamaEmbeddingAdapter adapter;
  private HttpClient httpClient;

  @BeforeEach
  void setUp() {
    httpClient = HttpClient.newHttpClient();
    String baseUrl = "http://localhost:" + wireMock.getPort();
    adapter = new OllamaEmbeddingAdapter(httpClient, baseUrl, "test-model");
  }

  @Test
  void generateEmbedding_shouldReturnEmbedding_whenValidResponseReceived() {
    // Given - Ollama API returns valid embedding
    wireMock.stubFor(
      post("/api/embeddings")
        .withRequestBody(matchingJsonPath("$.model", equalTo("test-model")))
        .withRequestBody(matchingJsonPath("$.prompt", equalTo("test text")))
        .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("{\"embedding\": [0.1, 0.2, 0.3, 0.4]}"))
    );

    // When - generating embedding
    List<Float> embedding = adapter.generateEmbedding("test text");

    // Then - embedding parsed correctly
    assertThat(embedding).containsExactly(0.1f, 0.2f, 0.3f, 0.4f);

    // And - correct request sent
    wireMock.verify(postRequestedFor(urlEqualTo("/api/embeddings")).withHeader("Content-Type", equalTo("application/json")));
  }

  @Test
  void generateEmbedding_shouldHandleActualJsonSerialization() {
    // Given - real JSON request/response
    wireMock.stubFor(post("/api/embeddings").willReturn(aResponse().withStatus(200).withBodyFile("ollama-embedding-response.json")));

    // When - generating embedding with special characters
    String textWithSpecialChars = "Test with \"quotes\" and newlines\n\ttabs";
    List<Float> embedding = adapter.generateEmbedding(textWithSpecialChars);

    // Then - request properly serialized
    wireMock.verify(postRequestedFor(urlEqualTo("/api/embeddings")).withRequestBody(matchingJsonPath("$.prompt", containing("quotes"))));

    assertThat(embedding).isNotEmpty();
  }
}

```

**Benefits:**

- Tests real HTTP communication
- Tests actual JSON serialization/deserialization
- Can use response files for complex scenarios
- Validates request format
- No mocking of HttpResponse internals

---

## Implementation Roadmap

### Week 1: Testcontainers Infrastructure (Phase 1)

- [ ] Add Testcontainers dependency to pom.xml
- [ ] Add WireMock dependency to pom.xml
- [ ] Add Awaitility dependency to pom.xml
- [ ] Create `ArcadeDbContainer` class
- [ ] Create `ArcadeDbTestBase` abstract class
- [ ] Create `FakeEmbeddingGenerator` class
- [ ] Set up test resource directory for WireMock response files
- [ ] Verify container starts and schema initializes correctly

### Week 2: Refactor Service Tests with Testcontainers (Phase 2)

- [ ] Refactor `BackfillEmbeddingsServiceTest` to extend `ArcadeDbTestBase`
- [ ] Replace mocks with real `ContentPersistenceAdapter`
- [ ] Refactor `SearchContentServiceTest` to extend `ArcadeDbTestBase`
- [ ] Update assertions to verify actual database state
- [ ] Verify all 32 service tests pass with Testcontainers
- [ ] Measure test execution time

### Week 3: Refactor HTTP Adapter Tests with WireMock (Phase 4)

- [ ] Refactor `OllamaEmbeddingAdapterTest` with WireMock
- [ ] Create response JSON files for edge cases
- [ ] Add tests for actual HTTP timeout behavior
- [ ] Add tests for connection errors and retry logic
- [ ] Verify all 15 HTTP tests pass
- [ ] Remove HttpClient mocks

### Week 4: Optional E2E Tests with Real Ollama (Phase 3)

- [ ] Create `VectorSearchE2ETest` with Ollama container
- [ ] Add end-to-end test with real embeddings
- [ ] Add dimension validation test
- [ ] Make E2E tests optional (Maven profile)
- [ ] Document Docker requirements
- [ ] Document how to run E2E tests locally

### Week 5: Cleanup & Documentation

- [ ] Remove unused Mockito mocks and verify statements
- [ ] Update test documentation with new approach
- [ ] Add README section explaining test types (unit with Testcontainers, HTTP with WireMock, E2E with Ollama)
- [ ] Create contributing guide for tests
- [ ] Measure final test coverage and execution times
- [ ] Final review and cleanup

---

## Dependencies to Add

### pom.xml additions:

```xml
<dependencies>
    <!-- Testcontainers -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>

    <!-- WireMock -->
    <dependency>
        <groupId>org.wiremock</groupId>
        <artifactId>wiremock</artifactId>
        <version>3.3.1</version>
        <scope>test</scope>
    </dependency>

    <!-- Awaitility for async testing (optional but recommended) -->
    <dependency>
        <groupId>org.awaitility</groupId>
        <artifactId>awaitility</artifactId>
        <version>4.2.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## Test Pyramid After Refactoring

```
                    /\
                   /  \
                  /    \
                 / E2E  \      2 tests (Ollama + ArcadeDB)
                /--------\     Optional, slow (~60s each)
               /          \
              /  HTTP Mock  \   15 tests (WireMock)
             /--------------\   Fast (~500ms total)
            /                \
           /  Integration DB  \  32 tests (Testcontainers + ArcadeDB)
          /--------------------\ Medium (~30s total)
```

**Distribution:**

- **Integration tests with Testcontainers + real DB**: 32 tests (BackfillEmbeddingsServiceTest, SearchContentServiceTest)
  - Uses REAL `ContentPersistenceAdapter`, `ArcadeContentRepository`, `ArcadeDB`
  - Tests actual SQL queries, vector indexing, and database behavior
  - Medium speed (~30 seconds total)
- **HTTP integration tests with WireMock**: 15 tests (OllamaEmbeddingAdapterTest)
  - Uses real `HttpClient` with mocked HTTP responses
  - Tests actual JSON serialization and HTTP handling
  - Fast (~500ms total)
- **E2E tests with Testcontainers + Ollama** (OPTIONAL): 2 tests (VectorSearchE2ETest)
  - Uses REAL Ollama embeddings + REAL ArcadeDB
  - Full system validation
  - Slow (~60 seconds each)
  - Run separately or in CI only

**Key Change:** Replaced in-memory stubs with Testcontainers + real production code for better bug detection and lower maintenance.

---

## Expected Benefits

### Maintainability

- ✅ **Reuses production code** - No need to maintain separate in-memory stubs
- ✅ **Tests survive refactoring** - Test behavior, not mock interactions
- ✅ **Easier to understand** - Given/When/Then with real database state
- ✅ **Less code to maintain** - ~50 lines of container setup vs ~200 lines of stub implementation

### Reliability

- ✅ **Catch real integration issues** - Tests against actual ArcadeDB with vector index
- ✅ **SQL validation** - Catches syntax errors, schema mismatches, mapping bugs
- ✅ **Schema validation** - Tests actual vector index configuration (LSM_TREE, COSINE, dimensions)
- ✅ **Deterministic embeddings** - FakeEmbeddingGenerator provides consistent test data
- ✅ **Better async handling** - Awaitility for robust async verification

### Expressiveness

- ✅ **Tests read like specifications** - Clear Given/When/Then structure
- ✅ **Real database state** - Assertions verify actual persisted data
- ✅ **No verify() statements** - State-based testing instead of interaction-based
- ✅ **Closer to production** - Tests use the same code paths as production

### Coverage

- ✅ **Real vector similarity** - Tests actual ArcadeDB LSM_TREE index and COSINE metric
- ✅ **Real SQL execution** - Validates `SELECT FROM Content WHERE embedding VECTOR KNN [?, ?]`
- ✅ **ContentMapper validation** - Tests actual domain-to-vertex mapping
- ✅ **Transaction behavior** - Tests real database transactions and concurrency

---

## Risks & Mitigations

### Risk 1: Slower Test Execution with Testcontainers

**Impact:** Testcontainers tests (~30s) are slower than mocks (~2s)

**Mitigation:**

- ✅ Acceptable tradeoff for better bug detection (see PHASE1_COMPARISON.md)
- Use single shared container across all tests (faster than per-test containers)
- Run E2E tests with Ollama separately (optional, CI-only)
- Consider parallel test execution for faster CI builds
- ~30s total for 32 integration tests is reasonable

### Risk 2: Docker Requirement for Development

**Impact:** Developers need Docker installed to run tests

**Mitigation:**

- Document Docker Desktop installation clearly
- Testcontainers is industry standard (2024)
- Provide troubleshooting guide for common Docker issues
- Add CI check to fail fast if Docker not available
- Alternative: Keep a few mock-based tests for quick feedback (optional)

### Risk 3: Container Startup Flakiness

**Impact:** Tests may fail intermittently due to container startup timing

**Mitigation:**

- Use Awaitility for robust async verification
- Configure generous container wait strategies
- Use `@Container` static field to share container across tests
- Add container health checks
- Monitor test stability in CI and adjust timeouts if needed

### Risk 4: Breaking Changes During Refactoring

**Impact:** Tests may fail when switching from mocks to Testcontainers

**Mitigation:**

- Refactor one test class at a time (BackfillEmbeddingsServiceTest first, then SearchContentServiceTest)
- Keep old tests temporarily until new tests are stable
- Run both old and new tests in parallel during transition
- Extensive review before deleting old mock-based tests
- Have rollback plan if issues arise

### Risk 5: Test Data Cleanup Issues

**Impact:** Tests may interfere with each other if database not cleaned properly

**Mitigation:**

- Use `@AfterEach` to clean database reliably
- ArcadeDbContainer provides `cleanDatabase()` helper
- Each test gets fresh data (no cross-test pollution)
- Use transactions with rollback if needed for faster cleanup

---

## Success Metrics

- [ ] **Code Coverage**: Maintain or increase from current 58% to at least 65%
- [ ] **Test Execution Time**:
  - Testcontainers integration tests: under 40 seconds total
  - WireMock HTTP tests: under 2 seconds total
  - E2E tests (optional): under 3 minutes total
- [ ] **Test Reliability**: Zero flaky tests in CI over 1 month
- [ ] **Code Quality**:
  - Reduced test changes when refactoring production code
  - Remove all Mockito `verify()` statements from service tests
  - All tests use Given/When/Then structure
- [ ] **Bug Detection**: Testcontainers catch at least 2 SQL/schema bugs that mocks would miss
- [ ] **Maintenance**: Fewer lines of test code (remove stub implementations, mock setup)

---

## References

- [Test Doubles (Mocks, Stubs, Fakes)](https://martinfowler.com/bliki/TestDouble.html)
- [Testcontainers Documentation](https://www.testcontainers.org/)
- [WireMock Documentation](https://wiremock.org/)
- [Testing Strategies for Vector Databases](https://www.pinecone.io/learn/testing-vector-databases/)
- [Don't Mock What You Don't Own](https://github.com/testdouble/contributing-tests/wiki/Don%27t-mock-what-you-don%27t-own)
