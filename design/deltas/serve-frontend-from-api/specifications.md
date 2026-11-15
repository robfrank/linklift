# Delta Specifications: Serve the frontend from the API container

## Frontend bundle serving {#frontend-bundle}

`FrontendBundle` (package `it.robfrank.linklift.config`) is a record holding the bundle directory. `FrontendBundle.fromEnvValue(String)` returns `Optional<FrontendBundle>`: empty for a null or blank value, empty with a warning through SLF4J for a value that is not a directory holding a regular file `index.html`, and present otherwise (RQ-SFFA-001). `Application.start` passes `FrontendBundle.fromEnvValue(System.getenv("LINKLIFT_WEBAPP_DIR"))` to `WebBuilder.withFrontend(Optional<FrontendBundle>)`, beside the other builder calls; tests call `withFrontend` with a bundle in a JUnit `@TempDir`.

`WebBuilder.build()` branches on the bundle:

- Without one, it registers `/` with the plain-text answer exactly as today and nothing else in the routing differs from the baseline (RQ-003).
- With one, it does not register the plain-text `/` route. It adds one static-file location through `config.staticFiles.add(Consumer<StaticFileConfig>)`: `hostedPath` `/`, `directory` the bundle path, `location` `Location.EXTERNAL`, and a `skipFileFunction` that skips any request path that is `/api` or starts with `/api/`. It registers `config.routes.error(404, …)`, a handler that acts only when the request method is `GET` and the path is neither `/api` nor under `/api/`: it sets status 200, content type `text/html` and the body to the bytes of the bundle's `index.html` (RQ-003, RQ-SFFA-001). Every other 404 passes through untouched, so the API's own 404 answers from the exception handlers and unmatched `/api` paths are left as they are without a bundle (RQ-SFFA-001).

`/up` and `/metrics` stay registered as routes in both branches, and a matched route is answered before any static file, so the bundle cannot shadow them (RQ-002, RQ-004). The global `JwtAuthenticationHandler` still runs on every request and stores an anonymous context when there is no token; static files and the fallback check no permission, and the `RequireAuthentication` filters are all registered on `/api/v1/*` paths, so the bundle adds no path to the `/api/v1/*` authorization of RQ-026. The CORS plugin applies to bundle answers as to every route (RQ-005).

## Container image and Kamal {#container-image}

The root `Dockerfile` is a three-stage build (NF-SFFA-001):

1. `webapp` stage on the `node` image the webapp's own `webapp/Dockerfile` uses: copies `webapp/package*.json`, runs `npm ci`, copies `webapp/`, runs `npm run build` (output `webapp/dist`, `publicPath` `/`) (NF-SFFA-001).
2. `backend` stage on `eclipse-temurin:25-alpine`: copies `mvnw`, `.mvn/`, `pom.xml` and `src/`, runs `./mvnw -B -q -DskipTests package` (the shade plugin of NF-001 writes `target/linklift-*.jar`).
3. Runtime stage on `eclipse-temurin:25-alpine`, pinned by the digest `Dockerfile.local` uses: the same `apk upgrade`, `appgroup`/`appuser`, `/app` ownership, `EXPOSE 7070`, `USER appuser`, `WORKDIR /app` and `ENTRYPOINT` lines as `Dockerfile.local`, plus `COPY --from=backend` of the JAR to `/app/app.jar`, `COPY --from=webapp` of `dist/` to `/app/webapp` (owned by `appuser`) and `ENV LINKLIFT_WEBAPP_DIR=/app/webapp`.

A new `.dockerignore` keeps `target/`, `webapp/node_modules/` <!-- external-ref --> and `webapp/dist/` out of a local build context so stale outputs never reach a stage. `Dockerfile.local` (NF-007), `src/main/docker/Dockerfile` and the `docker` Maven profile are not changed. In `config/deploy.yml` only `proxy.host` changes; the workflow `.github/workflows/kamal-deploy-api.yml` already runs `kamal deploy`, which builds the root `Dockerfile`, so it is not changed.

## New surface: Serve the frontend from the API container (`serve-frontend-from-api`)

- `src/main/java/it/robfrank/linklift/config/FrontendBundle.java` <!-- planned-artifact -->: record `FrontendBundle(Path directory)` with `static Optional<FrontendBundle> fromEnvValue(String value)` and `Path indexFile()` (RQ-SFFA-001).
- `WebBuilder.withFrontend(Optional<FrontendBundle>)`: new builder method in `src/main/java/it/robfrank/linklift/config/WebBuilder.java` (RQ-003, RQ-SFFA-001).
- `LINKLIFT_WEBAPP_DIR`: environment variable naming the bundle directory (RQ-SFFA-001).
- `.dockerignore` <!-- planned-artifact -->: build-context exclusions for the root `Dockerfile` (NF-SFFA-001).
- `src/test/java/it/robfrank/linklift/config/FrontendBundleTest.java` <!-- planned-artifact -->: JUnit class `FrontendBundleTest` (RQ-SFFA-001).
- `src/test/java/it/robfrank/linklift/adapter/in/web/FrontendServingTest.java` <!-- planned-artifact -->: JUnit class `FrontendServingTest` (RQ-003, RQ-SFFA-001).
