# Phase 1 Comparison: In-Memory Stub vs Testcontainers with Real Implementation

## Overview

Two approaches for replacing mocks in vector search tests:

1. **Approach A: In-Memory Stub** - Custom `InMemoryContentRepository` implementing the ports
2. **Approach B: Testcontainers** - Real `ContentPersistenceAdapter` + `ArcadeContentRepository` with ArcadeDB container

---

## Approach A: In-Memory Stub

### Implementation

```java
public class InMemoryContentRepository implements LoadContentPort, SaveContentPort {

  private final Map<String, Content> contents = new ConcurrentHashMap<>();
  private final Map<String, List<Float>> embeddings = new ConcurrentHashMap<>();

  @Override
  public List<Content> findContentsWithoutEmbeddings(int limit) {
    return contents.values().stream().filter(c -> c.embedding() == null).limit(limit).collect(Collectors.toList());
  }

  @Override
  public List<Content> findSimilar(List<Float> queryVector, int limit) {
    // Simple cosine similarity implementation
    return contents
      .values()
      .stream()
      .filter(c -> c.embedding() != null)
      .map(c -> new ScoredContent(c, cosineSimilarity(queryVector, c.embedding())))
      .sorted(Comparator.comparingDouble(ScoredContent::score).reversed())
      .limit(limit)
      .map(ScoredContent::content)
      .collect(Collectors.toList());
  }

  @Override
  public Content updateContent(Content content) {
    contents.put(content.id(), content);
    return content;
  }

  // Helper methods for test setup
  public void addContent(Content content) {
    contents.put(content.id(), content);
  }

  public void clear() {
    contents.clear();
  }

  private double cosineSimilarity(List<Float> a, List<Float> b) {
    double dotProduct = 0.0, normA = 0.0, normB = 0.0;
    for (int i = 0; i < a.size(); i++) {
      dotProduct += a.get(i) * b.get(i);
      normA += a.get(i) * a.get(i);
      normB += b.get(i) * b.get(i);
    }
    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
  }
}

```

### Usage in Tests

```java
@ExtendWith(MockitoExtension.class)
class BackfillEmbeddingsServiceTest {

  private InMemoryContentRepository repository;
  private FakeEmbeddingGenerator embeddingGenerator;
  private BackfillEmbeddingsService service;

  @BeforeEach
  void setUp() {
    repository = new InMemoryContentRepository();
    embeddingGenerator = new FakeEmbeddingGenerator();
    ExecutorService executor = Executors.newFixedThreadPool(2);
    service = new BackfillEmbeddingsService(
      repository, // Both LoadContentPort and SaveContentPort
      repository,
      embeddingGenerator,
      executor
    );
  }

  @Test
  void backfill_shouldAddEmbeddings_toContentWithoutThem() {
    // Given - content without embedding
    Content content = createContent("id-1", "Machine learning basics");
    repository.addContent(content);

    // When - backfill runs
    service.backfill();
    Thread.sleep(1000);

    // Then - content has embedding
    Content updated = repository.getById("id-1");
    assertThat(updated.embedding()).isNotNull().hasSize(384);
  }
}

```

---

## Approach B: Testcontainers with Real Implementation

### Implementation

