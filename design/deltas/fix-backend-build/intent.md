# Fix the red backend build (prettier, Testcontainers image, graph 401 test)
<!-- intent: draft -->

## Problem
The backend build on trunk is red. CI's `check` job and the CleanCode workflow fail because prettier (prettier-java, print width 160) reformats Java test files committed unformatted (`AskControllerTest`, `AskQuestionServiceTest`), so CI's Maven build never runs. Locally `./mvnw test` runs 476 tests with 1 failure and 6 errors: every Testcontainers class fails because the image tag follows the ArcadeDB library version and `arcadedata/arcadedb-headless:26.11.1` is not published; and `ListLinksControllerTest.getGraph_shouldReturn401_whenUserNotAuthenticated` gets 404 instead of 401 in the full suite while passing on its own.

## Outcome
- `./mvnw test` passes on a machine with Docker running.
- The Java sources pass the repo's prettier check, so CleanCode and CI's `check` job go green and CI's Maven build runs.
- The graph 401 test passes in the full suite, not only on its own.

## Baseline and rules
Trunk `design/` at the current head. Rules: java-developer. Standards: the repo's `CLAUDE.md`.

## Footprint
**Adds or changes:** the Testcontainers tests use the published `arcadedata/arcadedb:<library version>` image (NF-008); Java sources conform to prettier-java at width 160, enforced in CI (NF-FBB-001, new).
**Relies on:** anonymous requests answer 401 with code `2204` (RQ-026); the graph endpoint (RQ-045).
**Out of scope:** the missing `-Pe2e-tests` profile and the Ollama pull; OpenRewrite failing on JDK 27; the known security gaps; the webapp (delta `fix-webapp-tests`).

## Constraints and quality
Test-side and formatting changes only: no change to application behaviour. Done means `./mvnw test` passes with Docker running, and CleanCode and CI go green.

## Priority and lane
Priority high: a red trunk build. Light lane.

## Decisions
- One delta per design root — a single delta for both roots, or one per cause, rejected: a delta lives in one root and four is overhead for mechanical fixes · accepted
- Test containers use the full `arcadedata/arcadedb:26.11.1` image at the library version — a separate image property, pinning the library to 26.10.1 and the SNAPSHOT image rejected: the full image is published at the library's version · corrected
- Fix the graph test's isolation, not the graph route — changing the route rejected: the route maps the error to 401 correctly and the test passes alone · accepted
- New rule `java-developer` in the repo's `.agents/rules/`, with the default toolchain — no rule rejected: no gates at apply time · accepted
- Project standard is the committed `CLAUDE.md` — the conductor style guides rejected: not chosen as standards · accepted

## Open questions
None.

## Amendments
