# Baseline Specifications

How the requirements in `requirements.md` are realised in the code as it stands. Behaviour and literal values belong to the requirements; this document names the mechanism and cites the ids. All code paths are relative to the repository root; `src/main/java/it/robfrank/linklift/` is abbreviated `<pkg>/` only in prose, never inside a backticked path.

## System context {#system-context}

The backend is a single Java process (Javalin HTTP server) that talks to two external services over HTTP: ArcadeDB (document, graph and vector store, reached with the `arcadedb-network` remote client on port 2480) and Ollama (embeddings and text generation). Web clients reach it on port 7070 (RQ-001); the React webapp is specified in its own design root. Outbound HTTP to arbitrary web pages is used for content download (RQ-050).

```
client ──HTTP──▶ Javalin (WebBuilder routes, filters, GlobalExceptionHandler)
                   │ controllers (adapter/in/web)
                   ▼
            use cases (application/port/in) ◀── services (application/domain/service)
                   │ output ports (application/port/out)
                   ▼
   ArcadeDB adapters (adapter/out/persistence) ── RemoteDatabase ──▶ ArcadeDB :2480
   Ollama adapters  (adapter/out/ai)          ── java.net.http   ──▶ Ollama :11434
   HTTP/Jsoup       (adapter/out/http)        ── java.net.http   ──▶ the web
```

## Layering and wiring {#layering}

The code follows ports and adapters (ADR `adr/001-hexagonal-architecture-manual-wiring.md`). Input ports are the `*UseCase` interfaces and `*Command`/`*Query` records in `src/main/java/it/robfrank/linklift/application/port/in/`; output ports are the interfaces in `src/main/java/it/robfrank/linklift/application/port/out/`; services in `src/main/java/it/robfrank/linklift/application/domain/service/` implement the use cases. The one domain-to-adapter import is described by NF-002. `src/main/java/it/robfrank/linklift/Application.java` builds every object by hand (NF-003) and hands the controllers to `src/main/java/it/robfrank/linklift/config/WebBuilder.java`, which registers routes inside `Javalin.create(...)`.

Classes present but not wired by `Application.java`: `MergeCollectionsService`, `OllamaCollectionSummaryAdapter`, `LinkContentExtractorService`, `LinksController`, `SecurityConfiguration` (RQ-106 records the externally visible consequence for collections).

## HTTP surface {#http-surface}

`WebBuilder.build()` registers `/up` and `/metrics` (RQ-002, RQ-004), the plain-text `/` route only when no frontend bundle is configured (RQ-003; with a bundle, `#frontend-bundle`), the CORS plugin with `anyHost()` (RQ-005), Javalin dev logging, the Micrometer plugin bound to a `PrometheusMeterRegistry` tagged `app=linklift`, the exception handlers (RQ-130, RQ-131), and the deferred controller registrations.

