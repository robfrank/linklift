# Delta Requirements: Fix the red backend build

## Added

- **NF-FBB-001** Every Java source under `src/main/java` and `src/test/java` is left untouched by prettier's check mode with `prettier@3.3.3` and `prettier-plugin-java@2.6.4` under the repo's `.prettierrc.js` (`printWidth: 160`), and CI enforces it: the `check` job of `.github/workflows/ci.yml` and of `.github/workflows/clean-code.yml` runs the pre-commit `prettier` hook, which fails on any Java file it would reformat, and each workflow's `build` job runs only after `check` passes.
  - **Tags**: `build`, `ci`, `format`
  - **Falsifier**: formatting only `AskControllerTest` and `AskQuestionServiceTest` while another unformatted Java file stays passes a spot check of those — ruled out by test row `T-format-check`.

## Modified (overrides baseline)

- **NF-008** `./mvnw test` passes on a machine with a running Docker engine, and needs one: every ArcadeDB container the test sources start through Testcontainers uses the image `arcadedata/arcadedb:<v>`, where `<v>` is the ArcadeDB library version on the test classpath (`com.arcadedb.Constants.getRawVersion()`), and no ArcadeDB container in the test sources names `arcadedata/arcadedb-headless` or the `latest` tag; `VectorSearchE2ETest`, which is not excluded from the default run, starts an `ollama/ollama:latest` container and pulls `all-minilm:l6-v2`; and every test app the test sources start through `JavalinTest` listens on `127.0.0.1` only, so a JavalinTest-based test (`ListLinksControllerTest.getGraph_shouldReturn401_whenUserNotAuthenticated` among them) gets the same answer in the full suite as when run on its own.
  - **Tags**: `build`, `guide:local-run`
  - **Falsifier**: switching only `ArcadeDbContainer` to the full image while `DatabaseInitializerTest` and `AuthenticationIntegrationTest` keep the headless image passes `ArcadeDbContainerTest` — ruled out by test rows `T-image-sources` and `T-full-suite`.

## Assumes (read-only baseline dependencies)

- `RQ-026` an anonymous `/api/v1/*` request is answered 401 — the graph controller test asserts that status; relied upon as the baseline states it.
- `RQ-045` `GET /api/v1/graph` — the controller method the graph controller tests drive; relied upon as the baseline states it.
