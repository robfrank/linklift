# Vector Search Implementation - Fixes and Improvements Plan

This document outlines the plan to address issues and improvements identified during the codebase analysis of the vector search implementation.

## Issues Summary

1. Threading pattern inconsistency in AdminController
2. Schema dimension mismatch needs verification
3. Missing test coverage for new components
4. Missing backfill status endpoint (enhancement)
5. Unchecked test file refactoring items

---

## Phase 1: Critical Fixes

### 1.1 Verify and Fix Embedding Dimensions Mismatch

**Priority:** HIGH
**Estimated Complexity:** Low

**Issue:**

- Schema defines 384 dimensions (`007_add_vector_index.sql`)
- Plan specifies 768 dimensions for `nomic-embed-text` model
- Configuration defaults to `nomic-embed-text`

**Tasks:**

1. **Verify actual model dimensions:**

   ```bash
   # Test with Ollama to get actual dimensions
   curl http://localhost:11434/api/embeddings \
     -d '{"model":"nomic-embed-text","prompt":"test"}' | \
     jq '.embedding | length'
   ```

2. **Decision tree:**

   - **If output is 768:** Update schema to 768 dimensions
     - Create `008_update_vector_dimensions.sql`
     - Rebuild vector index
     - Re-run backfill for all existing embeddings
   - **If output is 384:** Update documentation to reflect correct model
     - Verify `all-minilm` is being used instead
     - Update VECTOR_SEARCH_IMPLEMENTATION_PLAN.md
     - Update SecureConfiguration default if needed

3. **Update configuration consistency:**
   - Ensure `SecureConfiguration.getOllamaModel()` matches schema dimensions
   - Add validation check in `OllamaEmbeddingAdapter` constructor
   - Log warning if dimension mismatch detected

**Files to modify:**

- `src/main/resources/schema/008_update_vector_dimensions.sql` (if needed)
- `VECTOR_SEARCH_IMPLEMENTATION_PLAN.md`
- `src/main/java/it/robfrank/linklift/adapter/out/ai/OllamaEmbeddingAdapter.java` (add validation)

---

### 1.2 Fix Threading Pattern in AdminController

**Priority:** MEDIUM
**Estimated Complexity:** Low

**Issue:**

- `AdminController.backfillEmbeddings()` creates threads directly
- Should use injected ExecutorService for consistency and resource management

**Tasks:**

1. **Inject ExecutorService into AdminController:**

   ```java
   public class AdminController {

     private final BackfillEmbeddingsUseCase backfillEmbeddingsUseCase;
     private final ExecutorService executorService;

     public AdminController(BackfillEmbeddingsUseCase backfillEmbeddingsUseCase, ExecutorService executorService) {
       this.backfillEmbeddingsUseCase = backfillEmbeddingsUseCase;
       this.executorService = executorService;
     }
   }

   ```

2. **Update backfillEmbeddings method:**

   ```java
   public void backfillEmbeddings(Context ctx) {
     executorService.submit(backfillEmbeddingsUseCase::backfill);
     ctx.status(HttpStatus.ACCEPTED).result("Backfill process started");
   }

   ```

3. **Update Application.java wiring:**
   - Pass `executorService` to AdminController constructor
   - Line ~121 in Application.java

**Files to modify:**

- `src/main/java/it/robfrank/linklift/adapter/in/web/AdminController.java`
- `src/main/java/it/robfrank/linklift/Application.java`

---

## Phase 2: Test Coverage

### 2.1 Create SearchContentServiceTest

**Priority:** HIGH
**Estimated Complexity:** Medium

**Test Cases to Cover:**

1. **Happy Path:**

   - Valid query returns results
   - Results are limited correctly
   - Embedding generation is called with query

2. **Validation:**

   - Null query throws exception
   - Empty query throws exception
   - Blank query throws exception

3. **Error Handling:**

   - Embedding generation failure propagates correctly
   - Repository errors are handled

4. **Edge Cases:**
   - Limit of 0
   - Negative limit
   - Very large limit
   - Query with special characters

**File to create:**

- `src/test/java/it/robfrank/linklift/application/domain/service/SearchContentServiceTest.java`

**Template:**