```java
@Testcontainers
class BackfillEmbeddingsServiceTest {

  @Container
  static ArcadeDbContainer arcadeDb = new ArcadeDbContainer().withExposedPorts(2480).waitingFor(Wait.forHttp("/api/v1/server"));

  private ContentPersistenceAdapter repository;
  private FakeEmbeddingGenerator embeddingGenerator;
  private BackfillEmbeddingsService service;
  private RemoteDatabase database;

  @BeforeEach
  void setUp() {
    // Create real ArcadeDB connection
    database = new RemoteDatabase(arcadeDb.getHttpUrl(), "linklift", "root", arcadeDb.getRootPassword());

    // Initialize schema
    database.transaction(() -> {
      database.command(
        "sql",
        """
            CREATE VERTEX TYPE Content IF NOT EXISTS
        """
      );
      database.command(
        "sql",
        """
            CREATE PROPERTY Content.embedding ARRAY IF NOT EXISTS
        """
      );
      database.command(
        "sql",
        """
            CREATE VECTOR INDEX Content[embedding]
            IF NOT EXISTS LSM_TREE METRIC COSINE DIMENSIONS 384
        """
      );
    });

    // Use REAL production implementations
    ArcadeContentRepository arcadeRepo = new ArcadeContentRepository(database);
    repository = new ContentPersistenceAdapter(arcadeRepo);

    embeddingGenerator = new FakeEmbeddingGenerator();
    ExecutorService executor = Executors.newFixedThreadPool(2);
    service = new BackfillEmbeddingsService(repository, repository, embeddingGenerator, executor);
  }

  @AfterEach
  void tearDown() {
    // Clean up database
    database.transaction(() -> {
      database.command("sql", "DELETE VERTEX Content");
    });
    database.close();
  }

  @Test
  void backfill_shouldAddEmbeddings_toContentWithoutThem() {
    // Given - content without embedding in REAL database
    Content content = createContent("id-1", "Machine learning basics");
    repository.saveContent(content);

    // When - backfill runs
    service.backfill();
    Thread.sleep(1000);

    // Then - content has embedding in database
    Content updated = repository.findContentById("id-1").orElseThrow();
    assertThat(updated.embedding()).isNotNull().hasSize(384);
  }

  @Test
  void findSimilar_shouldUseArcadeVectorIndex() {
    // Given - multiple content items with embeddings
    Content ml1 = createContent("id-1", "Machine learning tutorial");
    Content ml2 = createContent("id-2", "Deep learning basics");
    Content cooking = createContent("id-3", "Pasta recipe");

    // Generate and save with embeddings
    ml1 = ml1.withEmbedding(embeddingGenerator.generateEmbedding(ml1.textContent()));
    ml2 = ml2.withEmbedding(embeddingGenerator.generateEmbedding(ml2.textContent()));
    cooking = cooking.withEmbedding(embeddingGenerator.generateEmbedding(cooking.textContent()));

    repository.saveContent(ml1);
    repository.saveContent(ml2);
    repository.saveContent(cooking);

    // When - searching with real ArcadeDB vector index
    List<Float> queryVector = embeddingGenerator.generateEmbedding("AI and machine learning");
    List<Content> results = repository.findSimilar(queryVector, 2);

    // Then - ArcadeDB's LSM_TREE vector index returns similar content
    assertThat(results).hasSize(2);
    assertThat(results).extracting(Content::id).containsExactlyInAnyOrder("id-1", "id-2");
  }
}

```

---

## Detailed Comparison

### 1. Test Execution Speed

| Aspect                         | Approach A (Stub) | Approach B (Testcontainers) |
| ------------------------------ | ----------------- | --------------------------- |
| **Test Suite Startup**         | ~0ms (instant)    | ~5-10s (container startup)  |
| **Single Test**                | ~50-100ms         | ~200-500ms                  |
| **Full Test Suite (47 tests)** | ~5-7 seconds      | ~20-30 seconds              |
| **Parallel Execution**         | ✅ Excellent      | ⚠️ Limited by container     |
| **CI Pipeline Time**           | ✅ Minimal impact | ⚠️ Adds 20-30s per build    |

**Winner: Approach A** - 4-6x faster

---

### 2. Test Coverage & Confidence

| What Gets Tested                | Approach A (Stub)        | Approach B (Testcontainers)       |
| ------------------------------- | ------------------------ | --------------------------------- |
| **Business Logic**              | ✅ Yes                   | ✅ Yes                            |
| **Port Contracts**              | ✅ Yes                   | ✅ Yes                            |
| **Vector Similarity Algorithm** | ⚠️ Custom implementation | ✅ **Real ArcadeDB LSM_TREE**     |
| **SQL Query Syntax**            | ❌ Not tested            | ✅ **Real queries validated**     |
| **Database Schema**             | ❌ Not tested            | ✅ **Schema evolution validated** |
| **Vector Index Configuration**  | ❌ Not tested            | ✅ **Index params validated**     |
| **Concurrency Issues**          | ⚠️ Limited               | ✅ **Real database locks**        |
| **Transaction Behavior**        | ❌ Not tested            | ✅ **Real transactions**          |
| **Mapping Logic**               | ❌ Not tested            | ✅ **ContentMapper validated**    |

**Winner: Approach B** - Significantly higher confidence

---

### 3. Implementation Complexity

#### Approach A - Stub Code to Maintain

```java
// Need to implement:
- InMemoryContentRepository (~150 lines)
  - findContentsWithoutEmbeddings logic
  - findSimilar with cosine similarity
  - updateContent state management
  - saveContent logic
  - findById/findByLinkId logic
  - Thread-safe concurrent access

// Need to maintain:
- Cosine similarity algorithm matches ArcadeDB's
- Filtering logic matches SQL queries
- Null handling matches database behavior
```

