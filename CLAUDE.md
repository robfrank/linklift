# CLAUDE.md

This file provides guidance for Claude Code when working with the LinkLift project.

## Overview

LinkLift is a full-stack link management system with semantic search capabilities. Java 25 backend with Javalin, ArcadeDB graph/document/vector database, and Ollama-powered vector embeddings. React 19 frontend with TypeScript, MUI 7, and Zustand.

## Quick Navigation

- [Frontend Guidelines](webapp/CLAUDE.md)

## Project Structure

```
linklift/
├── src/main/java/it/robfrank/linklift/   # Java backend
│   ├── adapter/in/web/                    # REST controllers (Javalin)
│   ├── adapter/out/persistence/           # ArcadeDB repositories
│   ├── adapter/out/ai/                    # Ollama embedding/summary adapters
│   ├── adapter/out/http/                  # Content downloading & extraction
│   ├── adapter/out/security/              # BCrypt, JWT adapters
│   ├── application/domain/model/          # Domain entities (Java records)
│   ├── application/domain/service/        # Business logic (use case impls)
│   ├── application/domain/event/          # Domain events
│   ├── application/domain/exception/      # Custom exceptions + ErrorCode
│   ├── application/domain/validation/     # Business rule validators
│   ├── application/port/in/               # Input ports (27 use case interfaces)
│   ├── application/port/out/              # Output ports (19 repository interfaces)
│   └── config/                            # App config, DB init, web builder
├── src/test/java/it/robfrank/linklift/   # Backend tests
│   ├── integration/                       # Testcontainers integration tests
│   └── adapter/, application/             # Unit & adapter tests
├── webapp/                                # React frontend (see webapp/CLAUDE.md)
├── config/deploy.yml                      # Kamal deployment config
├── .github/workflows/                     # CI/CD pipelines
├── docker-compose.yml                     # Full local stack
├── pom.xml                                # Maven build (Java 25)
└── Dockerfile.local                       # Local dev container
```

## Architecture

**Hexagonal Architecture (Ports & Adapters)** with strict layer separation:

- **Domain layer** (`application/domain/`): Zero framework dependencies. Models are Java records. Services implement input port interfaces.
- **Port layer** (`application/port/`): Interfaces only. `in/` defines use cases, `out/` defines repository contracts.
- **Adapter layer** (`adapter/`): Framework-specific implementations. `in/web/` for Javalin controllers, `out/` for ArcadeDB, Ollama, JWT, etc.
- **Dependency rule**: Domain depends on nothing. Adapters depend on ports. Never import adapter code from domain/services.

Manual dependency wiring in `Application.java` (no Spring, no DI framework).

## Tech Stack

| Layer         | Technology                                  |
| ------------- | ------------------------------------------- |
| Language      | Java 25                                     |
| Web framework | Javalin 7.0.1                               |
| Database      | ArcadeDB 26.3.1 (document + graph + vector) |
| AI/Embeddings | Ollama (all-minilm:l6-v2, 384 dimensions)   |
| Auth          | JWT (Auth0 lib) + BCrypt                    |
| Build         | Maven with shade plugin (fat JAR)           |
| Frontend      | React 19 + TypeScript + MUI 7 + Zustand     |
| Testing       | JUnit 6, Testcontainers, WireMock, AssertJ  |
| Deployment    | Docker + Kamal                              |
| CI            | GitHub Actions                              |

## Build & Run Commands

```bash
# Backend
mvn clean package                     # Build fat JAR
mvn test                              # Run all tests (~400 tests)
mvn test -Pe2e-tests                  # Run E2E tests (requires Ollama)
mvn jacoco:report                     # Code coverage

# Frontend
cd webapp && npm start                # Dev server (webpack-dev-server)
cd webapp && npm test                 # Jest tests
cd webapp && npm run build            # Production build

# Docker
docker-compose up -d                  # Full stack: ArcadeDB + Ollama + API + Webapp
docker-compose -f docker-compose-backend.yml up -d  # Backend only
```

