# Baseline Implementation

The backend as built: where the code lives, how it is assembled, how it is built and tested, and how the older documents in the repository relate to this baseline. Behaviour is owned by `requirements.md`; mechanism by `specifications.md`.

## Module and package layout {#module-layout}

One Maven module at the repository root (`pom.xml`, artifact `linklift`). Java sources: 200 files under `src/main/java`.

<!-- verify: find src/main/java -name '*.java' | wc -l == 200 -->

| Path                                                               | Contents                                                                                                              | Requirements                           |
| ------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------- | -------------------------------------- |
| `src/main/java/it/robfrank/linklift/Application.java`              | entry point and manual wiring                                                                                         | RQ-001, NF-003                         |
| `src/main/java/it/robfrank/linklift/config/`                       | `WebBuilder` (routes, plugins), `DatabaseInitializer`, `SecureConfiguration`, unused `SecurityConfiguration`          | RQ-002..RQ-005, RQ-150..RQ-153, RQ-160 |
| `src/main/java/it/robfrank/linklift/adapter/in/web/`               | Javalin controllers, `error/` (`ErrorResponse`, `GlobalExceptionHandler`), `security/` (filters, context accessor)    | RQ-010..RQ-125, RQ-130, RQ-131         |
| `src/main/java/it/robfrank/linklift/adapter/out/persistence/`      | ArcadeDB repositories, mappers, port adapters                                                                         | RQ-034, RQ-160..RQ-164                 |
| `src/main/java/it/robfrank/linklift/adapter/out/ai/`               | Ollama embedding, question-answer and (unwired) collection-summary adapters                                           | RQ-070, RQ-082                         |
| `src/main/java/it/robfrank/linklift/adapter/out/http/`             | `HttpContentDownloader`, `JsoupContentExtractor`                                                                      | RQ-050..RQ-056                         |
| `src/main/java/it/robfrank/linklift/adapter/out/content/`          | `SimpleTextSummarizer`                                                                                                | RQ-055                                 |
| `src/main/java/it/robfrank/linklift/adapter/out/security/`         | `BCryptPasswordSecurityAdapter`, `JwtTokenAdapter`                                                                    | RQ-014, RQ-019                         |
| `src/main/java/it/robfrank/linklift/adapter/out/event/`            | `SimpleEventPublisher`                                                                                                | RQ-140                                 |
| `src/main/java/it/robfrank/linklift/application/domain/model/`     | records and enums (`Link`, `Content`, `User`, `AuthToken`, `Collection`, `Note`, `Tag`, `LinkPage`, `GraphData`, ...) |                                        |
| `src/main/java/it/robfrank/linklift/application/domain/service/`   | use-case implementations                                                                                              |                                        |
| `src/main/java/it/robfrank/linklift/application/domain/event/`     | domain events                                                                                                         | RQ-141                                 |
| `src/main/java/it/robfrank/linklift/application/domain/exception/` | `LinkLiftException` hierarchy, `ErrorCode`                                                                            | RQ-132                                 |
| `src/main/java/it/robfrank/linklift/application/port/in/`          | 34 `*UseCase` interfaces plus command and query records                                                               |                                        |
| `src/main/java/it/robfrank/linklift/application/port/out/`         | 22 output-port interfaces                                                                                             |                                        |
| `src/main/resources/schema/`                                       | 12 ordered SQL scripts                                                                                                | RQ-160..RQ-164                         |
| `src/main/resources/logback.xml`                                   | JSON console logging                                                                                                  | NF-006                                 |
| `src/test/java/it/robfrank/linklift/`                              | 72 test classes                                                                                                       | see the Testing section                |

<!-- verify: ls src/main/java/it/robfrank/linklift/application/port/in/ | grep -c 'UseCase.java$' == 34 -->
<!-- verify: ls src/main/java/it/robfrank/linklift/application/port/out/ | grep -c '.java$' == 22 -->
<!-- verify: ls src/main/resources/schema/ | grep -c '.sql$' == 12 -->
<!-- verify: find src/test -name '*Test.java' | wc -l == 72 -->