**Lines of Code:** ~200 (new test infrastructure)

#### Approach B - Container Setup

```java
// Need to implement:
- ArcadeDbContainer class (~50 lines)
  - Container configuration
  - Wait strategy
  - Schema initialization helper

// Already exists (0 new lines):
- ContentPersistenceAdapter ✅
- ArcadeContentRepository ✅
- ContentMapper ✅
```

**Lines of Code:** ~50 (reuses production code)

**Winner: Approach B** - Less code to maintain

---

### 4. Maintenance Burden

| Scenario                       | Approach A (Stub)              | Approach B (Testcontainers) |
| ------------------------------ | ------------------------------ | --------------------------- |
| **Add new port method**        | Must implement in stub         | Automatically supported     |
| **Change SQL query**           | ⚠️ Might need stub update      | ✅ Auto-validated           |
| **Update vector index config** | ❌ Stub doesn't know           | ✅ Test fails immediately   |
| **Refactor ContentMapper**     | ❌ Stub bypasses mapper        | ✅ Test fails immediately   |
| **Change database schema**     | ❌ Stub doesn't care           | ✅ Migration tested         |
| **Fix bug in repository**      | ⚠️ Might replicate bug in stub | ✅ Uses fixed code          |

**Winner: Approach B** - Lower maintenance, catches more issues

---

### 5. Test Isolation & Determinism

| Aspect                 | Approach A (Stub)      | Approach B (Testcontainers)  |
| ---------------------- | ---------------------- | ---------------------------- |
| **Test Isolation**     | ✅ Perfect (in-memory) | ✅ Good (per-test cleanup)   |
| **Determinism**        | ✅ Perfect             | ✅ Good (with fixed data)    |
| **Parallel Execution** | ✅ Excellent           | ⚠️ Needs separate containers |
| **Flakiness Risk**     | ✅ Very low            | ⚠️ Container startup issues  |

**Winner: Approach A** - Slightly more reliable

---

### 6. Debugging Experience

| Scenario                 | Approach A (Stub)         | Approach B (Testcontainers)   |
| ------------------------ | ------------------------- | ----------------------------- |
| **Test fails**           | Debug stub logic          | Debug real database           |
| **Stack traces**         | Short, clear              | Longer (includes DB driver)   |
| **Breakpoint debugging** | ✅ Easy                   | ✅ Easy (same code)           |
| **SQL debugging**        | ❌ No SQL to debug        | ✅ Can inspect actual queries |
| **Vector index issues**  | ❌ Can't debug (no index) | ✅ Can inspect index directly |

**Winner: Approach B** - Better for complex debugging

---

### 7. Developer Experience

#### Approach A - Stub

**Pros:**

- ✅ No Docker required
- ✅ Fast feedback loop
- ✅ Works offline
- ✅ Simple setup

**Cons:**

- ❌ Need to learn stub API
- ❌ Two implementations to understand
- ❌ Stub might diverge from reality

#### Approach B - Testcontainers

**Pros:**

- ✅ Tests look like production code
- ✅ One implementation to understand
- ✅ Validates real database behavior

**Cons:**

- ❌ Requires Docker installed
- ❌ Slower feedback
- ❌ More complex setup
- ❌ Doesn't work offline

**Winner: Tie** - Depends on developer preference

---

### 8. Real-World Bug Detection

#### Bugs Approach A Would Miss:

1. **Vector Index Misconfiguration**

   ```java
   // Production code has wrong dimension
   CREATE VECTOR INDEX Content[embedding] ... DIMENSIONS 768
   // But embeddings are 384-dimensional
   // Stub doesn't validate this!
   ```

2. **SQL Syntax Errors**

   ```java
   // Typo in production SQL
   "SELECT FROM Content WHERE embeding IS NULL" // Missing 'd'
   // Stub doesn't run SQL!
   ```

3. **Transaction Issues**

   ```java
   // Production code forgets transaction
   public Content update(Content content) {
     // Missing: database.transaction(() -> {
     database.command("sql", "UPDATE ...");
   }
   // Stub doesn't test transactions!

   ```

4. **Mapping Bugs**
   ```java
   // ContentMapper incorrectly maps date
   vertex.set("downloadedAt", content.downloadedAt().toString()); // Wrong!
   // Should be: vertex.set("downloadedAt", content.downloadedAt());
   // Stub bypasses mapper!
   ```

#### Bugs Approach B Would Catch:

All of the above ✅

**Winner: Approach B** - Catches production bugs

---

