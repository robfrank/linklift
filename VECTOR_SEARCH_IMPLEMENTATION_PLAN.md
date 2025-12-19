# Vector Search Implementation Plan (Refined: Ollama Integration)

This document outlines the focused plan for implementing vector search for content in LinkLift using ArcadeDB's vector index and **Ollama** as the embedding provider.

## 1. Goal

Enable semantic search across saved link content by generating text embeddings via Ollama and indexing them in ArcadeDB.

## 2. Infrastructure Requirements

- **Ollama**: Must be running (locally or as a sidecar container).
- **Model**: `nomic-embed-text` (preferred for its 8192 context length and high quality) or `all-minilm`.
- **ArcadeDB**: Version 25.1.1+ (currently using 25.11.1).

## 3. Data Model Changes

### 3.1 ArcadeDB Schema Updates

Add the `embedding` property and the vector index.

**File:** `src/main/resources/schema/007_add_vector_index.sql`

```sql
-- Add embedding property (List of Floats)
-- nomic-embed-text uses 768 dimensions
CREATE PROPERTY Content.embedding IF NOT EXISTS LIST<FLOAT>;

-- Create Vector Index using HNSW
-- DISTANCE COSINE is optimal for text embeddings
CREATE INDEX Content_embedding ON Content (embedding)
  VECTOR
  KEY M 16
  EF 100
  DISTANCE COSINE;
```

### 3.2 Domain Model Updates

Update `it.robfrank.linklift.application.domain.model.Content` record to include the embedding.

```java
public record Content(
  // ... existing fields ...
  @JsonProperty("embedding") @Nullable List<Float> embedding
) {
  // ...
}

```

## 4. Embedding Generation (Ollama Integration)

### 4.1 Interface (Port)

```java
public interface EmbeddingGenerator {
  List<Float> generateEmbedding(String text);
}

```

### 4.2 Implementation (Adapter)

Create `OllamaEmbeddingAdapter` using Java's `HttpClient` to call Ollama's REST API.

- **Endpoint**: `POST http://localhost:11434/api/embeddings`
- **Request Body**:
  ```json
  {
    "model": "nomic-embed-text",
    "prompt": "Text to embed..."
  }
  ```
- **Response**:
  ```json
  {
    "embedding": [0.1, 0.2, ...]
  }
  ```

### 4.3 Configuration

Add settings to `application.toml` or equivalent:

- `ollama.url`: `http://localhost:11434`
- `ollama.model`: `nomic-embed-text`

## 5. Implementation Strategy

### Phase 1: Foundation & Data Access

1.  **Execute Schema**: Apply `007_add_vector_index.sql`.
2.  **Update Repository**:
    - Modify `ArcadeContentRepository` and `ContentMapper` to persist and load the `embedding` field.
    - Implement `findSimilar(List<Float> vector, int limit)` using ArcadeDB's `KNN` syntax:
      `SELECT FROM Content WHERE embedding VECTOR KNN [vector, limit]`

### Phase 2: Ollama Connectivity

1.  **Create Adapter**: Implement `OllamaEmbeddingAdapter`.
2.  **Error Handling**: Handle cases where Ollama is unreachable or the model is not pulled (attempt to auto-pull or log clear instructions).

### Phase 3: Workflow Integration

1.  **Async Processing**: Generating embeddings can be slow for large texts. Integration should be asynchronous.
2.  **Use Case Update**: Update the content saving flow (e.g., `DownloadContentUseCase`) to trigger an extraction/embedding event.
3.  **Search API**:
    - Create `SearchContentUseCase`.
    - Input: Query string.
    - Process: `Query` -> `Ollama Embedding` -> `ArcadeDB KNN Search`.

## 6. Backfill Strategy

For existing links without embeddings:

1.  **Admin Task**: Create a utility or endpoint `POST /api/admin/embeddings/reindex`.
2.  **Batch Processing**:
    - Fetch chunks of `Content` where `embedding IS NULL`.
    - For each, generate embedding via Ollama and update ArcadeDB.
    - Add logs to track progress.
