# Ledger

A manager-readable history of what changed in this system and why: one entry per folded delta, newest first. Notes keep accumulating after the fold via note-delta.py.

## 2026-11-16 · Serve the frontend from the API container (`serve-frontend-from-api`)

### What changes

The API server can serve the built React app. When the image points it at the app's build, the home page and every app route load the app, its assets come from the same server, and the API keeps answering exactly as before. The image Kamal deploys is now built from source in one multi-stage build and carries both the app and the API, and Kamal serves it under the friendlier host name.

### Why

The frontend has never been deployed: Kamal ships only the API and the database, and the separate nginx webapp service never deployed. The webapp already calls the API at a relative path, so serving it from the same origin needs no frontend change.

### Impact and risk

- The public host name changes; the new name needs DNS, which is out of scope.
- A deploy now builds Node and Maven stages, so it takes longer than pulling a prebuilt image.
- Without the bundle setting (tests, local runs, the Docker Hub image) the server behaves as before.

### Changelog

The API container now serves the React app at the site root, and app routes fall back to the app's index page, also after a refresh. The API answers exactly as before. One Kamal deploy ships the app and the API together at linklift.arcadebrain.ai. Without a bundle, the server behaves as it did before.

### Notes

- 2026-11-15 · author — Host change: `linklift-api.arcadebrain.ai` becomes `linklift.arcadebrain.ai` in `config/deploy.yml` (NF-SFFA-001).

- 2026-11-15 · author — The bundle location is an environment variable, `LINKLIFT_WEBAPP_DIR`, set by the image; unset means no bundle and today's behaviour, including the plain-text `/` answer, so tests and local JAR runs need nothing.
- 2026-11-15 · author — A value that names no directory with `index.html` starts the server without a bundle and logs a warning, instead of failing startup.
- 2026-11-15 · author — The `index.html` fallback applies to `GET` only; other methods on unmatched paths keep their 404. Unmatched `/api` paths answer exactly as without a bundle, which is how the intent's "the API's JSON 404" is held: the design does not change that response.
- 2026-11-15 · author — Javalin's `spaRoot` is not used because it would also answer unmatched `/api` paths with `index.html`; a `404` error handler limited to non-API `GET` requests is used instead.
- 2026-11-15 · author — Added `RQ-004` (`/metrics`) and `NF-001` (shaded JAR) to the assumed requirements: the intent's constraints keep `/metrics` unchanged and the image's build stage relies on the JAR.
- 2026-11-15 · author — Added `.dockerignore` so a local `docker build .` does not send `target/` or `webapp/node_modules/` <!-- external-ref --> into the context; `Dockerfile.local`, `src/main/docker/Dockerfile`, the `docker` Maven profile and the deploy workflow stay as they are.
- 2026-11-15 · author — Path traversal outside the bundle directory is left to Javalin's static-file handler; no requirement states it.
- 2026-11-15 · fix R1-1 — Supersedes the `.dockerignore` note above: the file is `Dockerfile.dockerignore`, read only for the root `Dockerfile`, because a root `.dockerignore` excluding `target/` would break `docker-compose up`, whose `Dockerfile.local` copies the JAR from `target/` (NF-007).
- 2026-11-15 · gate — review: 2 round(s), lane light — blockers in round(s) 1 — 2 finding(s) were damage from a fix pass.
- 2026-11-16 · round 01 conductor — Gates green on JDK 25: dep-sync, type-check, test (528 run, 0 failures, 0 errors, 1 skipped), format-check; lint is owner-run and was not run. New code covered by FrontendBundleTest (6) and FrontendServingTest (9).
- 2026-11-16 · round 01 conductor — Root image: docker build succeeded; container runs as appuser with the bundle at /app/webapp on Temurin 25.0.1. Dockerfile.local still builds after ./mvnw package (T-local-image).
- 2026-11-16 · round 01 conductor — Tests use io.javalin.testtools Request/Response (okhttp3 is not on the classpath), so POST bodies are plain strings and headers are read via response.headers().
- 2026-11-16 · round 01 conductor — The prettier hook realigned the whole CLAUDE.md environment-variable table when the LINKLIFT_WEBAPP_DIR row was added.
- 2026-11-16 · round 01 conductor — Verifier smoke test against a live ArcadeDB: / and /collections/42 return 200 text/html (index.html), /up returns 200, /api/v1/nope returns the API's 404 (text/plain), not index.html. The app needs ArcadeDB to start.


### Effort
- implementation rounds: 1
- review rounds: 2
- attempts: 1
- coverage: 64%
- tokens: 309k
- wall-clock elapsed (includes idle, not a work measure): 22m
- main agent: 19k generated over 33 messages
- implementers: 23k generated over 4/4 dispatch(es)

- 2026-11-16 · fold — repaired after merge: `96a3e6ba7c2f82b2` → `5256f822b10ba6b3` (scoped `gate.py --repair-review`); the folded design is `5256f822b10ba6b3`

- 2026-11-16 · fold — provenance gap: changed on trunk since the last fold with no delta declaring them: `Dockerfile`, `Dockerfile.dockerignore`


## 2026-11-15 · Remove the Mergify configuration (`remove-mergify`)

### What changes

The repository's Mergify configuration is deleted. The Dependabot merge workflow stays (intent Amendment 1) and keeps merging labelled, approved Dependabot pull requests with `[skip ci]`; every other bot pull request is merged by a maintainer.

### Why

