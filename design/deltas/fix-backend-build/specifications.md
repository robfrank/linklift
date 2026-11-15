# Delta Specifications: Fix the red backend build

Test-side and formatting changes only; no file under `src/main` changes.

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