| Method        | Path                                      | Controller method                                          | Guard (before-filters)            | Requirement    |
| ------------- | ----------------------------------------- | ---------------------------------------------------------- | --------------------------------- | -------------- |
| POST          | `/api/v1/auth/register`                   | `AuthenticationController.register`                        | none                              | RQ-010         |
| POST          | `/api/v1/auth/login`                      | `AuthenticationController.login`                           | none                              | RQ-015         |
| POST          | `/api/v1/auth/refresh`                    | `AuthenticationController.refreshToken`                    | none                              | RQ-021         |
| POST          | `/api/v1/auth/logout`                     | `AuthenticationController.logout`                          | none                              | RQ-023         |
| PUT           | `/api/v1/link`                            | `NewLinkController.processLink`                            | auth + `CREATE_LINK`              | RQ-030         |
| GET           | `/api/v1/links`                           | `ListLinksController.listLinks`                            | auth + `READ_OWN_LINKS`           | RQ-035         |
| GET           | `/api/v1/graph`                           | `ListLinksController.getGraph`                             | auth + `READ_OWN_LINKS`           | RQ-045         |
| PATCH         | `/api/v1/links/{id}`                      | `LinkController.updateLink`                                | auth + `UPDATE_OWN_LINKS`         | RQ-041         |
| PATCH         | `/api/v1/links/{id}/status`               | `LinkController.updateLinkStatus`                          | auth + `UPDATE_OWN_LINKS`         | RQ-042         |
| DELETE        | `/api/v1/links/{id}`                      | `LinkController.deleteLink`                                | auth + `DELETE_OWN_LINKS`         | RQ-044         |
| GET           | `/api/v1/links/{linkId}/content`          | `GetContentController.getContent`                          | auth + `READ_OWN_LINKS`           | RQ-059         |
| POST          | `/api/v1/links/{linkId}/content/refresh`  | `GetContentController.refreshContent`                      | `UPDATE_OWN_LINKS` (implies auth) | RQ-060         |
| DELETE        | `/api/v1/links/{linkId}/content`          | `DeleteContentController.deleteContent`                    | auth + `DELETE_OWN_LINKS`         | RQ-061         |
| GET           | `/api/v1/search`                          | `SearchContentController.search`                           | auth + `READ_OWN_LINKS`           | RQ-073         |
| GET           | `/api/v1/links/{linkId}/related`          | `GetRelatedLinksController.getRelatedLinks`                | auth + `READ_OWN_LINKS`           | RQ-090         |
| POST          | `/api/v1/ask`                             | `AskController.ask`                                        | auth + `READ_OWN_LINKS`           | RQ-080         |
| POST          | `/api/v1/admin/backfill-embeddings`       | `AdminController.backfillEmbeddings`                       | auth + `ADMIN_ACCESS`             | RQ-075         |
| GET, POST     | `/api/v1/collections`                     | `CollectionController.listCollections`, `createCollection` | auth + `CREATE_COLLECTION`        | RQ-100, RQ-101 |
| GET, DELETE   | `/api/v1/collections/{id}`                | `getCollection`, `deleteCollection`                        | auth + `DELETE_OWN_COLLECTION`    | RQ-102, RQ-104 |
| POST          | `/api/v1/collections/{id}/links`          | `addLinkToCollection`                                      | auth + `UPDATE_OWN_COLLECTION`    | RQ-103         |
| DELETE        | `/api/v1/collections/{id}/links/{linkId}` | `removeLinkFromCollection`                                 | auth + `UPDATE_OWN_COLLECTION`    | RQ-104         |
| GET, POST     | `/api/v1/links/{linkId}/notes`            | `NoteController.getNotes`, `createNote`                    | auth                              | RQ-110, RQ-111 |
| PATCH, DELETE | `/api/v1/links/{linkId}/notes/{noteId}`   | `updateNote`, `deleteNote`                                 | auth                              | RQ-112         |
| GET, POST     | `/api/v1/tags`                            | `TagController.listTags`, `createTag`                      | auth                              | RQ-120, RQ-121 |
| DELETE        | `/api/v1/tags/{tagId}`                    | `deleteTag`                                                | auth                              | RQ-122         |
| GET, POST     | `/api/v1/links/{linkId}/tags`             | `getTagsForLink`, `addTagToLink`                           | auth                              | RQ-123, RQ-124 |
| DELETE        | `/api/v1/links/{linkId}/tags/{tagId}`     | `removeTagFromLink`                                        | auth                              | RQ-124         |
| GET           | `/api/v1/links/{linkId}/tags/suggest`     | `suggestTags`                                              | auth                              | RQ-125         |

Javalin before-filters match by path regardless of method. Consequently the collection permission filters apply to every method on their path: listing collections also passes the `CREATE_COLLECTION` filter and reading one passes the `DELETE_OWN_COLLECTION` filter. Under RQ-025 every authenticated user holds both, so this has no visible effect today. The link, content and status filters branch on `ctx.method()` and do not have this property.

## Authentication and authorization {#auth}

`JwtAuthenticationHandler` is registered as a global before-filter. It extracts the bearer token, and `AuthorizationService.createSecurityContext` validates it through `JwtTokenPort` (`JwtTokenAdapter`, Auth0 `java-jwt`, `Algorithm.HMAC256`, issuer check), reloads the user through `LoadUserPort`, and loads permissions through `LoadUserRolesPort`. The result, authenticated or anonymous, is stored on the Javalin context under `security.context` by `adapter/in/web/security/SecurityContext` (RQ-024). `RequireAuthentication` and `RequirePermission.any(...)` read it and throw `AuthenticationException` with `UNAUTHORIZED_ACCESS` or `INSUFFICIENT_PERMISSIONS` (RQ-026). The `LoadUserRolesPort` implementation, `UserRolePersistenceAdapter`, returns a constant list and does not touch the database (RQ-025, RQ-075).

`AuthenticationService` implements both `AuthenticateUserUseCase` and `RefreshTokenUseCase` (RQ-015 to RQ-022). Token lifetimes are constants in that class (RQ-016). Refresh tokens are stored through `AuthTokenPort` (`AuthTokenPersistenceAdapter` → `ArcadeAuthTokenRepository`) and validity is `AuthToken.isValid()` (not revoked, `usedAt` null, not expired), so a refresh token is single-use (RQ-021, RQ-022). The client address is resolved by the same header order in `AuthenticationController` and `JwtAuthenticationHandler` (RQ-020).