```java
@ExtendWith(MockitoExtension.class)
class SearchContentServiceTest {

  @Mock
  private LoadContentPort loadContentPort;

  @Mock
  private EmbeddingGenerator embeddingGenerator;

  private SearchContentService searchContentService;

  @BeforeEach
  void setUp() {
    searchContentService = new SearchContentService(embeddingGenerator, loadContentPort);
  }
  // Test methods...
}

```

---

### 2.2 Create BackfillEmbeddingsServiceTest

**Priority:** HIGH
**Estimated Complexity:** High

**Test Cases to Cover:**

1. **Concurrent Execution Prevention:**

   - Second call while backfill running returns immediately
   - Flag is reset after completion

2. **Batch Processing:**

   - Processes items in batches of 100
   - Continues after batch completion
   - Stops when no more items

3. **Error Resilience:**

   - Embedding generation failure doesn't stop batch
   - Failed items are logged
   - Success/failure counts are accurate
   - Repository errors are handled

4. **Content Update:**

   - Content is saved with correct embedding
   - All original content fields are preserved
   - Null text content is skipped

5. **Threading:**
   - ExecutorService is used correctly
   - Interruption is handled gracefully

**File to create:**

- `src/test/java/it/robfrank/linklift/application/domain/service/BackfillEmbeddingsServiceTest.java`

**Mock Strategy:**

```java
@ExtendWith(MockitoExtension.class)
class BackfillEmbeddingsServiceTest {

  @Mock
  private LoadContentPort loadContentPort;

  @Mock
  private SaveContentPort saveContentPort;

  @Mock
  private EmbeddingGenerator embeddingGenerator;

  @Mock
  private ExecutorService executorService;

  private BackfillEmbeddingsService service;

  @BeforeEach
  void setUp() {
    service = new BackfillEmbeddingsService(loadContentPort, saveContentPort, embeddingGenerator, executorService);
  }
  // Special setup for testing concurrency with CountDownLatch
}

```

---

### 2.3 Create OllamaEmbeddingAdapterTest

**Priority:** MEDIUM
**Estimated Complexity:** High (requires HTTP mocking)

**Test Cases to Cover:**

1. **Happy Path:**

   - Valid text returns embedding
   - Embedding has correct dimensions
   - HTTP request is formatted correctly

2. **HTTP Error Handling:**

   - 404 Not Found (model not available)
   - 500 Internal Server Error
   - Network timeout
   - Connection refused
   - Interrupted exception

3. **Response Parsing:**

   - Valid JSON response
   - Missing "embedding" field
   - Invalid JSON format
   - Non-list embedding value
   - Null values in embedding array

4. **Configuration:**
   - Custom Ollama URL is used
   - Custom model name is used
   - Defaults work correctly

**File to create:**

- `src/test/java/it/robfrank/linklift/adapter/out/ai/OllamaEmbeddingAdapterTest.java`

**Mock Strategy:**
Use `MockWebServer` or similar for HTTP mocking:

```java
class OllamaEmbeddingAdapterTest {

  private MockWebServer mockWebServer;
  private OllamaEmbeddingAdapter adapter;
  private HttpClient httpClient;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    httpClient = HttpClient.newHttpClient();
    adapter = new OllamaEmbeddingAdapter(httpClient, mockWebServer.url("/").toString(), "test-model");
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }
}

```

---

### 2.4 Address Test File Refactoring (from Plan 7.1)

**Priority:** LOW
**Estimated Complexity:** Low

**Tasks:**

1. **Review and fix null pointer warnings:**

   - Run static analysis on test files
   - Identify "Potential null pointer access" warnings
   - Add `Objects.requireNonNull()` where appropriate
   - Use `@NonNull` annotations in test helpers

2. **Refine mock behavior:**

   - Ensure mocks return non-null by default
   - Use `lenient()` only where necessary
   - Review `GetContentServiceTest`
   - Review `DownloadContentServiceTest`

3. **Add explicit null checks:**
   - Test helper methods should validate inputs
   - Factory methods should use `@NonNull` annotations

**Files to review:**

- `src/test/java/it/robfrank/linklift/application/domain/service/GetContentServiceTest.java`
- `src/test/java/it/robfrank/linklift/application/domain/service/DownloadContentServiceTest.java`
- All test utility/helper classes

---

## Phase 3: Enhancements