### 9. Cost Analysis

#### Approach A - Stub

**Initial Cost:**

- 4-6 hours to implement `InMemoryContentRepository`
- 2 hours to implement `FakeEmbeddingGenerator`
- 2 hours to refactor tests
- **Total: 8-10 hours**

**Ongoing Cost:**

- 1-2 hours per quarter to maintain stub
- **Annual: 4-8 hours**

#### Approach B - Testcontainers

**Initial Cost:**

- 2 hours to create `ArcadeDbContainer`
- 2 hours to refactor tests
- 1 hour to document Docker requirements
- **Total: 5 hours**

**Ongoing Cost:**

- 0-1 hours per quarter (reuses production code)
- **Annual: 0-4 hours**

**Winner: Approach B** - Lower total cost

---

### 10. Specific Use Cases

#### When Approach A (Stub) is Better:

1. **Pure Unit Tests**

   - Testing business logic in isolation
   - No database behavior to validate
   - Example: `ValidationService` tests

2. **CI/CD Constraints**

   - No Docker available
   - Extremely tight time budgets
   - Parallel test execution critical

3. **Offline Development**

   - Developers work without internet
   - No container registry access

4. **Algorithm Validation**
   - Testing custom similarity algorithms
   - Comparing different vector search approaches

#### When Approach B (Testcontainers) is Better:

1. **Integration Testing**

   - Testing database interactions ✅
   - Validating queries and schemas ✅
   - Testing vector indexing ✅

2. **Regression Prevention**

   - Catching SQL errors ✅
   - Validating migrations ✅
   - Testing production configurations ✅

3. **Confidence Building**
   - Proving system works end-to-end ✅
   - Validating performance characteristics ✅

---

## Recommended Hybrid Approach

### Best of Both Worlds

**Use both, strategically:**

```
src/test/java/
├── stubs/
│   ├── InMemoryContentRepository.java      (for pure unit tests)
│   └── FakeEmbeddingGenerator.java         (for all tests)
│
├── testcontainers/
│   └── ArcadeDbContainer.java               (for integration tests)
│
├── unit/                                    (Fast, Approach A)
│   ├── BackfillEmbeddingsServiceTest.java  (uses stubs)
│   ├── SearchContentServiceTest.java       (uses stubs)
│   └── ...
│
└── integration/                             (Confident, Approach B)
    ├── VectorSearchIntegrationTest.java    (uses Testcontainers)
    ├── ContentPersistenceIntegrationTest.java
    └── ...
```

### Test Distribution

| Test Type             | Count | Approach        | Execution Time |
| --------------------- | ----- | --------------- | -------------- |
| **Unit Tests**        | 40    | In-Memory Stubs | ~5 seconds     |
| **Integration Tests** | 7     | Testcontainers  | ~25 seconds    |
| **Total**             | 47    | Hybrid          | ~30 seconds    |

---

## Final Recommendation

### For Phase 1: Use **Approach B (Testcontainers)**

**Rationale:**

1. ✅ **Higher ROI** - Lower implementation cost (5h vs 10h)
2. ✅ **Better Coverage** - Tests real database behavior
3. ✅ **Less Maintenance** - Reuses production code
4. ✅ **Bug Prevention** - Catches SQL, schema, and mapping bugs
5. ✅ **Team Alignment** - Tests match production environment

**Trade-offs Accepted:**

- ⚠️ Slower tests (30s vs 5s) - acceptable for quality gain
- ⚠️ Docker required - standard in 2024 development

### Implementation Plan

**Week 1:**

- [ ] Create `ArcadeDbContainer` class
- [ ] Create `FakeEmbeddingGenerator` (still useful!)
- [ ] Set up test database schema initialization
- [ ] Document Docker requirements

**Week 2:**

- [ ] Refactor `BackfillEmbeddingsServiceTest` with Testcontainers
- [ ] Refactor `SearchContentServiceTest` with Testcontainers
- [ ] Add integration tests for edge cases

**Week 3:**

- [ ] Measure test performance
- [ ] Optimize slow tests (if needed)
- [ ] Consider splitting fast/slow tests if CI time is issue

---

## Conclusion

While **Approach A (Stubs)** offers speed, **Approach B (Testcontainers)** provides:

- Better bug detection
- Lower maintenance
- Higher confidence
- Production parity

**Use Testcontainers for Phase 1**, and introduce stubs later only if CI performance becomes a bottleneck.

The extra ~25 seconds per test run is a worthwhile investment for the quality and confidence gained.