`CreateUserService` validates and stores users (RQ-010 to RQ-013); normalisation happens in the `CreateUserCommand` and `AuthenticateUserCommand` compact constructors (RQ-011). Password hashing and the strength rule are in `BCryptPasswordSecurityAdapter` (`at.favre.lib:bcrypt`) (RQ-012, RQ-014). The JWT secret comes from `SecureConfiguration.getJwtSecret()`, read once into a static field of `Application` (RQ-150, RQ-151); see ADR `adr/004-jwt-bcrypt-authentication.md`.

## Links {#links}

`NewLinkController` validates the body with Javalin's `bodyValidator` (RQ-031) and calls `NewLinkService`, which checks the URL with `new URI(url).toURL()`; that call's `IllegalArgumentException` for relative URIs is not caught, which is why RQ-032 has a 500 branch. The uniqueness check is a global `SELECT FROM Link WHERE url = ?` (RQ-033). `ArcadeLinkRepository.saveLinkForUser` inserts the `Link` vertex, the `OwnsLink` edge and the `Domain` upsert with its `belongsToDomain` edge in one transaction (RQ-034). It also writes `fullText`, `summary`, `imageUrl` and `extractedUrls`, properties the schema does not declare (ArcadeDB accepts undeclared properties).

`ListLinksController` builds a `ListLinksQuery` (defaults in its compact constructor and factory methods, RQ-036); `ListLinksService` validates it (RQ-037) and publishes `LinksQueryEvent` (RQ-040). `ArcadeLinkRepository.findLinksWithPaginationForUser` expands `out('OwnsLink')` from the user, applies a parameterised `FilterClause` and a `HasTag` containment test for `tagId` (RQ-038, NF-005), and uses `ORDER BY <field> <dir> SKIP <offset> LIMIT <size>` with the field taken from a whitelist. `LinkPage`'s compact constructor derives the paging flags (RQ-039).

`UpdateLinkService` and `UpdateLinkStatusService` load the link through `findLinkByIdAndUserId`, a single ownership-scoped query (RQ-041 to RQ-043); `ArcadeLinkRepository.updateLink` writes `title`, `description`, `readStatus`, `archived` and `favorited` only. `DeleteLinkService` checks `userOwnsLink` and then runs `DELETE FROM Link WHERE id = ?` (RQ-044). `GetGraphService` delegates to `ArcadeLinkRepository.getGraphData` (RQ-045).

## Content download pipeline {#content-download}

`Application.configureEventSubscribers` subscribes `DownloadContentService.downloadContentAsync` to `LinkCreatedEvent` (RQ-034, RQ-141). The pipeline in `DownloadContentService`:

1. Publish `ContentDownloadStartedEvent` (RQ-050).
2. `HttpContentDownloader.downloadContent` (`java.net.http.HttpClient.sendAsync`, shared client built in `Application` with the connect timeout) with its own retry, wrapped in the service's delayed retry (RQ-050, RQ-051).
3. Size check (RQ-052).
4. `JsoupContentExtractor.extractMetadata` (RQ-054, RQ-056) and `SimpleTextSummarizer.generateSummary` (RQ-055); a failure here is logged and the pipeline continues without metadata.
5. Only when the extracted URL list is non-empty: build an in-memory `Link` carrying the list, call `UpdateLinkPort.updateLink` (which writes `title`, `description`, `readStatus`, `archived` and `favorited` only, so the list is not persisted) and `SaveLinkPort.syncLinkConnections`, which deletes the link's outgoing `linksTo` edges and recreates them from that in-memory list; `ArcadeLinkRepository.syncLinkConnections` also returns early on an empty list (RQ-056).
6. `EmbeddingGenerator.generateEmbedding` on the extracted main text (RQ-057).
7. `SaveContentPort.saveContent`, `createHasContentEdge`, publish `ContentDownloadCompletedEvent` (RQ-053).
8. On any failure: `handleDownloadFailure` publishes `ContentDownloadFailedEvent` and tries to save a `FAILED` record (RQ-058).

The work after step 2 runs on the HTTP client's completion threads, not on the request thread (NF-004). `Content(linkId)` is UNIQUE (RQ-162) and `ArcadeContentRepository.save` always inserts a new vertex, so a refresh (RQ-060) of a link that already has a Content record attempts a second insert for the same `linkId`; this baseline records the mechanism and makes no claim about the outcome (see `implementation.md#known-gaps`).

