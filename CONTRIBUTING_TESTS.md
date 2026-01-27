# Contributing Tests Guide

## Overview

LinkLift uses a modern testing approach that emphasizes **integration testing with real implementations** over mocking. This guide will help you write effective tests that validate actual behavior while maintaining fast execution times.

## Testing Philosophy

### Core Principles

1. **Test Behavior, Not Implementation** - Focus on what the code does, not how it does it
2. **Use Real Implementations** - Prefer real databases and HTTP clients over mocks
3. **Minimize Mocking** - Only mock external services you don't control (e.g., Ollama API)
4. **Fast Feedback** - Keep tests fast enough for frequent execution
5. **Deterministic** - Tests should produce consistent results

### Why We Moved Away from Mocks

Traditional mock-heavy testing has several drawbacks:

- ❌ Tests break when refactoring internal implementation
- ❌ Doesn't catch integration issues (SQL errors, schema mismatches)
- ❌ Requires maintaining complex mock setup code
- ❌ Tests verify mock interactions, not actual behavior

Our approach:

- ✅ Tests survive refactoring (test behavior, not mocks)
- ✅ Catches real integration bugs early
- ✅ Less test code to maintain
- ✅ Higher confidence in production readiness

## Test Types

### 1. Integration Tests with Testcontainers

**When to use:**

- Testing services that interact with the database
- Validating repository implementations
- Testing complex queries and transactions
- Vector search functionality

**Examples:**

- `BackfillEmbeddingsServiceTest` - Tests embedding backfill with real ArcadeDB
- `SearchContentServiceTest` - Tests vector search with real LSM_TREE index
- `ArcadeDbContainerTest` - Tests container setup and schema initialization

**How to write:**

```java
class MyServiceTest extends ArcadeDbTestBase {

  private MyService myService;
  private FakeEmbeddingGenerator embeddingGenerator;

  @BeforeEach
  void setUp() {
    super.setUpDatabase(); // Creates real database and repository

    embeddingGenerator = new FakeEmbeddingGenerator();
    myService = new MyService(repository, embeddingGenerator);
  }

  @Test
  void myTest_shouldDoSomething_whenCondition() {
    // Given - setup test data in REAL database
    Content content = createTestContent("id-1", "link-1", "test content");
    repository.saveContent(content);

    // When - execute the operation
    myService.doSomething("id-1");

    // Then - verify actual database state
    Content updated = repository.findContentById("id-1").orElseThrow();
    assertThat(updated.someField()).isEqualTo(expectedValue);
  }

  @AfterEach
  void tearDown() {
    super.tearDownDatabase(); // Cleans database
  }
}

```

**Key Points:**

- Extend `ArcadeDbTestBase` for automatic database setup/cleanup
- Use `repository` field (real `ContentPersistenceAdapter`)
- Use `FakeEmbeddingGenerator` for deterministic embeddings
- Test actual database state, not mock interactions
- No `verify()` statements needed

### 2. HTTP Tests with WireMock

**When to use:**

- Testing HTTP adapters (Ollama, content downloaders)
- Validating JSON serialization/deserialization
- Testing error handling for HTTP failures
- Simulating external API responses

**Examples:**

- `OllamaEmbeddingAdapterTest` - Tests Ollama API integration
- `HttpContentDownloaderTest` - Tests HTTP content fetching
- `LinkContentExtractorServiceTest` - Tests HTML parsing with mocked HTTP

**How to write:**

```java
class MyHttpAdapterTest {

  @RegisterExtension
  static WireMockExtension wireMock = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private MyHttpAdapter adapter;
  private HttpClient httpClient;

  @BeforeEach
  void setUp() {
    httpClient = HttpClient.newHttpClient();
    String baseUrl = "http://localhost:" + wireMock.getPort();
    adapter = new MyHttpAdapter(httpClient, baseUrl);
  }

  @Test
  void myTest_shouldHandleSuccessResponse() {
    // Given - mock HTTP response
    wireMock.stubFor(
      post("/api/endpoint")
        .withRequestBody(matchingJsonPath("$.field", equalTo("value")))
        .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("{\"result\": \"success\"}"))
    );

    // When - make HTTP call
    String result = adapter.doSomething("value");

    // Then - verify result
    assertThat(result).isEqualTo("success");

    // And - verify request was made correctly
    wireMock.verify(postRequestedFor(urlEqualTo("/api/endpoint")).withHeader("Content-Type", equalTo("application/json")));
  }
}

```

**Key Points:**