## Wiring {#wiring}

`Application.start(port)` runs, in order: `DatabaseInitializer.initializeDatabase()` (RQ-160); one shared `RemoteDatabase`; repositories and their port adapters; the BCrypt and JWT adapters (the JWT secret is a static field initialised from `SecureConfiguration`, RQ-150); one shared `HttpClient` with the connect timeout of RQ-050; the downloader, extractor, summarizer and Ollama embedding adapter; a fixed thread pool sized to `Runtime.availableProcessors()`; the `SimpleEventPublisher`; the services; the event subscriptions (RQ-141); the controllers; and finally `WebBuilder` with every controller, then `app.start(port)`. The same `LinkPersistenceAdapter` instance serves as `LoadLinksPort`, `SaveLinkPort`, `UpdateLinkPort` and `DeleteLinkPort`; the same `ContentPersistenceAdapter` serves `LoadContentPort` and `SaveContentPort`; `AuthenticationService` is passed twice to `AuthenticationController` as both use cases. See ADR `adr/001-hexagonal-architecture-manual-wiring.md`.

## Build and run {#build}

| Command                   | Effect                                                                                          | Requirement |
| ------------------------- | ----------------------------------------------------------------------------------------------- | ----------- |
| `mvn clean package`       | compile (release 25), run tests, build the shaded JAR in `target/`                              | NF-001      |
| `mvn package -DskipTests` | shaded JAR without tests                                                                        | NF-001      |
| `mvn test`                | run every `*Test` class, including `VectorSearchE2ETest`; needs Docker                          | NF-008      |
| `mvn verify`              | what CI runs (`.github/workflows/ci.yml`), followed there by `mvn package -DskipTests -Pdocker` |             |
| `mvn jacoco:report`       | coverage report (the jacoco plugin is bound in `pom.xml`)                                       |             |
| `mvn rewrite:run`         | OpenRewrite recipes from `rewrite.yml` (CI workflow `.github/workflows/clean-code.yml`)         |             |
| `docker-compose up -d`    | ArcadeDB, Ollama, model pull, API and webapp (`operational-guides/local-run.md`)                |             |

Surefire and failsafe run with `--enable-preview`. Maven profiles in `pom.xml` are `native`, `docker` and `macos`; there is no profile that includes or excludes tests.

<!-- verify: grep -c 'e2e-tests' pom.xml == 0 -->

## Testing {#testing}

The suite has 500 `@Test` methods across the 72 test classes (count of lines holding only the `@Test` annotation). One test class also uses parameterised, repeated or dynamic tests, which this count leaves out.

<!-- verify: grep -rlE '@ParameterizedTest|@TestFactory|@RepeatedTest' src/test | wc -l == 1 -->
<!-- verify: grep -rhE '^[[:space:]]*@Test[[:space:]]*$' src/test | wc -l == 500 -->