The JSON shapes of RQ-063 and RQ-064 are the `@JsonProperty` components of the `Link` and `Content` records; enums serialise by name.

`GetContentService`, `DeleteContentService` and `DownloadContentService.refreshContent` take only a link id (RQ-059 to RQ-062). `refreshContent` loads the link through `LinkPersistenceAdapter.getLinkById`, which throws `LinkNotFoundException` for an unknown id (RQ-060).

## Embeddings and vector search {#vector-search}

`OllamaEmbeddingAdapter` implements `EmbeddingGenerator` (RQ-070, RQ-071); see ADR `adr/003-ollama-embeddings-vector-index.md`. `ContentMapper.mapToVertex` writes the embedding as a float list and sets `needsEmbedding`, substituting the zero placeholder when there is none; `mapFromMap` turns an all-zero vector back into null (RQ-072).

`ArcadeContentRepository.findSimilar` serves search, ask, related links and tag suggestions. It first collects the caller's owned link ids (`SELECT expand(out('OwnsLink').id) FROM User WHERE id = ?`), returns nothing when there are none, then runs `SELECT expand(vectorNeighbors('Content[embedding]', <vector literal>, <fetch>))` with `fetch = min(max(limit,1) * 5, 1000)` and keeps, in neighbour order, results whose `linkId` is owned until `limit` are collected. The over-fetch exists because the ArcadeDB version in use cannot filter a `vectorNeighbors()` result in SQL; the vector is inlined as a literal after the finiteness check of NF-005. Rows that cannot be mapped are logged and skipped. `SearchContentService` applies the limit rules (RQ-073); `SearchContentController` handles the empty query (RQ-074).

`BackfillEmbeddingsService` implements RQ-076 using `LoadContentPort.findContentsWithoutEmbeddings` (`needsEmbedding = true AND textContent IS NOT NULL LIMIT ?`) and an `AtomicBoolean` guard. `AdminController` submits `backfill()` to the shared fixed thread pool (sized to the available processors) and answers 202, but the route is unreachable under RQ-075.

## Question answering {#ask}

`AskQuestionService` embeds the question, calls `findSimilar` with the top-k constant, batch-loads the links with `LoadLinksPort.findLinksByIds`, builds a `Source:/URL:/Content:` context and calls `QuestionAnswerPort.generateAnswer` (RQ-080 to RQ-082). `OllamaQuestionAnswerAdapter` builds the prompt, truncates the context, applies the request timeout and converts failures into fixed answer strings (RQ-082, RQ-083).

## Related links {#related-links}

`GetRelatedLinksService` tries the vector path through `findSimilar` and falls back to `ArcadeLinkRepository.getRelatedLinks`, a `unionall(out('belongsToDomain').in('belongsToDomain'), out('hasTag').in('hasTag'))` expansion filtered by `in('OwnsLink').id CONTAINS ?` with `LIMIT 10` (RQ-090). Tags are attached with the edge type `HasTag` (RQ-124) while this fallback traverses `hasTag`; both type names exist in the schema (RQ-161).

## Collections, notes and tags {#collections-notes-tags}

Collections: `CreateCollectionService`, `ListCollectionsService`, `GetCollectionService`, `AddLinkToCollectionService`, `RemoveLinkFromCollectionService`, `DeleteCollectionService` over `CollectionRepository` (`CollectionPersistenceAdapter` → `ArcadeCollectionRepository`) (RQ-100 to RQ-106). Ownership is the `userId` property on the `Collection` vertex, compared in each service; a mismatch raises `LinkLiftException` with code `UNAUTHORIZED` (RQ-105). Membership is the `ContainsLink` edge with `addedAt = SYSDATE()`.

Notes: `CreateNoteService` (link ownership through `LoadLinksPort.userOwnsLink`), `UpdateNoteService`, `DeleteNoteService`, `GetNotesForLinkService` over `NoteRepository` (`NotePersistenceAdapter` → `ArcadeNoteRepository`) (RQ-110 to RQ-112). Notes are stand-alone `Note` vertices keyed by `linkId` and `userId` properties, with no edge to the link. `Note.userId` is `@JsonIgnore` (RQ-110).