- Use `WireMockExtension` with dynamic port
- Use real `HttpClient` (not mocked)
- Stub HTTP responses with realistic JSON
- Verify request format and headers
- Test error scenarios (timeouts, 500 errors, malformed JSON)

### 3. Unit Tests (No External Dependencies)

**When to use:**

- Testing domain models and value objects
- Testing pure business logic
- Testing utility classes
- Testing validation rules

**Examples:**

- `LinkTest` - Tests Link domain model
- `UserTest` - Tests User domain model
- `RoleTest` - Tests Role enum

**How to write:**

```java
class MyDomainModelTest {

  @Test
  void constructor_shouldValidateInput() {
    // When/Then - test validation
    assertThatThrownBy(() -> new MyModel(null, "valid")).isInstanceOf(ValidationException.class).hasMessageContaining("cannot be null");
  }

  @Test
  void method_shouldCalculateCorrectly() {
    // Given
    MyModel model = new MyModel("field1", "field2");

    // When
    String result = model.calculate();

    // Then
    assertThat(result).isEqualTo("expected");
  }
}

```

**Key Points:**

- No external dependencies (database, HTTP, etc.)
- Fast execution (milliseconds)
- Focus on business logic and validation
- Use AssertJ for fluent assertions

### 4. Controller Tests (Javalin)

**When to use:**

- Testing REST API endpoints
- Validating request/response mapping
- Testing authentication/authorization
- Testing error handling at HTTP layer

**Examples:**

- `NewLinkControllerTest` - Tests link creation endpoint
- `AuthenticationControllerTest` - Tests auth endpoints
- `CollectionControllerTest` - Tests collection endpoints

**How to write:**

```java
class MyControllerTest {

  private MyController controller;
  private MyUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = mock(MyUseCase.class);
    controller = new MyController(useCase);
  }

  @Test
  void endpoint_shouldReturnSuccess_whenValidRequest() throws Exception {
    // Given
    when(useCase.execute(any())).thenReturn(expectedResult);

    // When/Then
    JavalinTest.test(Javalin.create().post("/api/endpoint", controller::handle), (server, client) -> {
      var response = client.post("/api/endpoint", Map.of("field", "value"));

      assertThat(response.code()).isEqualTo(200);
      assertThat(response.body().string()).contains("success");
    });
  }
}

```

**Key Points:**

- Use `JavalinTest` for HTTP testing
- Mock use cases (not repositories)
- Test HTTP status codes and response format
- Test error scenarios (400, 401, 500)

### 5. E2E Tests (Optional)

**When to use:**

- Pre-release validation
- Testing with real Ollama embeddings
- Validating complete workflows
- Rarely - these are slow (~2-3 minutes)

**Examples:**

- `VectorSearchE2ETest` (if created)

**How to write:**

```java
@Testcontainers
class MyE2ETest {

  @Container
  static ArcadeDbContainer arcadeDb = new ArcadeDbContainer();

  @Container
  static GenericContainer<?> ollama = new GenericContainer<>("ollama/ollama:latest")
    .withExposedPorts(11434)
    .waitingFor(Wait.forHttp("/api/tags").forPort(11434));

  // ... setup with REAL Ollama adapter

  @Test
  void endToEnd_shouldWork_withRealEmbeddings() {
    // Test with real Ollama service
    // This is SLOW but validates actual semantic similarity
  }
}

```

**Key Points:**

- Only for critical end-to-end validation
- Requires Docker and Ollama image (~400MB)
- Slow execution (~60s+ per test)
- Run separately: `mvn test -Pe2e-tests`

## Best Practices

### Test Structure (Given/When/Then)

Always use clear Given/When/Then structure:

```java
@Test
void methodName_shouldExpectedBehavior_whenCondition() {
  // Given - setup test data and preconditions
  Content content = createTestContent("id-1", "link-1", "text");
  repository.saveContent(content);

  // When - execute the operation being tested
  service.processContent("id-1");

  // Then - verify the expected outcome
  Content result = repository.findContentById("id-1").orElseThrow();
  assertThat(result.processed()).isTrue();
}

```

### Test Naming Convention

Use descriptive test names that explain:

1. What method/feature is being tested
2. What the expected behavior is
3. Under what conditions

**Good examples:**

- `search_shouldReturnSimilarContent_whenQueryMatches()`
- `backfill_shouldSkipContent_whenEmbeddingGenerationFails()`
- `saveContent_shouldThrowException_whenIdIsNull()`

**Bad examples:**

