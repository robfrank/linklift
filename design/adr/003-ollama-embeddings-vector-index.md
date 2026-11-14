# Ollama for embeddings and answer generation

## Status

Accepted (recorded retroactively in the retrofit baseline).

## Context

Semantic search, related links, tag suggestions and question answering need text embeddings and a text generator. The project prefers a self-hosted model server over a hosted API.

## Decision

Call a self-hosted Ollama server over its HTTP API: `/api/embeddings` for vectors (RQ-070) and `/api/generate` for answers (RQ-082), both with the model named by `LINKLIFT_OLLAMA_MODEL` (RQ-152). Default to `all-minilm:l6-v2`, whose 384-dimension output matches the fixed dimension of the vector index (RQ-163). Store a zero placeholder when no embedding is available and mark the record for backfill (RQ-072, RQ-076).

## Consequences

- A content download does not fail because of Ollama; the content is stored without an embedding (RQ-057). Search answers 500 when the query cannot be embedded (RQ-074).
- Changing to a model with another dimension requires a schema change; the configured dimension only drives a warning (RQ-071).
- The same model name is sent to `/api/generate`, so answers come from the embedding-oriented default unless the variable is changed (RQ-152).
- Deployments run Ollama as a separate service (`operational-guides/deploy.md`, `operational-guides/local-run.md`).