### 3.1 Add Backfill Status Endpoint

**Priority:** MEDIUM
**Estimated Complexity:** Medium

**Goal:** Provide visibility into backfill progress and status.

**Tasks:**

1. **Create BackfillStatus domain object:**

   ```java
   public record BackfillStatus(
     boolean isRunning,
     @Nullable LocalDateTime startedAt,
     @Nullable LocalDateTime completedAt,
     int totalProcessed,
     int successCount,
     int errorCount,
     @Nullable String lastError
   ) {}

   ```

2. **Update BackfillEmbeddingsService:**

   - Add `getStatus()` method
   - Track start/completion timestamps
   - Store last error message
   - Make counters accessible (thread-safe)

3. **Create GetBackfillStatusUseCase:**

   ```java
   public interface GetBackfillStatusUseCase {
     @NonNull
     BackfillStatus getStatus();
   }

   ```

4. **Update AdminController:**

   ```java
   public void getBackfillStatus(Context ctx) {
     BackfillStatus status = getBackfillStatusUseCase.getStatus();
     ctx.json(status);
   }

   ```

5. **Add route in WebBuilder.java:**

   ```java
   app.get("/api/v1/admin/backfill-status", adminController::getBackfillStatus);
   ```

6. **Update frontend AdminPage:**
   - Poll status endpoint when backfill is running
   - Display progress (processed count, success/error)
   - Show completion message

**Files to create/modify:**

- `src/main/java/it/robfrank/linklift/application/domain/model/BackfillStatus.java` (new)
- `src/main/java/it/robfrank/linklift/application/port/in/GetBackfillStatusUseCase.java` (new)
- `src/main/java/it/robfrank/linklift/application/domain/service/BackfillEmbeddingsService.java` (modify)
- `src/main/java/it/robfrank/linklift/adapter/in/web/AdminController.java` (modify)
- `src/main/java/it/robfrank/linklift/config/WebBuilder.java` (modify)
- `webapp/src/infrastructure/ui/pages/AdminPage.tsx` (modify)

---

### 3.2 Add Dimension Validation in OllamaEmbeddingAdapter

**Priority:** LOW
**Estimated Complexity:** Low

**Goal:** Detect and warn about dimension mismatches early.

**Tasks:**

1. **Add expected dimensions to configuration:**

   ```java
   public static int getOllamaExpectedDimensions() {
     return Integer.parseInt(System.getenv().getOrDefault("LINKLIFT_OLLAMA_DIMENSIONS", "384"));
   }

   ```