- `testSearch()`
- `test1()`
- `shouldWork()`

### Assertions

Use AssertJ for fluent, readable assertions:

```java
// Good - fluent and descriptive
assertThat(results)
  .hasSize(2)
  .extracting(Content::id)
  .containsExactlyInAnyOrder("id-1", "id-2");

// Avoid - less readable
assertEquals(2, results.size());
assertTrue(results.stream().anyMatch(c -> c.id().equals("id-1")));
```

### Test Data Creation

Create helper methods for test data:

```java
private Content createTestContent(String id, String linkId, String text) {
  return new Content(
    id,
    linkId,
    text,
    null, // embedding - will be generated if needed
    LocalDateTime.now(FIXED_TEST_CLOCK),
    null, // error
    false // needsEmbedding
  );
}

```

### Async Testing

For async operations, use Awaitility:

```java
@Test
void async_shouldComplete_eventually() {
  // When
  service.startAsyncOperation();

  // Then - wait for completion
  await().atMost(5, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).until(() -> repository.findById("id").isPresent());
}

```

### Test Isolation

Ensure tests don't interfere with each other:

```java
@AfterEach
void tearDown() {
  super.tearDownDatabase(); // Clean up database
  executorService.shutdown(); // Clean up threads
}

```

## Common Patterns

### Testing Validation

```java
@Test
void method_shouldThrowValidationException_whenInputInvalid() {
  assertThatThrownBy(() -> service.method(null)).isInstanceOf(ValidationException.class).hasMessageContaining("cannot be null");
}

```

### Testing Error Handling

```java
@Test
void method_shouldHandleError_gracefully() {
  // Given - setup error condition
  embeddingGenerator.throwOnNextCall(new RuntimeException("API error"));

  // When/Then - verify error handling
  assertThatThrownBy(() -> service.generateEmbedding("text")).isInstanceOf(RuntimeException.class).hasMessageContaining("API error");
}

```

### Testing Pagination

```java
@Test
void list_shouldReturnPage_whenPaginationRequested() {
  // Given - create multiple items
  for (int i = 0; i < 25; i++) {
    repository.save(createTestItem("id-" + i));
  }

  // When - request page
  Page<Item> page = service.list(0, 10);

  // Then - verify pagination
  assertThat(page.content()).hasSize(10);
  assertThat(page.totalElements()).isEqualTo(25);
  assertThat(page.hasNext()).isTrue();
}

```

## Running Tests

### All Tests (Excludes E2E)

```bash
mvn test
```

### Specific Test Class

```bash
mvn test -Dtest=BackfillEmbeddingsServiceTest
```

### Specific Test Method

```bash
mvn test -Dtest=BackfillEmbeddingsServiceTest#backfill_shouldProcessBatch_whenContentExists
```

### E2E Tests (Optional)

```bash
mvn test -Pe2e-tests
```

### With Coverage

```bash
mvn test jacoco:report
open target/site/jacoco/index.html
```

## Troubleshooting

### Docker Issues

**Problem:** Tests fail with "Could not start container"

**Solution:**

- Ensure Docker Desktop is running
- Check Docker has enough resources (4GB+ RAM)
- Try: `docker system prune` to clean up

### Slow Tests

**Problem:** Tests take too long

**Solution:**

- Check if you're extending `ArcadeDbTestBase` correctly (shares single container)
- Avoid creating new containers per test
- Use `@Container static` for shared containers

### Flaky Tests

**Problem:** Tests pass sometimes, fail other times

**Solution:**

- Use `await()` for async operations instead of `Thread.sleep()`
- Ensure proper cleanup in `@AfterEach`
- Check for shared mutable state between tests

### Test Data Pollution

**Problem:** Tests fail when run together but pass individually

**Solution:**

- Ensure `@AfterEach` cleanup is working
- Use unique IDs for test data
- Check for static state that persists between tests

## Resources

- [Testcontainers Documentation](https://www.testcontainers.org/)
- [WireMock Documentation](https://wiremock.org/)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [Awaitility Documentation](http://www.awaitility.org/)
- [TEST_REFACTORING_PLAN.md](TEST_REFACTORING_PLAN.md) - Detailed refactoring plan

## Questions?

If you have questions about testing:

1. Check this guide first
2. Look at existing tests for examples
3. Review `TEST_REFACTORING_PLAN.md` for rationale
4. Ask in GitHub Discussions

---

**Remember:** Good tests are an investment. They give you confidence to refactor, catch bugs early, and serve as living documentation of how the system works.