Tags: `CreateTagService`, `DeleteTagService`, `ListTagsService`, `GetTagsForLinkService`, `AddTagToLinkService`, `RemoveTagFromLinkService`, `SuggestTagsService` over `TagRepository` (`TagPersistenceAdapter` → `ArcadeTagRepository`) (RQ-120 to RQ-125). `Tag`'s compact constructor normalises the name; `Tag.userId` is `@JsonIgnore`. `ArcadeTagRepository.addTagToLink` counts existing `HasTag` edges before creating one (RQ-124). The schema carries both a `Tag(name)` UNIQUE index (script 006) and a `Tag(name, userId)` UNIQUE index (script 012) (RQ-162); whether two users can hold tags of the same name is therefore decided by the database, not by `CreateTagService`, and this baseline does not assert the outcome.

## Error handling {#error-handling}

Domain errors extend `LinkLiftException` and carry an `ErrorCode` (`src/main/java/it/robfrank/linklift/application/domain/exception/ErrorCode.java`, RQ-132). `GlobalExceptionHandler.configure` registers one handler per exception class plus `LinkLiftException` and `Exception` fallbacks (RQ-131) and renders `ErrorResponse`, a record annotated `@JsonInclude(NON_NULL)` (RQ-130). Three paths bypass `ErrorResponse`: Javalin's own body-validator response (RQ-031), and the plain-text 400 answers written directly by `ListLinksController`, `LinkController` and `SearchContentController` (RQ-038, RQ-042, RQ-074). `CollectionController` and `GetRelatedLinksController` also write a 401 `ErrorResponse` themselves when the user id is null; the before-filters reject such requests first (RQ-026).

## Domain events {#events}

`DomainEventPublisher` is implemented by `SimpleEventPublisher`, a `CopyOnWriteArrayList` of typed subscribers (RQ-140). Event types: `LinkCreatedEvent`, `LinksQueryEvent`, `ContentDownloadStartedEvent`, `ContentDownloadCompletedEvent`, `ContentDownloadFailedEvent` in `src/main/java/it/robfrank/linklift/application/domain/event/`, plus the records `CreateUserService.UserCreatedEvent`, `AuthenticationService.UserAuthenticatedEvent` and `AuthenticationService.TokenRefreshedEvent` (RQ-018, RQ-021, RQ-142). Subscriptions are made once in `Application.configureEventSubscribers` (RQ-141). Because delivery is synchronous, a subscriber exception propagates to the publisher's caller.

## Persistence and schema {#persistence}

ArcadeDB is the only store (ADR `adr/002-arcadedb-multi-model-store.md`). `DatabaseInitializer` creates the database and applies `src/main/resources/schema/001_create_link_table.sql` through `src/main/resources/schema/012_create_tags.sql` (RQ-160 to RQ-164). Most statements use `IF NOT EXISTS`; the inserts in `src/main/resources/schema/004_initial_data.sql` do not. Graph relationships in use:

| Edge              | From → To         | Created by                                       | Requirement    |
| ----------------- | ----------------- | ------------------------------------------------ | -------------- |
| `OwnsLink`        | User → Link       | `ArcadeLinkRepository.saveLinkForUser`           | RQ-034         |
| `belongsToDomain` | Link → Domain     | `ArcadeLinkRepository.saveLinkForUser`           | RQ-034, RQ-090 |
| `HasContent`      | Link → Content    | `ArcadeContentRepository.createHasContentEdge`   | RQ-053         |
| `linksTo`         | Link → Link       | `ArcadeLinkRepository.syncLinkConnections`       | RQ-045, RQ-056 |
| `ContainsLink`    | Collection → Link | `ArcadeCollectionRepository.addLinkToCollection` | RQ-103         |
| `HasTag`          | Link → Tag        | `ArcadeTagRepository.addTagToLink`               | RQ-124         |

`HasToken`, `HasRole` (only the seed row of RQ-164), `hasTag`, `inCollection` are declared but no production code creates them. Timestamps are truncated to seconds in the domain records and written as `yyyy-MM-dd HH:mm:ss` strings where the repositories format them.

## Configuration {#configuration}

Configuration is read from environment variables through `SecureConfiguration` (RQ-150 to RQ-152) and from the JVM property `linklift.arcadedb.host` in `Application.start` (RQ-153). There is no configuration file. The operator view is in `operational-guides/configuration.md`.

## Observability {#observability}

Metrics: Micrometer Prometheus registry with JVM binders, served at `/metrics` (RQ-004). Logs: `src/main/resources/logback.xml` (NF-006); every domain event is logged by its subscriber (RQ-141). Health: `/up` (RQ-002), used as the Kamal proxy health check (`operational-guides/deploy.md`).

## Cross-cutting notes {#cross-cutting}

