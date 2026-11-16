# Delta Requirements: Serve the frontend from the API container

## Added

- **RQ-SFFA-001** The server runs with a frontend bundle when the environment variable `LINKLIFT_WEBAPP_DIR` names a directory that contains a file `index.html`; when the variable is unset or blank it runs without one, and when it names anything else it runs without one and logs a warning naming the value. With a bundle, a `GET` to a path that is neither `/api` nor under `/api/` and that no route matches answers 200 with the file at that path inside the directory when one exists, and otherwise with that directory's `index.html` as `text/html` (so `/collections/42` loads the app, also on a refresh); these answers need no credentials. A request to `/api` or to any path under `/api/` that no route matches answers exactly as it does without a bundle (404, never the bundle's `index.html`), and so does a non-`GET` request to any path that no route matches. Without a bundle, a request that no route matches answers 404.
  - **Tags**: `api`, `frontend`, `guide:configuration`, `guide:deploy`
  - **Falsifier**: a catch-all fallback on every unmatched `GET` (Javalin's `spaRoot` at `/`) serves `index.html` to a browser that asks for `/api/v1/nope` — ruled out by test row `T-api-unmatched`.

- **NF-SFFA-001** The repository-root `Dockerfile` (the one `kamal deploy` builds for `config/deploy.yml`) builds the webapp with `npm ci` and `npm run build` in `webapp/` and the shaded JAR with `./mvnw` (tests skipped) from the checked-out sources, in build stages of its own; its final image runs the JAR as the non-root user `appuser` on `eclipse-temurin:25-alpine`, exposes port 7070, takes JVM options from `JAVA_OPTS`, holds the webapp build output (`webapp/dist`) at `/app/webapp` and sets `LINKLIFT_WEBAPP_DIR=/app/webapp`. `config/deploy.yml` routes the kamal-proxy host `linklift.arcadebrain.ai` to that container on `app_port` 7070 with health-check path `/up` and SSL off, and keeps its service name, image, registry, builder, environment, SSH user and accessories unchanged, so one `kamal deploy` ships the app and the API together.
  - **Tags**: `build`, `guide:deploy`
  - **Falsifier**: a `Dockerfile` that copies a prebuilt JAR from `target/` and no bundle still builds and boots but ships no frontend — ruled out by test row `T-image-contents`.

## Modified (overrides baseline)

- **RQ-003** `GET /` needs no credentials. With a frontend bundle (RQ-SFFA-001) it answers HTTP 200 with the bundle's `index.html` as `text/html`; without one it answers HTTP 200 with the plain-text body `LinkLift`.
  - **Tags**: `api`, `frontend`
  - **Falsifier**: a builder that keeps the plain-text `/` route registered while also serving static files passes the no-bundle test but never loads the app at `/` — ruled out by test row `T-root-bundle`.

## Assumes (read-only baseline dependencies)

- `RQ-002` `/up` health check — relied upon unchanged; the kamal-proxy health check and the bundle must not shadow it.
- `RQ-004` `/metrics` — relied upon unchanged with or without a bundle.
- `RQ-005` CORS from any origin — relied upon unchanged.
- `RQ-026` `/api/v1/*` authorization and JSON errors — relied upon unchanged; bundle files are served without this check because they are not under `/api/`.
- `NF-001` `mvn package` produces the shaded executable JAR — relied upon by the image's Maven build stage.
- `NF-007` the `Dockerfile.local` image — relied upon unchanged; the root `Dockerfile`'s runtime stage reuses its base image and user setup.
- `RQ-010`, `RQ-015`, `RQ-021`, `RQ-023`, `RQ-025`, `RQ-030`, `RQ-035`, `RQ-041`, `RQ-042`, `RQ-044`, `RQ-045`, `RQ-059`, `RQ-060`, `RQ-061`, `RQ-073`, `RQ-075`, `RQ-080`, `RQ-090`, `RQ-100`, `RQ-101`, `RQ-102`, `RQ-103`, `RQ-104`, `RQ-110`, `RQ-111`, `RQ-112`, `RQ-120`, `RQ-121`, `RQ-122`, `RQ-123`, `RQ-124`, `RQ-125`, `RQ-130`, `RQ-131` — the API routes and exception handlers cited by the trunk `#http-surface` section, which the delta's specification restates with only its first sentence changed; relied upon unchanged.
