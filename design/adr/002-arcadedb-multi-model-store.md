# ArcadeDB as the single multi-model store

## Status

Accepted (recorded retroactively in the retrofit baseline).

## Context

LinkLift stores documents (links, users, content, notes), relationships (ownership, domains, tags, collection membership, page-to-page links) and embedding vectors. Using a separate relational store, graph store and vector store would multiply operational components.

## Decision

Use one ArcadeDB database named `linklift`, reached over HTTP with the `arcadedb-network` `RemoteDatabase` client (RQ-153). Model entities as vertex types and relationships as edge types (RQ-161), query with ArcadeDB SQL including graph traversal (`out()`, `in()`, `expand()`), and keep embeddings on the `Content` vertex under an `LSM_VECTOR` index (RQ-163). Create and evolve the schema from ordered SQL scripts applied at every start by `src/main/java/it/robfrank/linklift/config/DatabaseInitializer.java` (RQ-160).

## Consequences

- Ownership checks and listings are graph traversals from the `User` vertex (`specifications.md#persistence`).
- Schema scripts must stay re-runnable; most use `IF NOT EXISTS`, the seed inserts do not (`implementation.md#known-gaps`).
- Vector-search filtering is limited by the server's SQL support, which forces the over-fetch-then-filter approach and an inlined query vector (NF-005, `specifications.md#vector-search`).
- Integration tests need a real ArcadeDB, provided by Testcontainers (NF-008).
- Credentials are currently literals in code (RQ-153).