- Data isolation is per user through `OwnsLink` and `userId` properties; the exceptions are the content routes (RQ-062), the global URL uniqueness (RQ-033) and collection membership (RQ-103).
- Secrets: the ArcadeDB credentials are literals in `Application.java` (RQ-153); the JWT secret is externalised (RQ-150).
- No boundary-level decision beyond the four ADRs in `adr/` is visible in the code.

## Test container image {#test-container-image}

The test sources name the ArcadeDB image as a string literal in each container declaration. At the delta's baseline revision, four of them name an image other than the one NF-008 requires (three the headless image, one the `latest` tag)
<!-- verify: git grep -l -e arcadedb-headless -e arcadedata/arcadedb:latest c0f2fcedba2103e638239ebfd61ce2a7765fb8d3 -- src/test/java | wc -l == 4 -->
<!-- blind spot: only string literals in src/test/java at that revision; an image assembled from parts or read from a resource file is not matched. -->
: `src/test/java/it/robfrank/linklift/testcontainers/ArcadeDbContainer.java` (the shared container behind `ArcadeDbTestBase`), `src/test/java/it/robfrank/linklift/config/DatabaseInitializerTest.java`, `src/test/java/it/robfrank/linklift/integration/AuthenticationIntegrationTest.java` and `src/test/java/it/robfrank/linklift/adapter/out/persistence/ArcadeAuthTokenRepositoryTest.java`. Each literal becomes `"arcadedata/arcadedb:" + Constants.getRawVersion()`, the form the other persistence tests already use, so the tag follows the ArcadeDB dependency in `pom.xml` (NF-008). No separate image property is introduced and the library version is not pinned. `ArcadeDbContainer` keeps its environment, user override and readiness wait.

## Loopback test apps {#loopback-test-apps}

A new test helper `LoopbackJavalin` (package `it.robfrank.linklift.adapter.in.web`, under `src/test/java`) is the single place JavalinTest-based tests start an app. `LoopbackJavalin.test(app, testCase)` sets `app.unsafe.jetty.host` to the loopback address of NF-008 and then calls `JavalinTest.test(app, testCase)`. Javalin creates its Jetty server lazily on `start`, and JavalinTest starts the app with `app.start(0)`, so the host is in force when the connector binds; JavalinTest's own client already sends to the loopback origin
(`HttpClient.origin` is `http://127.0.0.1:<port>` in javalin-testtools 7.2.4, read from its sources jar) <!-- external-ref -->.

Every `JavalinTest.test(` call in the test sources is replaced by `LoopbackJavalin.test(` with the same arguments, and each caller drops its then-unused `import io.javalin.testtools.JavalinTest;`. At the delta's baseline revision there are 50 calls
<!-- verify: git grep -n 'JavalinTest.test(' c0f2fcedba2103e638239ebfd61ce2a7765fb8d3 -- src/test/java | wc -l == 50 -->
in 9 test classes
<!-- verify: git grep -l 'JavalinTest' c0f2fcedba2103e638239ebfd61ce2a7765fb8d3 -- src/test/java | wc -l == 9 -->
<!-- blind spot: matches the literal call text only; a static import of JavalinTest.test would be missed (none exists: grep -rn 'import static io.javalin' src/test/java returns nothing). -->
, including `ObservabilityTest`, whose app comes from `WebBuilder.build()`; the helper sets the host on the built app, so `WebBuilder` is not in the file plan. After the change the only `JavalinTest.test(` text and the only `JavalinTest` import are in `LoopbackJavalin`, whose comments do not contain the call text. The controller-test line of `CLAUDE.md` § Testing Conventions becomes "Use `LoopbackJavalin.test`, which wraps `JavalinTest`, for REST endpoint testing".

The cause of the full-suite 404 has not been diagnosed. Before the change the graph test failed with 404 in the intent's local full run (JDK 27) and passed in the gate's full run of the same tree (no failures, only the six image-pull errors), so its failure rate is unknown and passing full runs alone do not show the cause is fixed. The hypothesis is that another listener on `127.0.0.1` at the app's port takes the client's connections while the app is on the wildcard address; on macOS a socket probe showed a reuse-address bind to `127.0.0.1:<p>` beside a dual-stack wildcard listener on `<p>` succeeding and receiving the connection, and the same bind beside a `127.0.0.1:<p>` listener failing with `EADDRINUSE`. `T-loopback-stray-listener` tests that mechanism directly. If the graph test still answers 404 under loopback, the hypothesis is wrong: the implementer narrows the full suite to the classes that run before it until the 404 reproduces, fixes that test-side isolation with no `src/main` change, so NF-008's full-suite clause holds, and records the result, or the gap if no cause is found, in the round summary.

