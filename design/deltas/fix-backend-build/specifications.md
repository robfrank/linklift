# Delta Specifications: Fix the red backend build

Test-side and formatting changes only; no file under `src/main` changes.

## Test container image {#test-container-image}

The test sources name the ArcadeDB image as a string literal in each container declaration. Four of them name an image other than the one NF-008 requires (three the headless image, one the `latest` tag)
<!-- verify: grep -rl -e arcadedb-headless -e arcadedata/arcadedb:latest src/test/java | wc -l == 4 -->
<!-- blind spot: only string literals in src/test/java; an image assembled from parts or read from a resource file is not matched. -->
: `src/test/java/it/robfrank/linklift/testcontainers/ArcadeDbContainer.java` (the shared container behind `ArcadeDbTestBase`), `src/test/java/it/robfrank/linklift/config/DatabaseInitializerTest.java`, `src/test/java/it/robfrank/linklift/integration/AuthenticationIntegrationTest.java` and `src/test/java/it/robfrank/linklift/adapter/out/persistence/ArcadeAuthTokenRepositoryTest.java`. Each literal becomes `"arcadedata/arcadedb:" + Constants.getRawVersion()`, the form the other persistence tests already use, so the tag follows the ArcadeDB dependency in `pom.xml` (NF-008). No separate image property is introduced and the library version is not pinned. `ArcadeDbContainer` keeps its environment, user override and readiness wait.

## Loopback test apps {#loopback-test-apps}

A new test helper `LoopbackJavalin` (package `it.robfrank.linklift.adapter.in.web`, under `src/test/java`) is the single place JavalinTest-based tests start an app. `LoopbackJavalin.test(app, testCase)` sets `app.unsafe.jetty.host` to the loopback address of NF-008 and then calls `JavalinTest.test(app, testCase)`. Javalin creates its Jetty server lazily on `start`, and JavalinTest starts the app with `app.start(0)`, so the host is in force when the connector binds; JavalinTest's own client already sends to the loopback origin
(`HttpClient.origin` is `http://127.0.0.1:<port>` in javalin-testtools 7.2.4, read from its sources jar) <!-- external-ref -->.

Every `JavalinTest.test(` call in the test sources is replaced by `LoopbackJavalin.test(` with the same arguments: 50 calls
<!-- verify: grep -rn 'JavalinTest.test(' src/test/java | wc -l == 50 -->
in 9 test classes
<!-- verify: grep -rl 'JavalinTest' src/test/java | wc -l == 9 -->
<!-- blind spot: matches the literal call text only; a static import of JavalinTest.test would be missed (none exists: grep -rn 'import static io.javalin' src/test/java returns nothing). -->
, including `ObservabilityTest`, whose app comes from `WebBuilder.build()`; the helper sets the host on the built app, so `WebBuilder` is not in the file plan. After the change the only `JavalinTest.test(` call is the one inside `LoopbackJavalin`.

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