Mergify dropped the `commit_message_template` option the configuration used, so the file is invalid and every pull request carries failing Mergify checks. The user no longer wants Mergify; upgrading the configuration (PR #954) was rejected.

### Impact and risk

No application code, CI workflow or Dependabot configuration changes. Bot pull requests the workflow does not merge (any from `github-actions[bot]`, and Dependabot ones without its label) no longer merge on their own, and a maintainer's merge of one runs CI on `main`. Uninstalling the Mergify GitHub App and closing PR #954 are left to the user.

### Changelog

The repository no longer uses Mergify, so pull requests stop showing failing Mergify checks. Approved Dependabot pull requests labelled for auto-merge still merge through the existing Dependabot workflow; every other bot pull request is merged by a maintainer, like any other pull request.

### Notes

- 2026-11-15 · author — no `specifications.md`: the delta adds no source file, function or CLI option, and deletes one configuration file.
- 2026-11-15 · author — `NF-RM-001` names every configuration location Mergify reads, not only the one file deleted, so the requirement is checkable after the change.
- 2026-11-15 · author — the consequence that bot merges now run CI on `main` (no `[skip ci]`) follows from removing the rule; recorded as impact, not as a new requirement.
- 2026-11-15 · fix — supersedes the entry above after intent Amendment 1: only maintainer-merged bot pull requests run CI on `main`; labelled, approved Dependabot pull requests keep merging through `.github/workflows/merge-dependabot.yml` with `[skip ci]`.
- 2026-11-15 · gate — review: 2 round(s), lane light — blockers in round(s) 1.
- 2026-11-15 · round 01 conductor — Round 01 deleted .mergify.yml (f80f4c2); T-no-mergify-config and T-ci-untouched hold, merge-dependabot.yml byte-identical. Not checkable from the tree: no Mergify checks on the next PR, which also needs the Mergify GitHub App uninstalled.


### Effort
- implementation rounds: 1
- review rounds: 2
- attempts: 1
- coverage: 0%
- tokens: 90k
- wall-clock elapsed (includes idle, not a work measure): 20m
- main agent: 4k generated over 10 messages
- implementers: 3k generated over 2/2 dispatch(es)


## 2026-11-15 · Fix the red backend build (prettier, Testcontainers image, graph 401 test) (`fix-backend-build`)

### What changes

Test sources and formatting only. The Testcontainers tests start the published full ArcadeDB image at the library's version, every JavalinTest-based test app listens on loopback through one shared helper, and the Ask tests are reformatted so the prettier check in CI passes.

### Why

Trunk's backend build is red: CI stops at the prettier check before Maven runs, the Testcontainers tests ask for an image tag that is not published, and the graph controller's unauthenticated test fails in the full suite while passing alone.

### Impact and risk

No application behaviour changes; production code is not touched. The risk is confined to the test suite: a test that relied on the headless image's defaults could behave differently on the full image, which `T-full-suite` would show.

### Changelog

Backend build green again: Testcontainers tests use the full ArcadeDB image at the library version, JavalinTest apps bind to loopback, and the Java sources pass the prettier check.

### Notes

- 2026-11-14 · author — `ArcadeAuthTokenRepositoryTest`, which used the `latest` tag rather than the headless image, is switched to the library version too, because NF-008 as written covers every ArcadeDB container.
- 2026-11-14 · author — the shared helper sets the host on an already-built app (`app.unsafe.jetty.host`) instead of wrapping `Javalin.create`, so `ObservabilityTest`'s `WebBuilder.build()` app is covered without changing production code.
- 2026-11-14 · author — the helper is named `LoopbackJavalin`, not `…Test`, so Surefire's default includes do not treat it as a test class.
- 2026-11-14 · author — NF-FBB-001's CI enforcement is already wired (pre-commit prettier hook in both workflows' `check` job); no workflow file changes.
- 2026-11-14 · author — the "passes" outcome is carried in NF-008 rather than a new requirement, since the intent's footprint lists only NF-008 and NF-FBB-001.
- 2026-11-14 · author — all work sits in round 00: its acceptance (every java-developer gate green) is the delta's outcome, so no feature round follows.
- 2026-11-14 · author — supersedes the round-00 entry above: the acceptance is every java-developer gate green except lint, which the owner runs.
- 2026-11-14 · gate — review: 2 round(s), lane light — blockers in round(s) 1 — 1 finding(s) were damage from a fix pass.
- 2026-11-14 · gate — review: 2 round(s), lane light — blockers in round(s) 1.
- 2026-11-14 · round 00 conductor — Gates green: dep-sync, format-check, type-check, test (513 run, 0 failures, 0 errors, 1 skipped); lint is owner-run per the approved override and was not run.
- 2026-11-14 · round 00 conductor — Three consecutive full runs green (runs 3-5); the graph 401 test passed in all five runs. Run 2 failed once on a DatabaseInitializerTest Testcontainers readiness timeout (90 s startup cap) with the full arcadedata/arcadedb:26.11.1 image, not reproduced in four other runs; follow-up: raise withStartupTimeout on the container declarations (own delta).
- 2026-11-14 · round 00 conductor — The 404 cause was never independently reproduced; the loopback-binding hypothesis is backed by T-loopback-stray-listener.
- 2026-11-14 · round 00 conductor — SecurityIntegrationTest (package adapter.in.web.security) needed an explicit import of LoopbackJavalin; ArcadeAuthTokenRepositoryTest needed a com.arcadedb.Constants import.


### Effort
- implementation rounds: 1
- review rounds: 2
- attempts: 1
- coverage: 64%
- tokens: 1079k
- wall-clock elapsed (includes idle, not a work measure): 11h19m
- main agent: 20k generated over 45 messages
- implementers: 17k generated over 2/2 dispatch(es)

- 2026-11-15 · fold — the design changed DURING implementation: reviewed on the shelf as `954ee2261b9e9d09`, built as `7b0f97cf521c7ae4` (re-reviewed in 2 round(s))