## Testing Conventions

- **Real implementations over mocks**: Use Testcontainers with real ArcadeDB for integration tests. Use WireMock for external HTTP. Avoid mocking repositories when possible.
- **FakeEmbeddingGenerator**: Use for deterministic vector tests (no Ollama needed).
- **Test base classes**: `ArcadeDbTestBase` for integration tests with automatic DB setup/cleanup.
- **E2E tests are optional**: Gated behind `-Pe2e-tests` profile since they require Ollama.
- **Controller tests**: Use `LoopbackJavalin.test`, which wraps `JavalinTest`, for REST endpoint testing.
- **Frontend tests**: Jest + React Testing Library + MSW for API mocking.

## Key Patterns

- **Domain events**: `LinkCreatedEvent`, `ContentDownloadCompletedEvent`, etc. Published via `SimpleEventPublisher`.
- **Command/Query objects**: Immutable records like `NewLinkCommand`, `ListLinksQuery` for input to use cases.
- **Error handling**: `LinkLiftException` hierarchy with `ErrorCode` enum. `GlobalExceptionHandler` maps to `ErrorResponse` JSON.
- **Vector search**: Content is embedded via Ollama, stored in ArcadeDB vector indexes, queried by cosine similarity.
- **Pagination**: `LinkPage` record with page/size/sortBy/sortDirection. Default sort by `extractedAt` DESC.

## API Endpoints

| Method   | Path                        | Purpose                  |
| -------- | --------------------------- | ------------------------ |
| GET      | `/up`                       | Health check             |
| PUT      | `/api/v1/link`              | Create link              |
| GET      | `/api/v1/links`             | List links (paginated)   |
| GET      | `/api/v1/link/:id`          | Get link by ID           |
| DELETE   | `/api/v1/link/:id`          | Delete link              |
| POST     | `/api/v1/user/register`     | Register                 |
| POST     | `/api/v1/user/authenticate` | Login                    |
| POST     | `/api/v1/auth/refresh`      | Refresh JWT              |
| GET/POST | `/api/v1/collections`       | List/Create collections  |
| GET      | `/api/v1/content/:id`       | Get extracted content    |
| POST     | `/api/v1/search`            | Vector similarity search |
| GET      | `/api/v1/related/:id`       | Related links (graph)    |

## Environment Variables

| Variable                     | Purpose                                                          | Default                               |
| ---------------------------- | ---------------------------------------------------------------- | ------------------------------------- |
| `LINKLIFT_JWT_SECRET`        | JWT signing secret                                               | (required)                            |
| `LINKLIFT_OLLAMA_URL`        | Ollama endpoint                                                  | `http://localhost:11434`              |
| `LINKLIFT_OLLAMA_MODEL`      | Embedding model                                                  | `all-minilm:l6-v2`                    |
| `LINKLIFT_OLLAMA_DIMENSIONS` | Vector dimensions                                                | `384`                                 |
| `LINKLIFT_WEBAPP_DIR`        | Built frontend bundle directory served at `/` (unset = API only) | (unset; the image sets `/app/webapp`) |
| `linklift.arcadedb.host`     | ArcadeDB host (JVM prop)                                         | `localhost`                           |

## Design Deltas

Design lives in `design/` (backend) and `webapp/design/` (frontend), maintained with the product-architect skill. Commits here carry `Delta-Id: <slug>` and `Delta-Phase: <event>` git trailers naming the design delta they serve. `Delta-Phase` is the lifecycle event at commit time, NOT the delta's current maturity — that lives in `design/registry.yaml` (and `webapp/design/registry.yaml`). Read history by delta with `delta-log.py <repo> --delta <slug>`.

## Code Style

- Domain entities are Java `record` types (immutable).
- Use case interfaces are single-method (`@FunctionalInterface` style).
- Controllers are thin: validate input, delegate to use case, map response.
- No annotations for DI — all wiring is explicit in `Application.java`.
- OpenRewrite is configured for automated code modernization (`rewrite.yml`).
