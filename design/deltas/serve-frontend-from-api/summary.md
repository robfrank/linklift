# Serve the frontend from the API container
<!-- footprint: RQ-003 modify · RQ-SFFA-001 add · NF-SFFA-001 add · doc:operational-guides/deploy#interfaces-and-usage modify -->

## What changes

The API server can serve the built React app. When the image points it at the app's build, the home page and every app route load the app, its assets come from the same server, and the API keeps answering exactly as before. The image Kamal deploys is now built from source in one multi-stage build and carries both the app and the API, and Kamal serves it under the friendlier host name.

## Why

The frontend has never been deployed: Kamal ships only the API and the database, and the separate nginx webapp service never deployed. The webapp already calls the API at a relative path, so serving it from the same origin needs no frontend change.

## Impact and risk

- The public host name changes; the new name needs DNS, which is out of scope.
- A deploy now builds Node and Maven stages, so it takes longer than pulling a prebuilt image.
- Without the bundle setting (tests, local runs, the Docker Hub image) the server behaves as before.

## Changelog

## Notes

- 2026-11-15 · author — Host change: `linklift-api.arcadebrain.ai` becomes `linklift.arcadebrain.ai` in `config/deploy.yml` (NF-SFFA-001).

- 2026-11-15 · author — The bundle location is an environment variable, `LINKLIFT_WEBAPP_DIR`, set by the image; unset means no bundle and today's behaviour, including the plain-text `/` answer, so tests and local JAR runs need nothing.
- 2026-11-15 · author — A value that names no directory with `index.html` starts the server without a bundle and logs a warning, instead of failing startup.
- 2026-11-15 · author — The `index.html` fallback applies to `GET` only; other methods on unmatched paths keep their 404. Unmatched `/api` paths answer exactly as without a bundle, which is how the intent's "the API's JSON 404" is held: the design does not change that response.
- 2026-11-15 · author — Javalin's `spaRoot` is not used because it would also answer unmatched `/api` paths with `index.html`; a `404` error handler limited to non-API `GET` requests is used instead.
- 2026-11-15 · author — Added `RQ-004` (`/metrics`) and `NF-001` (shaded JAR) to the assumed requirements: the intent's constraints keep `/metrics` unchanged and the image's build stage relies on the JAR.
- 2026-11-15 · author — Added `.dockerignore` so a local `docker build .` does not send `target/` or `webapp/node_modules/` <!-- external-ref --> into the context; `Dockerfile.local`, `src/main/docker/Dockerfile`, the `docker` Maven profile and the deploy workflow stay as they are.
- 2026-11-15 · author — Path traversal outside the bundle directory is left to Javalin's static-file handler; no requirement states it.
