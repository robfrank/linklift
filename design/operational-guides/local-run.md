# Operational Guide: Local run

## Interfaces and Usage {#interfaces-and-usage}

Two ways to run the backend locally.

**Full stack with Docker Compose.** `docker-compose.yml` defines `arcadedb` (ports 2480 and 2424, root password set through `JAVA_OPTS`), `ollama` (port 11434), a one-shot `ollama-init` that pulls `all-minilm:l6-v2`, `linklift` (built from `Dockerfile.local`, port 7070) and `webapp` (port 80). `Dockerfile.local` copies `target/linklift-*.jar`, so build the JAR first:

```bash
mvn package -DskipTests
docker-compose up -d
```

**API from the IDE or the command line, dependencies in Docker.** Start only `arcadedb`, `ollama` and `ollama-init` from the same compose file, then run the shaded JAR (NF-001). The API connects to ArcadeDB on `localhost` unless `-Dlinklift.arcadedb.host=<host>` is given (RQ-153), creates the database and applies the schema scripts on start (RQ-160).

```bash
docker-compose up -d arcadedb ollama ollama-init
java -jar target/linklift-*.jar
```

## Configuration {#configuration}

See `operational-guides/configuration.md`. Outside a development environment (RQ-151) set `LINKLIFT_JWT_SECRET` to at least 32 bytes or the process refuses to start (RQ-150); the compose file already sets one for the `linklift` service.

## Verification and Smoke Tests {#verification}

- `curl -i http://localhost:7070/up` answers 200 (RQ-002).
- Register and log in through `/api/v1/auth/register` and `/api/v1/auth/login` (RQ-010, RQ-015), then call `PUT /api/v1/link` with the returned access token as `Authorization: Bearer <token>` (RQ-030); `GET /api/v1/links/{id}/content` returns the downloaded content once the asynchronous download has finished (RQ-059, NF-004).
- `mvn test` runs the automated suite and needs Docker (NF-008); `VectorSearchE2ETest` additionally pulls an Ollama image and model.

## Monitoring, Failure Modes, Rollback {#monitoring}

- API exits at start with "JWT secret not configured": see RQ-150.
- Connection refused to port 2480 at start: ArcadeDB not up or wrong `linklift.arcadedb.host`; the database credentials are fixed in code (RQ-153) and must match the ArcadeDB root password.
- Links saved but no content or search results: Ollama unreachable or model not pulled; failed embeddings are logged and content is stored without them (RQ-057).
- Local data lives in the `arcadedb_data` and `ollama_models` volumes; `docker-compose down -v` resets it.