## Java formatting {#java-formatting}

`AskControllerTest` and `AskQuestionServiceTest` are rewritten by prettier with the plugin and config of NF-FBB-001; the change is layout only. The CI enforcement NF-FBB-001 states is already wired: both workflows run the pre-commit action in `check`
<!-- verify: grep -l 'pre-commit/action' .github/workflows/ci.yml .github/workflows/clean-code.yml | wc -l == 2 -->
and gate `build` on it
<!-- verify: grep -l 'needs: check' .github/workflows/ci.yml .github/workflows/clean-code.yml | wc -l == 2 -->
<!-- blind spot: text match in the two workflow files only; other workflows are not read. -->
, so the workflows are not in the file plan.

## New surface

- `src/test/java/it/robfrank/linklift/adapter/in/web/LoopbackJavalin.java` <!-- planned-artifact -->: test-only class `LoopbackJavalin` with `public static void test(Javalin app, io.javalin.testtools.TestCase testCase)`; binds the app to loopback, then runs JavalinTest (NF-008).
- `src/test/java/it/robfrank/linklift/adapter/in/web/LoopbackJavalinTest.java` <!-- planned-artifact -->: JUnit class `LoopbackJavalinTest`, the test of `LoopbackJavalin` (NF-008).

## New surface: Fix the red backend build (prettier, Testcontainers image, graph 401 test) (`fix-backend-build`)

<!-- generated by the fold: the delta wrote no specification for these names. Refine in place. -->

- `src/test/java/it/robfrank/linklift/adapter/in/web/AuthenticationControllerTest.java` (NF-008, NF-FBB-001)
- `src/test/java/it/robfrank/linklift/adapter/in/web/CollectionControllerTest.java` (NF-008, NF-FBB-001)
- `src/test/java/it/robfrank/linklift/adapter/in/web/DeleteContentControllerTest.java` (NF-008, NF-FBB-001)
- `src/test/java/it/robfrank/linklift/adapter/in/web/GetRelatedLinksControllerTest.java` (NF-008, NF-FBB-001)
- `src/test/java/it/robfrank/linklift/adapter/in/web/LinkControllerTest.java` (NF-008, NF-FBB-001)
- `src/test/java/it/robfrank/linklift/adapter/in/web/ListLinksControllerTest.java` (NF-008, NF-FBB-001)
- `src/test/java/it/robfrank/linklift/adapter/in/web/NewLinkControllerTest.java` (NF-008, NF-FBB-001)
- `src/test/java/it/robfrank/linklift/adapter/in/web/ObservabilityTest.java` (NF-008, NF-FBB-001)
- `src/test/java/it/robfrank/linklift/adapter/in/web/security/SecurityIntegrationTest.java` (NF-008, NF-FBB-001)
- `src/test/java/it/robfrank/linklift/adapter/in/web/AskControllerTest.java` (NF-008, NF-FBB-001)
- `src/test/java/it/robfrank/linklift/application/domain/service/AskQuestionServiceTest.java` (NF-008, NF-FBB-001)

## New surface: Remove the Mergify configuration (`remove-mergify`)

<!-- generated by the fold: the delta wrote no specification for these names. Refine in place. -->

- `.mergify.yml` (NF-RM-001)

## Frontend bundle serving {#frontend-bundle}

`FrontendBundle` (package `it.robfrank.linklift.config`) is a record holding the bundle directory. `FrontendBundle.fromEnvValue(String)` returns `Optional<FrontendBundle>`: empty for a null or blank value, empty with a warning through SLF4J for a value that is not a directory holding a regular file `index.html`, and present otherwise, holding the directory's real path (`Path.toRealPath()`), because the static handler answers 404 for files under a directory named through a symlink, such as a macOS `@TempDir` (RQ-SFFA-001) <!-- env: ENV-002 -->. `Application.start` passes `FrontendBundle.fromEnvValue(System.getenv("LINKLIFT_WEBAPP_DIR"))` to `WebBuilder.withFrontend(Optional<FrontendBundle>)`, beside the other builder calls; tests pass `withFrontend` the `fromEnvValue` of a JUnit `@TempDir`.

`WebBuilder.build()` branches on the bundle:

