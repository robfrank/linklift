# Delta Specifications: Serve the frontend from the API container

## Frontend bundle serving {#frontend-bundle}

`FrontendBundle` (package `it.robfrank.linklift.config`) is a record holding the bundle directory. `FrontendBundle.fromEnvValue(String)` returns `Optional<FrontendBundle>`: empty for a null or blank value, empty with a warning through SLF4J for a value that is not a directory holding a regular file `index.html`, and present otherwise, holding the directory's real path (`Path.toRealPath()`), because the static handler answers 404 for files under a directory named through a symlink, such as a macOS `@TempDir` (RQ-SFFA-001) <!-- env: ENV-002 -->. `Application.start` passes `FrontendBundle.fromEnvValue(System.getenv("LINKLIFT_WEBAPP_DIR"))` to `WebBuilder.withFrontend(Optional<FrontendBundle>)`, beside the other builder calls; tests pass `withFrontend` the `fromEnvValue` of a JUnit `@TempDir`.

`WebBuilder.build()` branches on the bundle:

- Without one, it registers `/` with the plain-text answer exactly as today and nothing else in the routing differs from the baseline (RQ-003).
- With one, it does not register the plain-text `/` route. It adds one static-file location through `config.staticFiles.add(Consumer<StaticFileConfig>)`: `hostedPath` `/`, `directory` the bundle path, `location` `Location.EXTERNAL`, and a `skipFileFunction` that skips any request path that is `/api` or starts with `/api/` <!-- env: ENV-002, ENV-004 -->. It registers `config.routes.error(404, …)`, a handler that acts only when the request method is `GET` and the path is neither `/api` nor under `/api/`: it sets status 200, content type `text/html` and the body to the bytes of the bundle's `index.html` (RQ-003, RQ-SFFA-001). Every other 404 passes through untouched, so the API's own 404 answers from the exception handlers and unmatched `/api` paths are left as they are without a bundle (RQ-SFFA-001) <!-- env: ENV-003 -->.

`/up` and `/metrics` stay registered as routes in both branches, and a matched route is answered before any static file, so the bundle cannot shadow them (RQ-002, RQ-004) <!-- env: ENV-001 -->. The global `JwtAuthenticationHandler` still runs on every request and stores an anonymous context when there is no token; static files and the fallback check no permission, and the `RequireAuthentication` filters are all registered on `/api/v1/*` paths, so the bundle adds no path to the `/api/v1/*` authorization of RQ-026. The CORS plugin applies to bundle answers as to every route (RQ-005).

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