2. **Add lazy dimension validation:**

   The actual implementation uses a lazy, thread-safe validation approach that occurs on the first successful embedding generation, rather than in the constructor. This avoids infinite recursion and defers validation until the Ollama service is actually called.

   ```java
   private volatile boolean dimensionValidated = false;

   @Override
   @NonNull
   public List<Float> generateEmbedding(@NonNull String text) {
       try {
           // ... HTTP request code ...

           Map<String, Object> responseBody = objectMapper.readValue(response.body(), ...);
           List<Float> embedding = extractEmbeddingFromResponse(responseBody);

           // Validate dimensions on first successful embedding (thread-safe lazy validation)
           if (!dimensionValidated) {
               validateDimensions(embedding.size());
           }

           return embedding;
       } catch (IOException e) {
           // ... error handling ...
       }
   }

   private synchronized void validateDimensions(int actualDimensions) {
       if (dimensionValidated) {
           return; // Already validated by another thread
       }

       int expectedDimensions = SecureConfiguration.getOllamaExpectedDimensions();
       if (actualDimensions != expectedDimensions) {
           logger.warn(
               "Dimension mismatch detected! Model '{}' produces {} dimensions, " +
               "but schema/configuration expects {} dimensions. " +
               "Update LINKLIFT_OLLAMA_DIMENSIONS environment variable to match, " +
               "or update the vector index schema to {} dimensions.",
               model, actualDimensions, expectedDimensions, actualDimensions
           );
       } else {
           logger.debug("Embedding dimensions validated: {} dimensions match expected configuration", actualDimensions);
       }

       dimensionValidated = true;
   }
   ```

   **Key Benefits:**

   - Avoids infinite recursion (validation doesn't call generateEmbedding)
   - Thread-safe with synchronized method and volatile flag
   - Lazy validation (only on first successful embedding)
   - Defers validation until Ollama service is actually available

**Files to modify:**

- `src/main/java/it/robfrank/linklift/config/SecureConfiguration.java`
- `src/main/java/it/robfrank/linklift/adapter/out/ai/OllamaEmbeddingAdapter.java`

---

## Phase 4: Documentation Updates

### 4.1 Update VECTOR_SEARCH_IMPLEMENTATION_PLAN.md

**Tasks:**

1. Mark completed items as done
2. Update dimension information (after verification)
3. Update Section 7 checklist
4. Add "Completed" status and date
5. Document actual vs. planned deviations

### 4.2 Update README.md (if exists)

**Tasks:**

1. Document vector search feature
2. Document Ollama setup requirements
3. Document environment variables:
   - `LINKLIFT_OLLAMA_URL`
   - `LINKLIFT_OLLAMA_MODEL`
   - `LINKLIFT_OLLAMA_DIMENSIONS` (new)
4. Document API endpoints
5. Document backfill process

### 4.3 Create VECTOR_SEARCH_USAGE.md

**Content:**

1. **Setup Guide:**

   - Install Ollama
   - Pull embedding model
   - Configure environment variables
   - Run initial backfill

2. **API Usage Examples:**

   - Search query examples
   - Trigger backfill
   - Check backfill status

3. **Troubleshooting:**

   - Ollama not running
   - Model not found
   - Dimension mismatches
   - Empty search results

4. **Performance Tuning:**
   - Batch size configuration
   - Index parameters
   - Model selection

---

## Implementation Order

### Sprint 1: Critical Fixes (1-2 days)

1. ✅ Verify embedding dimensions
2. ✅ Fix schema if needed
3. ✅ Fix AdminController threading pattern
4. ✅ Add dimension validation

### Sprint 2: Test Coverage (3-5 days)

1. ✅ Create SearchContentServiceTest
2. ✅ Create BackfillEmbeddingsServiceTest
3. ✅ Create OllamaEmbeddingAdapterTest
4. ✅ Address test file refactoring

### Sprint 3: Enhancements (2-3 days)

1. ✅ Implement backfill status tracking
2. ✅ Create status endpoint
3. ✅ Update frontend to show status

### Sprint 4: Documentation (1 day)

1. ✅ Update all documentation
2. ✅ Create usage guide
3. ✅ Update README

---

## Success Criteria

- ✅ All unit tests pass with >80% coverage for new code
- ✅ No threading pattern inconsistencies
- ✅ Embedding dimensions match schema
- ✅ Dimension validation warns on mismatch
- ✅ Backfill status is visible via API
- ✅ All documentation is up-to-date
- ✅ No static analysis warnings in test files
- ✅ Frontend shows backfill progress

---

## Risk Assessment

| Risk                                              | Likelihood | Impact | Mitigation                                  |
| ------------------------------------------------- | ---------- | ------ | ------------------------------------------- |
| Dimension change requires re-indexing all content | Medium     | High   | Run backfill during low-traffic period      |
| Ollama service unavailable during testing         | Medium     | Low    | Use MockWebServer for HTTP tests            |
| Thread pool exhaustion from backfill              | Low        | Medium | ExecutorService already has bounded pool    |
| Breaking changes to existing embeddings           | Low        | High   | Create migration script, test on copy first |

---

## Dependencies

- **External:** Ollama service running with appropriate model
- **Internal:** ExecutorService configured in Application
- **Testing:** MockWebServer or WireMock for HTTP mocking
- **Database:** ArcadeDB 25.11.1+ with LSM_VECTOR support

---

## Rollback Plan

If issues arise during implementation:

1. **Schema Changes:**

   - Keep old index alongside new one
   - Switch back via configuration flag
   - Drop new index if needed

2. **Code Changes:**

   - All changes should be backward compatible
   - Feature flag for vector search if needed
   - Git revert if critical issues found

3. **Data:**
   - Embeddings are nullable, so removal is safe
   - Can regenerate via backfill at any time

---

## Notes

- All changes should maintain backward compatibility
- Existing functionality must not be disrupted
- Vector search is an additive feature, not a replacement
- Graceful degradation if Ollama is unavailable