- Without one, it registers `/` with the plain-text answer exactly as today and nothing else in the routing differs from the baseline (RQ-003).
- With one, it does not register the plain-text `/` route. It adds one static-file location through `config.staticFiles.add(Consumer<StaticFileConfig>)`: `hostedPath` `/`, `directory` the bundle path, `location` `Location.EXTERNAL`, and a `skipFileFunction` that skips any request path that is `/api` or starts with `/api/` <!-- env: ENV-002, ENV-004 -->. It registers `config.routes.error(404, …)`, a handler that acts only when the request method is `GET` and the path is neither `/api` nor under `/api/`: it sets status 200, content type `text/html` and the body to the bytes of the bundle's `index.html` (RQ-003, RQ-SFFA-001). Every other 404 passes through untouched, so the API's own 404 answers from the exception handlers and unmatched `/api` paths are left as they are without a bundle (RQ-SFFA-001) <!-- env: ENV-003 -->.

`/up` and `/metrics` stay registered as routes in both branches, and a matched route is answered before any static file, so the bundle cannot shadow them (RQ-002, RQ-004) <!-- env: ENV-001 -->. The global `JwtAuthenticationHandler` still runs on every request and stores an anonymous context when there is no token; static files and the fallback check no permission, and the `RequireAuthentication` filters are all registered on `/api/v1/*` paths, so the bundle adds no path to the `/api/v1/*` authorization of RQ-026. The CORS plugin applies to bundle answers as to every route (RQ-005).

## Container image and Kamal {#container-image}

The root `Dockerfile` is a three-stage build (NF-SFFA-001):

1. `webapp` stage on the `node` image the webapp's own `webapp/Dockerfile` uses: copies `webapp/package*.json`, runs `npm ci`, copies `webapp/`, runs `npm run build` (output `webapp/dist`, `publicPath` `/`) (NF-SFFA-001).
2. `backend` stage on `eclipse-temurin:25-alpine`: copies `mvnw`, `.mvn/`, `pom.xml` and `src/`, runs `./mvnw -B -q -DskipTests package` (the shade plugin of NF-001 writes `target/linklift-*.jar`).
3. Runtime stage on `eclipse-temurin:25-alpine`, pinned by the digest `Dockerfile.local` uses: the same `apk upgrade`, `appgroup`/`appuser`, `/app` ownership, `EXPOSE 7070`, `USER appuser`, `WORKDIR /app` and `ENTRYPOINT` lines as `Dockerfile.local`, plus `COPY --from=backend` of the JAR to `/app/app.jar`, `COPY --from=webapp` of `dist/` to `/app/webapp` (owned by `appuser`) and `ENV LINKLIFT_WEBAPP_DIR=/app/webapp`.

A new `Dockerfile.dockerignore` keeps `target/`, `webapp/node_modules/` <!-- external-ref --> and `webapp/dist/` out of the root `Dockerfile`'s build context. BuildKit reads a `<Dockerfile>.dockerignore` only for the Dockerfile it is named after, so `Dockerfile.local` builds (`docker-compose up`, which copies `target/linklift-*.jar`) still see `target/`; the repository has no root `.dockerignore` <!-- verify: test ! -e .dockerignore -->. `Dockerfile.local` (NF-007), `src/main/docker/Dockerfile` and the `docker` Maven profile are not changed. In `config/deploy.yml` only `proxy.host` changes; the workflow `.github/workflows/kamal-deploy-api.yml` already runs `kamal deploy`, which builds the root `Dockerfile`, so it is not changed.

## New surface: Serve the frontend from the API container (`serve-frontend-from-api`)

- `src/main/java/it/robfrank/linklift/config/FrontendBundle.java` <!-- planned-artifact -->: record `FrontendBundle(Path directory)` with `static Optional<FrontendBundle> fromEnvValue(String value)` and `Path indexFile()` (RQ-SFFA-001).
- `WebBuilder.withFrontend(Optional<FrontendBundle>)`: new builder method in `src/main/java/it/robfrank/linklift/config/WebBuilder.java` (RQ-003, RQ-SFFA-001).
- `LINKLIFT_WEBAPP_DIR`: environment variable naming the bundle directory (RQ-SFFA-001).
- `Dockerfile.dockerignore` <!-- planned-artifact -->: build-context exclusions for the root `Dockerfile` only (NF-SFFA-001).
- `src/test/java/it/robfrank/linklift/config/FrontendBundleTest.java` <!-- planned-artifact -->: JUnit class `FrontendBundleTest` (RQ-SFFA-001).
- `src/test/java/it/robfrank/linklift/adapter/in/web/FrontendServingTest.java` <!-- planned-artifact -->: JUnit class `FrontendServingTest` (RQ-003, RQ-SFFA-001).