| Kind             | Mechanism                                                                                                                                                                                                              | Examples                                                                                                        |
| ---------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------- |
| Domain model     | plain JUnit 6 + AssertJ                                                                                                                                                                                                | `AuthTokenTest`, `LinkTest`, `SecurityContextTest`                                                              |
| Service unit     | Mockito for ports; `FakeEmbeddingGenerator` (deterministic vectors) where embeddings matter                                                                                                                            | `NewLinkServiceTest`, `DownloadContentServiceTest`, `SearchContentServiceTest`, `BackfillEmbeddingsServiceTest` |
| Controller       | `JavalinTest` against a Javalin app registering the controller and `GlobalExceptionHandler`, use cases mocked                                                                                                          | `NewLinkControllerTest`, `ListLinksControllerTest`, `CollectionControllerTest`                                  |
| Security filters | `JavalinTest` with the real filters                                                                                                                                                                                    | `SecurityIntegrationTest`                                                                                       |
| Persistence      | Testcontainers ArcadeDB (`ArcadeDbContainer`, image `arcadedata/arcadedb-headless` at the client library's version); schema from `DatabaseInitializer.initializeSchema`; fresh database per test in `ArcadeDbTestBase` | `ArcadeLinkRepositoryTest`, `ArcadeTagRepositoryTest`, `GraphOperationsTest`                                    |
| Outbound HTTP    | WireMock                                                                                                                                                                                                               | `HttpContentDownloaderTest`, `OllamaEmbeddingAdapterTest`, `OllamaCollectionSummaryAdapterTest`                 |
| End to end       | Testcontainers ArcadeDB and real auth stack                                                                                                                                                                            | `AuthenticationIntegrationTest`                                                                                 |
| Vector E2E       | Testcontainers ArcadeDB plus `OllamaContainer` (`ollama/ollama:latest`, pulls `all-minilm:l6-v2`)                                                                                                                      | `VectorSearchE2ETest`                                                                                           |

Testcontainers is used by 11 test sources.

<!-- verify: grep -rl '@Testcontainers' src/test | wc -l == 11 -->

`FakeEmbeddingGenerator` lives in `src/test/java/it/robfrank/linklift/adapter/out/ai/FakeEmbeddingGenerator.java`; `ArcadeDbTestBase` and `ArcadeDbContainer` live in `src/test/java/it/robfrank/linklift/testcontainers/`. No test class targets `NoteController`, `TagController`, `AdminController` or `SearchContentController` directly.

<!-- verify: find src/test -name 'NoteControllerTest.java' -o -name 'TagControllerTest.java' -o -name 'AdminControllerTest.java' -o -name 'SearchContentControllerTest.java' | wc -l == 0 -->

The requirement-to-test mapping is in `requirement-checklist.md`.

## Known gaps observed while baselining {#known-gaps}

These are read from the code and were not exercised at run time; each is a candidate for a future delta, not a requirement.

- Content refresh inserts a second `Content` vertex for a `linkId` that is UNIQUE (`specifications.md#content-download`).
- `004_initial_data.sql` inserts roles and the system user without existence guards and is re-applied at every start (RQ-160); its behaviour against an existing database is untested (every persistence test starts from a fresh database).
- `Tag(name)` is UNIQUE besides `Tag(name, userId)` (RQ-162), which may stop a second user creating a tag name another user already has.
- The related-links graph fallback traverses `hasTag` while tags are stored on `HasTag` (`specifications.md#related-links`).
- `BackfillEmbeddingsService` neither updates nor marks failed a record whose text is blank but non-null, and its loop reloads such records (RQ-076).
- Extracted page URLs are never persisted on the Link, and a re-download that finds no URLs keeps stale `linksTo` edges (RQ-056).
- Registration with a null or blank `username`, `email` or `password` fails inside the `CreateUserCommand` constructor during `ctx.bodyAsClass`; how Jackson and Javalin wrap that `IllegalArgumentException`, and so which handler of RQ-131 answers, was not determined from the code (RQ-012).
- `config/deploy.yml` sets no `LINKLIFT_JWT_SECRET` (`operational-guides/deploy.md`).

## Relationship to legacy documents {#relationship-to-legacy-documents}

Status vocabulary: _still accurate_; _historical plan_ (a plan or progress note whose work is finished or abandoned, kept for history, not a description of the system); _partially accurate_ (describes the system with specific errors); _conflicts with code_. Where a legacy statement is still true, the requirement that now owns it is named. Frontend-only documents (`FRONTEND_ARCHITECTURE_REFACTOR_PLAN.md`, `FRONTEND_REFACTOR_IMPLEMENTATION_PLAN.md`) are covered by the webapp root.

<!-- external-ref -->

The root `CLAUDE.md` exists in the main checkout but is not tracked in this repository, so it is listed by name only.

| Document                                        | Area                    | Status                                      | Superseded by                                                 |
| ----------------------------------------------- | ----------------------- | ------------------------------------------- | ------------------------------------------------------------- |
| root CLAUDE.md (untracked)                      | overview, API, commands | conflicts with code                         | RQ-010, RQ-015, RQ-030, RQ-059, RQ-073, RQ-090, NF-008        |
| `ARCHITECTURE.md`                               | layering                | partially accurate                          | NF-002, NF-003, `specifications.md#layering`                  |
| `DEVELOPER_GUIDE.md`                            | setup, API examples     | partially accurate                          | NF-001, RQ-026, `operational-guides/local-run.md`             |
| `CONTRIBUTING_TESTS.md`                         | tests                   | partially accurate                          | NF-008, `#testing`                                            |
| `README.md`                                     | overview, API, config   | conflicts with code                         | RQ-026, RQ-036, RQ-152, `operational-guides/configuration.md` |
| `SECURITY.md`                                   | JWT secret              | partially accurate                          | RQ-150, RQ-151                                                |
| `ROADMAP.md`                                    | roadmap                 | historical plan                             |                                                               |
| `FEATURE_ROADMAP.md`                            | roadmap                 | historical plan                             |                                                               |
| `OLLAMA_DEPLOYMENT_PLAN.md`                     | deploy                  | historical plan                             | RQ-152, `operational-guides/deploy.md`                        |
| `VECTOR_SEARCH_IMPLEMENTATION_PLAN.md`          | vector search           | historical plan                             | RQ-070..RQ-073, RQ-163                                        |
| `VECTOR_SEARCH_FIXES_PLAN.md`                   | vector search           | historical plan                             | RQ-075, RQ-076                                                |
| `TEST_REFACTORING_PLAN.md`                      | tests                   | historical plan                             | `#testing`                                                    |
| `PHASE1_COMPARISON.md`                          | tests                   | historical plan                             |                                                               |
| `PHASE3_PROGRESS.md`                            | tests                   | historical plan                             |                                                               |
| `528-testcontainers-infrastructure.md`          | tests                   | historical plan                             | `#testing`                                                    |
| `529-refactor-service-tests.md`                 | tests                   | historical plan                             | `#testing`                                                    |
| `530-optional-e2e-tests.md`                     | tests                   | historical plan, conflicts with code        | NF-008                                                        |
| `conductor/product.md`                          | product vision          | partially accurate                          |                                                               |
| `conductor/tech-stack.md`                       | versions                | conflicts with code                         | NF-001, `#build`                                              |
| `conductor/tracks/collections_20260206/spec.md` | collections             | partially accurate (spec only partly built) | RQ-100..RQ-106                                                |
| `conductor/archive/auth_flow_20260205/spec.md`  | auth                    | historical plan                             | RQ-010..RQ-023                                                |

### Concrete conflicts {#legacy-conflicts}

Each line is a legacy statement the code contradicts; the code is authoritative.

- Root CLAUDE.md, API table: lists `/api/v1/user/register` and `/api/v1/user/authenticate`, `GET /api/v1/link/:id`, `GET /api/v1/content/:id`, `POST /api/v1/search` and `/api/v1/related/:id`. The routes are `/api/v1/auth/*` (RQ-010, RQ-015), no get-link-by-id route exists, content is `/api/v1/links/{linkId}/content` (RQ-059), search is `GET` (RQ-073) and related links is `/api/v1/links/{linkId}/related` (RQ-090). The table omits notes, tags, graph, ask, status, admin and `/metrics`.
- Root CLAUDE.md, tech stack: names Javalin 7.0.1 and ArcadeDB 26.3.1; `pom.xml` pins newer versions (`javalin.version`, `arcadedb.version`).
- Root CLAUDE.md, `CONTRIBUTING_TESTS.md`, `README.md`, `530-optional-e2e-tests.md`: E2E tests gated behind `-Pe2e-tests` and excluded from `mvn test`. No such profile exists and `VectorSearchE2ETest` runs in the default suite (NF-008).
- Root CLAUDE.md: `docker-compose -f docker-compose-backend.yml`. The file does not exist; only `docker-compose.yml` does.
- Root CLAUDE.md: "Ollama embedding/summary adapters". Content summaries come from the extractive `SimpleTextSummarizer` (RQ-055); `OllamaCollectionSummaryAdapter` is not wired (`specifications.md#layering`).
- Root CLAUDE.md: `ArcadeDbTestBase` placed with the integration tests. It lives in the `testcontainers` test package (`#testing`).
- `README.md`, `DEVELOPER_GUIDE.md`, `ARCHITECTURE.md`, `ROADMAP.md`: Java 17, 17+ or 24. The compiler release is 25 (NF-001).
- `README.md`, `DEVELOPER_GUIDE.md`, `ARCHITECTURE.md`, `conductor/tech-stack.md`: JUnit 5; `conductor/tech-stack.md` also Javalin 6.7.0 and ArcadeDB 25.12.1. `pom.xml` uses JUnit 6 and newer Javalin and ArcadeDB.
- `README.md`, `DEVELOPER_GUIDE.md`: ArcadeDB images 25.6.1 or 25.7.1 and curl examples without a bearer token. `docker-compose.yml` pins 26.11.1 and every `/api/v1/*` route except auth requires a token (RQ-026).
- `README.md`: API section lists `/up` and two link endpoints; the surface is the table in `specifications.md#http-surface`.
- `README.md`: JWT secret "auto-generated (dev)". It is generated only when the development check of RQ-151 holds; otherwise startup fails (RQ-150).
- `SECURITY.md`: development secret "deterministic per installation". It mixes in random bytes at each process start, so tokens do not survive a restart (RQ-150). The document also omits that logout revokes nothing (RQ-023) and that permissions are fixed in code (RQ-025).
- `CONTRIBUTING_TESTS.md`, `PHASE1_COMPARISON.md`: vector index type `LSM_TREE`. The index is `LSM_VECTOR` (RQ-163).
- `ARCHITECTURE.md`, `DEVELOPER_GUIDE.md`: Spring-style samples (`@Service`, `@RestController`, `@PutMapping`) and a Spring extension recommendation. There is no Spring; wiring is manual (NF-003). The `ARCHITECTURE.md` package tree shows two controllers; there are fourteen controller classes plus the error and security packages.
- `VECTOR_SEARCH_IMPLEMENTATION_PLAN.md`, `OLLAMA_DEPLOYMENT_PLAN.md`: default model `nomic-embed-text` (768 dimensions). The default is `all-minilm:l6-v2` with 384 dimensions, and a dimension mismatch only logs a warning (RQ-071, RQ-152).
- `OLLAMA_DEPLOYMENT_PLAN.md`: arcadedb-headless 25.12.1 and `ollama:latest` accessories. `config/deploy.yml` pins newer images by digest.
- `conductor/product.md`: team collaboration and link sharing. All data is per user (`specifications.md#cross-cutting`).
- `conductor/tracks/collections_20260206/spec.md`: rename, bulk add, merge and an Ollama "Generate Summary" action. None has a route (RQ-106).
- `ROADMAP.md`: collections backend "Create only". List, get, delete and membership routes exist (RQ-100 to RQ-105).

<!-- verify: find . -maxdepth 1 -name 'docker-compose-backend.yml' | wc -l == 0 -->
<!-- verify: ls src/main/java/it/robfrank/linklift/adapter/in/web/ | grep -c 'Controller.java$' == 14 -->

## Fix the red backend build (prettier, Testcontainers image, graph 401 test) (`fix-backend-build`)

## Delta Implementation: Fix the red backend build

### Resolved Document Set

<!-- GENERATED by scripts/gen-docs.py from the manifest and this delta's files; the one Resolved Document Set of a pinned delta. Do not edit. Regenerate with: python3 scripts/gen-docs.py <design-root> <slug> -->

- Baseline: `design/requirements.md`, `design/specifications.md`, `design/implementation.md`
- Delta Requirements: `design/archive/fix-backend-build/requirements.md`
- Delta Specifications: `design/archive/fix-backend-build/specifications.md`
- Delta Requirement Checklist: `design/archive/fix-backend-build/requirement-checklist.md`
- Rounds:
  - `ROUND-00`: `design/archive/fix-backend-build/phases/round-00-toolchain.md`
- Footprint (from the manifest):
  - assumes: `RQ-026`, `RQ-045`
  - changes: `NF-008` (modify), `NF-FBB-001` (add)
- Development Rules: `java-developer`
- Project Standards: CLAUDE.md

### Module & File Layout

| File | Responsibility | Key Functions | Entry Points |
|------|----------------|---------------|--------------|
| `src/test/java/it/robfrank/linklift/adapter/in/web/LoopbackJavalin.java` <!-- planned-artifact --> | Bind a JavalinTest app to loopback, then run it (spec `#loopback-test-apps`) | `test(Javalin, TestCase)` | JavalinTest-based tests |
| `src/test/java/it/robfrank/linklift/adapter/in/web/LoopbackJavalinTest.java` <!-- planned-artifact --> | Tests the helper (rows `T-loopback-helper`, `T-loopback-stray-listener`) | — | Surefire |
| `src/test/java/it/robfrank/linklift/adapter/in/web/AuthenticationControllerTest.java` | Call `LoopbackJavalin.test`; drop the `JavalinTest` import | — | Surefire |
| `src/test/java/it/robfrank/linklift/adapter/in/web/CollectionControllerTest.java` | Call `LoopbackJavalin.test`; drop the `JavalinTest` import | — | Surefire |
| `src/test/java/it/robfrank/linklift/adapter/in/web/DeleteContentControllerTest.java` | Call `LoopbackJavalin.test`; drop the `JavalinTest` import | — | Surefire |
| `src/test/java/it/robfrank/linklift/adapter/in/web/GetRelatedLinksControllerTest.java` | Call `LoopbackJavalin.test`; drop the `JavalinTest` import | — | Surefire |
| `src/test/java/it/robfrank/linklift/adapter/in/web/LinkControllerTest.java` | Call `LoopbackJavalin.test`; drop the `JavalinTest` import | — | Surefire |
| `src/test/java/it/robfrank/linklift/adapter/in/web/ListLinksControllerTest.java` | Call `LoopbackJavalin.test`; drop the `JavalinTest` import | — | Surefire |
| `src/test/java/it/robfrank/linklift/adapter/in/web/NewLinkControllerTest.java` | Call `LoopbackJavalin.test`; drop the `JavalinTest` import | — | Surefire |
| `src/test/java/it/robfrank/linklift/adapter/in/web/ObservabilityTest.java` | Call `LoopbackJavalin.test`; drop the `JavalinTest` import | — | Surefire |
| `src/test/java/it/robfrank/linklift/adapter/in/web/security/SecurityIntegrationTest.java` | Call `LoopbackJavalin.test`; drop the `JavalinTest` import | — | Surefire |
| `src/test/java/it/robfrank/linklift/testcontainers/ArcadeDbContainer.java` | Full ArcadeDB image (spec `#test-container-image`) | — | `ArcadeDbTestBase` |
| `src/test/java/it/robfrank/linklift/config/DatabaseInitializerTest.java` | Full ArcadeDB image | — | Surefire |
| `src/test/java/it/robfrank/linklift/integration/AuthenticationIntegrationTest.java` | Full ArcadeDB image | — | Surefire |
| `src/test/java/it/robfrank/linklift/adapter/out/persistence/ArcadeAuthTokenRepositoryTest.java` | Full ArcadeDB image at the library version | — | Surefire |
| `CLAUDE.md` | Controller-test line names `LoopbackJavalin.test` (spec `#loopback-test-apps`) | — | — |
| `src/test/java/it/robfrank/linklift/adapter/in/web/AskControllerTest.java` | prettier layout (spec `#java-formatting`) | — | Surefire |
| `src/test/java/it/robfrank/linklift/application/domain/service/AskQuestionServiceTest.java` | prettier layout | — | Surefire |

No file under `src/main` is in the plan. Supersedes baseline "image `arcadedata/arcadedb-headless` at the client library's version" (`implementation.md#testing`, Persistence row): the image is `arcadedata/arcadedb` at the client library's version (NF-008).

### Implementation Rounds

| Round ID  | File | Summary | Goal | Requirement IDs | Depends on |
|-----------|------|---------|------|-----------------|------------|
| `ROUND-00` | `phases/round-00-toolchain.md` | `summaries/round-00-toolchain-summary.md` | Every java-developer gate except lint, which the owner runs, green: prettier layout for the Ask tests, the full image, loopback test apps | `NF-008`, `NF-FBB-001` | none |

### Testing Plan

Behavioral, per the java-developer rule. This table is the only place the delta's tests are stated. The gate commands are the rule's `### Gate Commands`; the local build commands are in `CLAUDE.md` § Build & Run Commands.

| Test | Requirement IDs | Case | Assertion |
|------|-----------------|------|-----------|
| `T-format-check` | `NF-FBB-001` | java-developer `format-check` gate over `src/main/java src/test/java` | exits 0; prints no file |
| `T-image-sources` | `NF-008` | `grep -rn -e arcadedb-headless -e arcadedata/arcadedb:latest src/test/java` | no match |
| `T-image-full` | `NF-008` | `grep -rn 'arcadedata/arcadedb:" + Constants.getRawVersion()' src/test/java` | at least one match in each of `ArcadeDbContainer`, `DatabaseInitializerTest`, `AuthenticationIntegrationTest`, `ArcadeAuthTokenRepositoryTest` |
| `T-loopback-helper` | `NF-008` | `LoopbackJavalinTest`: run an app with one `GET /ping` route through `LoopbackJavalin.test` | inside the test case the Jetty `ServerConnector` host is `127.0.0.1` and `client.get("/ping")` answers 200 with body `pong` |
| `T-loopback-stray-listener` | `NF-008` | `LoopbackJavalinTest`: inside `LoopbackJavalin.test` with the `/ping` app, bind a reuse-address `ServerSocket` on `127.0.0.1` at `app.port()` that answers any request 404, then `client.get("/ping")` | the bind fails with `BindException`, or `/ping` answers 200 `pong` |
| `T-loopback-callers` | `NF-008` | `grep -rn 'JavalinTest.test(' src/test/java`; `grep -rl 'import io.javalin.testtools.JavalinTest;' src/test/java` | each: exactly one match, in `src/test/java/it/robfrank/linklift/adapter/in/web/LoopbackJavalin.java` <!-- planned-artifact --> |
| `T-graph-401-full-suite` | `NF-008`, `RQ-026` | `ListLinksControllerTest.getGraph_shouldReturn401_whenUserNotAuthenticated` in each of three consecutive full `./mvnw test` runs | passes (401, message `Unauthorized access`); a 404: spec `#loopback-test-apps` |
| `T-full-suite` | `NF-008` | java-developer `test` gate with Docker running (an Ollama-pull failure: round 00 § Gates) | exit 0; Surefire reports 0 failures and 0 errors |

Delta acceptance: every row of the round 00 gate table except lint, which the owner runs, reads green locally. CI's `build` job runs `mvn -ntp verify`, which includes `VectorSearchE2ETest`'s Ollama pull, so CI going green after merge is the user's observation, not this delta's acceptance.
