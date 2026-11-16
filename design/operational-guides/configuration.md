# Operational Guide: Configuration

## Interfaces and Usage {#interfaces-and-usage}

The backend reads environment variables and one JVM system property; there is no configuration file. Behaviour of each setting is owned by the cited requirement.

| Setting                      | Kind                             | Default                                 | Requirement    |
| ---------------------------- | -------------------------------- | --------------------------------------- | -------------- |
| `LINKLIFT_JWT_SECRET`        | env                              | none                                    | RQ-150         |
| `LINKLIFT_JWT_SECRET_FILE`   | env (path)                       | none                                    | RQ-150         |
| `ENVIRONMENT`                | env                              | unset                                   | RQ-151         |
| `LINKLIFT_OLLAMA_URL`        | env                              | see RQ-152                              | RQ-152         |
| `LINKLIFT_OLLAMA_MODEL`      | env                              | see RQ-152                              | RQ-152         |
| `LINKLIFT_OLLAMA_DIMENSIONS` | env                              | see RQ-152                              | RQ-071, RQ-152 |
| `linklift.arcadedb.host`     | JVM `-D` property                | see RQ-153                              | RQ-153         |
| `JAVA_OPTS`                  | env read by the image entrypoint | heap and GC flags in `Dockerfile.local` | NF-007         |

Fixed in code (no setting): HTTP port (RQ-001), ArcadeDB port, user, password and database name (RQ-153), token lifetimes (RQ-016), timeouts (RQ-050, RQ-082).

## Configuration {#configuration}

- Prefer `LINKLIFT_JWT_SECRET_FILE` pointing at a mounted secret over a literal environment value (RQ-150).
- `LINKLIFT_OLLAMA_DIMENSIONS` does not change the vector index; changing the model to one with a different dimension needs a schema change (RQ-071, RQ-163).

## Verification and Smoke Tests {#verification}

- The startup log warns `Using development JWT secret` when the development fallback is taken (RQ-150).
- After the first embedding, a dimension mismatch is logged as a warning naming the model and both sizes (RQ-071).

## Monitoring, Failure Modes, Rollback {#monitoring}

- A secret shorter than 32 bytes is ignored with a warning, then the next source is tried (RQ-150).
- With the development fallback, every restart signs with a new secret and previously issued tokens stop verifying (RQ-150, RQ-024).

## Frontend bundle {#frontend-bundle}

`LINKLIFT_WEBAPP_DIR` (env, a directory path; unset by default) names the built React bundle the server serves (RQ-SFFA-001). The image built from the root `Dockerfile` sets it (NF-SFFA-001); leave it unset for tests and local JAR runs. A value that names no usable bundle is logged as a warning at startup and the server runs without the frontend (RQ-SFFA-001).
