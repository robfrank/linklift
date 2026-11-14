---
description: Java Developer rule — Java 25 + Maven (always ./mvnw), prettier-plugin-java (format), OpenRewrite dry run (lint), javac (types), JUnit Jupiter + AssertJ + Mockito + Testcontainers (tests), JaCoCo (coverage report, no threshold). Chosen at Step 1.5 as the readiness lens for Java/Maven deltas.
paths:
  - "**/*.java"
  - "pom.xml"
  - "**/pom.xml"
  - "rewrite.yml"
  - ".mvn/**"
smoke_tested: 2026-11-14 · ./mvnw npx timeout · java-smoke
---

# Java Developer Rule

This rule targets Java 25 (`<release>25</release>`) built with Maven through the committed wrapper (`./mvnw`) + OpenRewrite `rewrite:dryRun` (lint) + prettier with prettier-plugin-java (format) + javac via `compile test-compile` (types) + JUnit Jupiter with AssertJ, Mockito and Testcontainers on Surefire (tests) + JaCoCo (coverage report, no global threshold).

## Purpose

The **Java Developer (JD)** writes production-ready, maintainable Java for services built with Maven: small immutable domain types, explicit wiring, and tests that exercise real collaborators wherever that is affordable. The toolchain is deliberately one tool per concern — the Maven wrapper so every machine runs the same Maven, OpenRewrite as the linter (a recipe that _would_ change code is a lint finding), prettier-plugin-java as the only formatter, javac as the type checker, and Surefire running JUnit Jupiter.

"Green" means every gate below passes in order: dependencies resolve, the OpenRewrite dry run proposes no change, prettier finds nothing to reformat, main and test sources compile, the suite passes, and the JaCoCo report is written. Coverage is reported, not thresholded: new or changed production code must arrive with tests (see Testing policy), which the verifier and the reviewers enforce.

## Core Principles

### 1. Architecture: hexagonal, ports and adapters

- **Domain at the centre, with zero framework dependencies.** Domain models, domain services, domain events, exceptions and validators import nothing from the web framework, the database driver or the HTTP client.
- **Ports are interfaces.** Input ports (`port/in`) are the use cases the outside world calls — one use case per interface, a single method, `@FunctionalInterface` style. Output ports (`port/out`) are the contracts the domain needs from persistence, messaging, AI, security and HTTP.
- **Adapters implement ports.** `adapter/in/web` holds thin controllers; `adapter/out/*` holds the implementations of output ports. The dependency rule is one-way: adapters depend on ports; the domain never imports adapter code.
- **Manual wiring.** No DI container and no DI annotations: every object graph is assembled explicitly in one composition root (e.g. `Application.java`). A new adapter or service is wired there, in plain constructor calls, so the wiring is readable and compile-checked.
- **Thin controllers.** Validate input, map it to a command/query record, delegate to the use case, map the result to a response. No business logic in a controller.

### 2. Modern, immutable Java

- **Records for domain entities, commands, queries and value objects.** Validate invariants in the compact constructor; never expose mutable collections (`List.copyOf`, `Map.copyOf`).
- Use the language the release offers: sealed interfaces for closed hierarchies, pattern matching for `switch` and `instanceof`, text blocks, `var` where the type is obvious from the right-hand side.
- **Null discipline.** Prefer `Optional` for absent return values, never for fields or parameters. Annotate nullability where the project uses JSpecify (`@Nullable`, `@NullMarked`).
- No preview features in production code unless the project's `pom.xml` already enables them deliberately.

### 3. Errors and logging

- **One exception hierarchy** rooted in a project exception type that carries an `ErrorCode` enum; a single global handler maps it to the error response body. Do not throw `RuntimeException`/`Exception` directly, and never swallow an exception without logging it with context.
- **SLF4J for logging** (parameterised messages, `logger.info("link {} saved", id)`), never `System.out`/`System.err` — OpenRewrite's logging recipes flag them. No secrets, tokens or passwords in logs.

### 4. Dependencies and build

- **Always `./mvnw`, never a system `mvn`.** The wrapper pins Maven; CI and every gate run through it.
- Versions live in `<properties>` and are referenced as `${…}`; BOMs (`junit-bom`, `jackson-bom`) go in `<dependencyManagement>` with `<scope>import</scope>`. Test-only libraries carry `<scope>test</scope>`.
- Build with a JDK whose major version equals `<release>`: newer JDKs compile fine but break OpenRewrite's parser (see Platform facts).

### 5. Testing policy

- **Real implementations over mocks.** Integration tests use Testcontainers with the real database; outbound HTTP goes through WireMock; deterministic fakes (e.g. a fake embedding generator) replace non-deterministic services. Do not mock repositories or output ports when a real or in-memory implementation is affordable; mock only true external collaborators.
- **Controllers are tested through the HTTP layer** (e.g. `JavalinTest`), not by calling handler methods directly — routing, serialization and the global exception handler are the point.
- **AssertJ for assertions** (`assertThat(...)`), JUnit Jupiter for structure (`@Test`, `@ParameterizedTest`, `@Nested`), Mockito only for collaborators. Awaitility for asynchronous outcomes — never `Thread.sleep`.
- **Every public use case, endpoint and adapter** gets a happy path with realistic input and a full output assertion, at least one failure path (validation, missing resource, auth, timeout), and the boundary cases.
- **New or changed code comes with tests.** There is no global coverage threshold; a change that adds production code without adding or changing tests is a finding. The JaCoCo report the `test` gate writes is evidence for reviewers, not a gate.
- Tests that need Docker are part of the normal suite; tests that need heavyweight optional services (e.g. a real Ollama) live behind a Maven profile (`-Pe2e-tests`) and are not run by the gates.

## Rule Contract Sections

### Gate Commands

Placeholders `{SRC_ROOTS}`, `{TEST_ROOTS}`, `{SCRIPTS_ROOTS}` come from Layout; `{TEST_CAP}`, `{PER_TEST}`, `{OVERALL}`, `{TEST_ID}` from Quality Thresholds and the invocation. Maven discovers its own source roots from `pom.xml`, so only `format-check` takes roots. Every Maven gate runs through the wrapper (`./mvnw`), with `JAVA_HOME` pointing at a JDK whose major version equals `<release>` (checked by `env-verify`).

#### dep-sync

```bash
./mvnw -q dependency:resolve
```

#### lint

OpenRewrite in dry-run mode with the project's active recipes (`rewrite.yml` + the `rewrite-maven-plugin` `<activeRecipes>`): the gate fails when any recipe would change a file, and when an active recipe is misconfigured. The plugin's user property is `failOnDryRunResults` (no `rewrite.` prefix). The proposed patch is written to `target/rewrite/rewrite.patch`.

```bash
./mvnw -q rewrite:dryRun -DfailOnDryRunResults=true -Drewrite.failOnInvalidActiveRecipes=true
```

#### format-check

Prettier with prettier-plugin-java, versions pinned to the ones the repo's `.pre-commit-config.yaml` installs; it honours the project's prettier config (`.prettierrc.js` / `.prettierrc.json`, `printWidth: 160`). Check only — never `--write`. The plugin is passed by absolute path and `NODE_PATH` points at npx's install, because neither a bare `--plugin=` name nor a config file's `require.resolve(...)` can see a package that only npx installed (see Platform facts); `@prettier/plugin-xml` is installed too so a config that lists it still loads.

```bash
npx --yes -p prettier@3.3.3 -p prettier-plugin-java@2.6.4 -p @prettier/plugin-xml@3.4.1 -c 'export NODE_PATH="$(dirname "$(dirname "$(command -v prettier)")")"; prettier --plugin="$(node -p "require.resolve(\"prettier-plugin-java\")")" --check {SRC_ROOTS} {TEST_ROOTS}'
```

#### type-check

```bash
./mvnw -q -DskipTests compile test-compile
```

#### test-baseline

A fast pass/fail run with no coverage (`-Djacoco.skip=true` also blanks `@{argLine}`, so a Surefire `argLine` that references it still works), used by apply mode's STEP 0 to confirm the prior phase's main is green before mutating anything. Testcontainers tests run here too, so Docker must be up.

```bash
timeout {TEST_CAP}s ./mvnw -q test -Djacoco.skip=true
```

#### test

The full suite with the JaCoCo agent attached (`prepare-agent` from the pom) and the report written to `target/site/jacoco/` (`jacoco.xml`, `jacoco.csv`, `index.html`). No coverage threshold: `{OVERALL}` is not used (see Quality Thresholds).

```bash
timeout {TEST_CAP}s ./mvnw -q test jacoco:report
```

#### test-targeted

For re-running one failing or timing-out test in the retry ladder. `{TEST_ID}` is a Surefire `-Dtest` filter (`LinkServiceTest`, `LinkServiceTest#createsLink`, `*ControllerTest`); `{PER_TEST}` is the per-test timeout in seconds, escalated 10 → 30 → 90. The `testable.method` key times out `@Test`/`@ParameterizedTest` methods only, never `@BeforeAll`/`@BeforeEach`, so a container started in a lifecycle method is not cut short.

```bash
./mvnw -q test -Djacoco.skip=true -Dtest={TEST_ID} -Dsurefire.failIfNoSpecifiedTests=false -Djunit.jupiter.execution.timeout.testable.method.default={PER_TEST}s
```

### Baseline

Test libraries and build configuration a project MUST carry. At the apply crossing (and `env-check`) the skill runs `dep-baseline` below, which adds the missing test dependencies, and commits what it changed as one `chore: java-developer baseline`; then `dep-sync`, then `env-verify`. The plugin snippets stay a pre-flight judgement (they change how an existing build runs).

#### dev-deps

`<dependency>` elements, inserted into the project-level `<dependencies>` of `pom.xml`.

```xml
<dependency>
  <groupId>org.junit.jupiter</groupId>
  <artifactId>junit-jupiter</artifactId>
  <version>6.1.3</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.assertj</groupId>
  <artifactId>assertj-core</artifactId>
  <version>3.27.7</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.mockito</groupId>
  <artifactId>mockito-core</artifactId>
  <version>5.24.0</version>
  <scope>test</scope>
</dependency>
```

#### dep-baseline

Adds every dev-dep above that `pom.xml` does not declare (as a project dependency, in `<dependencyManagement>`, or in a profile; plugin dependencies do not count) to the project-level `<dependencies>`, as text, keeping the file's indentation; when the pom imports `junit-bom`, JUnit artifacts are added without `<version>` so the BOM keeps managing them. A declared artifact is never touched, whatever its version. Idempotent: with nothing missing it changes nothing. Maven has no lockfile, so there is nothing to re-lock; `dep-sync` resolves next. A 401/403 while resolving means credentials for a private repository: check the `<server>` entry in `~/.m2/settings.xml` whose `<id>` matches the repository (for GitHub Packages, a token with `read:packages`).

```bash
python3 - "{DEV_DEPS_FILE}" <<'PY'
import re, sys, xml.etree.ElementTree as ET
from pathlib import Path

def local(tag):
    return tag.rsplit("}", 1)[-1]

def coords(el):
    kids = {local(c.tag): (c.text or "").strip() for c in el}
    return kids.get("groupId"), kids.get("artifactId")

pom = Path("pom.xml")
if not pom.is_file():
    print(f"no pom.xml in {Path.cwd()}")
    sys.exit(1)
src = pom.read_text(encoding="utf-8")
root = ET.fromstring(src)
want = list(ET.fromstring("<deps>" + Path(sys.argv[1]).read_text(encoding="utf-8") + "</deps>"))

declared, bom_junit = set(), False
def walk(el, parents):
    global bom_junit
    name = local(el.tag)
    if name == "dependency" and parents[-1:] == ["dependencies"] and parents[-2:-1] in (["project"], ["dependencyManagement"], ["profile"]):
        declared.add(coords(el))
        if coords(el) == ("org.junit", "junit-bom"):
            bom_junit = True
    for c in el:
        walk(c, parents + [name])
walk(root, [])

missing = [w for w in want if coords(w) not in declared]
if not missing:
    sys.exit(0)

masked = re.sub(r"<!--.*?-->|<!\[CDATA\[.*?\]\]>|<\?.*?\?>", lambda m: " " * len(m.group(0)), src, flags=re.S)
stack, close_at = [], None
for m in re.finditer(r"<(/?)([A-Za-z_][\w.:-]*)[^>]*?(/?)>", masked):
    closing, name, selfclose = m.group(1), m.group(2).split(":")[-1], m.group(3)
    if closing:
        if name == "dependencies" and stack == ["project", "dependencies"]:
            close_at = m.start()
        if stack and stack[-1] == name:
            stack.pop()
    elif not selfclose:
        stack.append(name)

def render(el, ci, step):
    lines = [f"{ci}<dependency>"]
    for c in el:
        tag, text = local(c.tag), (c.text or "").strip()
        if tag == "version" and bom_junit and (coords(el)[0] or "").startswith("org.junit"):
            continue
        lines.append(f"{ci}{step}<{tag}>{text}</{tag}>")
    lines.append(f"{ci}</dependency>")
    return "\n".join(lines) + "\n"

if close_at is not None:
    line_start = src.rfind("\n", 0, close_at) + 1
    indent = src[line_start:close_at] if not src[line_start:close_at].strip() else ""
    prev = re.findall(r"\n([ \t]*)<dependency>", src[:close_at])
    ci = prev[-1] if prev and len(prev[-1]) > len(indent) else indent + "  "
    step = ci[len(indent):] or "  "
    block = "".join(render(w, ci, step) for w in missing)
    out = src[:line_start] + block + src[line_start:]
else:
    end = src.rfind("</project>")
    line_start = src.rfind("\n", 0, end) + 1
    block = "  <dependencies>\n" + "".join(render(w, "    ", "  ") for w in missing) + "  </dependencies>\n"
    out = src[:line_start] + block + src[line_start:]
pom.write_text(out, encoding="utf-8")
print("adding to pom.xml <dependencies>:", " ".join(":".join(coords(w)) for w in missing))
PY
```

#### env-verify

Run after `dep-sync`; never builds or installs anything (`dependency:list` runs offline). It stops when: `./mvnw` is missing; the JDK (`$JAVA_HOME/bin/java`, else `java` on `PATH`) is older than `<release>`, or newer than it while the pom declares `rewrite-maven-plugin` (OpenRewrite cannot parse on a newer JDK); `rewrite-maven-plugin` or `jacoco-maven-plugin` is not declared (the `lint` and `test` gates call them by prefix); Docker is unreachable while the pom declares Testcontainers; a dev-dep is not on the resolved test classpath; or `junit-jupiter` resolves to a different major than the rule's (the tests would run on an engine this rule never validated). Any other dev-dep on a different major, a newer JDK without OpenRewrite, and an unreachable Docker without Testcontainers only warn.

```bash
python3 - "{DEV_DEPS_FILE}" <<'PY'
import os, re, shutil, subprocess, sys, tempfile, xml.etree.ElementTree as ET
from pathlib import Path

def local(tag):
    return tag.rsplit("}", 1)[-1]

def coords(el):
    kids = {local(c.tag): (c.text or "").strip() for c in el}
    return kids.get("groupId"), kids.get("artifactId"), kids.get("version")

stops, warns = [], []
pom = Path("pom.xml")
if not pom.is_file():
    print(f"no pom.xml in {Path.cwd()}")
    sys.exit(1)
src = pom.read_text(encoding="utf-8")
root = ET.fromstring(src)
if not os.access("mvnw", os.X_OK):
    stops.append("./mvnw is missing or not executable — every gate runs Maven through the wrapper")

props = {local(e.tag): (e.text or "").strip() for p in root if local(p.tag) == "properties" for e in p}
release = None
for e in root.iter():
    if local(e.tag) in ("release", "maven.compiler.release"):
        v = (e.text or "").strip()
        v = props.get(v[2:-1], v) if v.startswith("${") else v
        if v.isdigit():
            release = int(v)
            break
uses_rewrite = "rewrite-maven-plugin" in src
uses_jacoco = "jacoco-maven-plugin" in src
uses_tc = "org.testcontainers" in src
if not uses_rewrite:
    stops.append("rewrite-maven-plugin is not declared in pom.xml — the lint gate runs rewrite:dryRun (see rewrite-snippet)")
if not uses_jacoco:
    stops.append("jacoco-maven-plugin is not declared in pom.xml — the test gate runs jacoco:report (see jacoco-snippet)")

java = os.path.join(os.environ["JAVA_HOME"], "bin", "java") if os.environ.get("JAVA_HOME") else shutil.which("java")
if not java or not os.path.exists(java):
    stops.append("no JDK found: set JAVA_HOME or put java on PATH")
else:
    out = subprocess.run([java, "-XshowSettings:properties", "-version"], capture_output=True, text=True).stderr
    m = re.search(r"java\.specification\.version = (\d+)", out)
    major = int(m.group(1)) if m else 0
    if release and major < release:
        stops.append(f"JDK {major} ({java}) is older than <release>{release}</release> — set JAVA_HOME to a JDK {release}")
    elif release and major > release:
        msg = f"JDK {major} ({java}) is newer than <release>{release}</release>"
        if uses_rewrite:
            stops.append(msg + f" — OpenRewrite cannot construct its Java parser on it; set JAVA_HOME to a JDK {release}")
        else:
            warns.append(msg)

if shutil.which("docker") is None or subprocess.run(["docker", "info"], capture_output=True, timeout=60).returncode != 0:
    (stops if uses_tc else warns).append("Docker is not reachable (`docker info` failed) — Testcontainers tests cannot start")

fd, listing = tempfile.mkstemp(suffix=".txt")
os.close(fd)
r = subprocess.run(["./mvnw", "-q", "-o", "dependency:list", "-DincludeScope=test", "-DoutputFile=" + listing, "-DappendOutput=false"],
                   capture_output=True, text=True)
resolved = {}
if r.returncode != 0:
    stops.append("./mvnw -o dependency:list failed — dep-sync did not resolve the test classpath: "
                 + " ".join((r.stdout + r.stderr).strip().splitlines()[-2:])[:300])
else:
    for line in Path(listing).read_text(encoding="utf-8").splitlines():
        parts = line.split(" -- ")[0].strip().split(":")
        if len(parts) >= 5:
            resolved[(parts[0], parts[1])] = parts[-2]
os.unlink(listing)

if r.returncode == 0:
    for w in ET.fromstring("<deps>" + Path(sys.argv[1]).read_text(encoding="utf-8") + "</deps>"):
        g, a, v = coords(w)
        have = resolved.get((g, a))
        if have is None:
            stops.append(f"{g}:{a} is in the rule's dev-deps but not on the resolved test classpath")
        elif have.split(".")[0] != v.split(".")[0]:
            msg = f"{g}:{a}: resolved {have} is a different major than the rule's {v}"
            (stops if a == "junit-jupiter" else warns).append(msg)

print("\n".join(stops or warns))
sys.exit(1 if stops else 0)
PY
```

#### pom-snippet

The compiler release, Surefire with the JaCoCo agent line, JaCoCo's agent bound to the build, and the OpenRewrite plugin whose dry run is the lint gate. Recipe modules go in the plugin's `<dependencies>`; the active recipe is defined in `rewrite.yml` (below). `@{argLine}` lets JaCoCo prepend its agent; keep it first if you add JVM flags.

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <version>3.16.0</version>
      <configuration>
        <release>25</release>
      </configuration>
    </plugin>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-surefire-plugin</artifactId>
      <version>3.6.1</version>
      <configuration>
        <argLine>@{argLine}</argLine>
      </configuration>
    </plugin>
    <plugin>
      <groupId>org.jacoco</groupId>
      <artifactId>jacoco-maven-plugin</artifactId>
      <version>0.8.16</version>
      <executions>
        <execution>
          <goals>
            <goal>prepare-agent</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
    <plugin>
      <groupId>org.openrewrite.maven</groupId>
      <artifactId>rewrite-maven-plugin</artifactId>
      <version>6.46.1</version>
      <configuration>
        <activeRecipes>
          <recipe>com.example.GeneralCleanup</recipe>
        </activeRecipes>
      </configuration>
      <dependencies>
        <dependency>
          <groupId>org.openrewrite.recipe</groupId>
          <artifactId>rewrite-migrate-java</artifactId>
          <version>3.42.1</version>
        </dependency>
      </dependencies>
    </plugin>
  </plugins>
</build>
```

#### rewrite-snippet

`rewrite.yml` at the project root. Keep recipes that agree with prettier-plugin-java's output (no OpenRewrite formatting recipes such as `AutoFormat`: prettier owns layout). Add `org.openrewrite.java.testing.assertj.JUnitToAssertj` (with `rewrite-testing-frameworks` in the plugin's dependencies) and the `rewrite-logging-frameworks` SLF4J recipes once the project uses AssertJ and SLF4J throughout.

```yaml
---
type: specs.openrewrite.org/v1beta/recipe
name: com.example.GeneralCleanup
recipeList:
  - org.openrewrite.java.RemoveUnusedImports
  - org.openrewrite.java.OrderImports
  - org.openrewrite.java.ShortenFullyQualifiedTypeReferences
  - org.openrewrite.java.migrate.UpgradeToJava21
```

#### prettier-snippet

`.prettierrc.json` (or the equivalent `.prettierrc.js`). The `format-check` gate supplies prettier-plugin-java itself, so the config needs no `plugins` entry; if it has one that uses `require.resolve(...)`, the gate's `NODE_PATH` makes it resolve.

```json
{
  "printWidth": 160,
  "tabWidth": 2,
  "useTabs": false,
  "trailingComma": "none"
}
```

### Layout

How the workflow discovers roots without assuming a single-module project. Maven gates find their own roots from `pom.xml`; only `format-check` substitutes them.

- **`{SRC_ROOTS}`** — the main source directory of every module: `<build><sourceDirectory>` if the pom sets it, else `src/main/java`. In a multi-module build, one entry per module that has one (`core/src/main/java api/src/main/java`), space-separated. A design may declare explicit roots in `design/implementation.md` under a `## Source Roots` block; that overrides discovery.
- **`{TEST_ROOTS}`** — `<build><testSourceDirectory>` if set, else `src/test/java`, per module as above.
- **`{SCRIPTS_ROOTS}`** — omitted; build logic lives in `pom.xml`, not in scripts the gates check.
- **`{config_root}`** — the directory holding the aggregator `pom.xml` and `mvnw`; workflows discover it by walking up from `cwd` to the topmost directory that has both.

### Quality Thresholds

| Threshold                            | Value                                                                                                                                                                                  | Where applied                                                                                                        |
| ------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| `coverage_overall`                   | N/A — no global threshold by policy; the `test` gate writes the JaCoCo report (`target/site/jacoco/jacoco.xml`, `jacoco.csv`) as evidence, and `{OVERALL}` is not substituted anywhere | `test` gate (report only)                                                                                            |
| `coverage_per_file`                  | N/A — instead, every production class added or changed by the delta must be exercised by a test (non-zero covered lines in `jacoco.csv`)                                               | Verifier, via the Coverage parsing helper                                                                            |
| `test_growth_floor`                  | `src_lines_added / 5`                                                                                                                                                                  | If `src_lines_added > 50` and `tests_added < src_lines_added / 5`, the verifier emits a `test-growth-floor` finding. |
| `test_cap_initial`                   | 300 (seconds)                                                                                                                                                                          | Default `{TEST_CAP}` for the `test-baseline` and `test` gates.                                                       |
| `test_cap_escalated`                 | 900 (seconds)                                                                                                                                                                          | One-time escalation if the initial cap is hit (container start-up dominates cold runs).                              |
| `per_test_timeout_initial`           | 10 (seconds)                                                                                                                                                                           | Default `{PER_TEST}` in `test-targeted` (`junit.jupiter.execution.timeout.testable.method.default`).                 |
| `per_test_timeout_escalation_ladder` | `10 → 30 → 90`                                                                                                                                                                         | Per the retry ladder.                                                                                                |

### Testing Anti-patterns

Regexes the verifier greps test files for (`git diff --name-only main...HEAD -- '{TEST_ROOTS}/**/*.java'`). Each match is a finding `{file, line, pattern_name, snippet}`. In the regex column, `|` is written as `\|` for markdown-table safety; unescape `\|` → `|` before compiling (Python flavour).

| `pattern_name`            | Regex                                                                                                           | When it fires                                                                                                                                                   |
| ------------------------- | --------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `empty-test-body`         | `(?ms)@(Test\|ParameterizedTest\|RepeatedTest)\b[^{;]*?\bvoid\s+\w+\s*\([^)]*\)\s*(throws\s+[\w.,\s]+)?\{\s*\}` | A test method whose body is empty.                                                                                                                              |
| `skip-without-issue-link` | `@Disabled\b(?!\s*\(\s*"[^"]*(#\d+\|https?://)[^"]*"\s*\))`                                                     | A `@Disabled` without a reason that carries an issue reference (`#123` or a URL).                                                                               |
| `assert-not-null-only`    | `^\s*(assertThat\(.+\)\.isNotNull\(\)\|assertNotNull\(.+\));\s*$`                                               | Only when this is the SOLE assertion in the test method (verifier counts assertions per method).                                                                |
| `assert-true-literal`     | `assert(True\|That)\(\s*true\s*\)`                                                                              | Tautological assertion.                                                                                                                                         |
| `sut-mock`                | `\b(mock\|spy)\(\s*([A-Z]\w*)\.class\s*\)`                                                                      | The mocked class is the class under test (`Foo` mocked in `FooTest`) — mock collaborators, not the SUT.                                                         |
| `repository-mock`         | `\bmock\(\s*\w*(Repository\|Port)\.class\s*\)`                                                                  | Advisory: a repository or output port is mocked where the policy prefers a real implementation (Testcontainers) or a fake. Report for review, do not auto-fail. |
| `sleep-in-test`           | `\bThread\.sleep\(`                                                                                             | A test waits on wall-clock time instead of an observable condition (use Awaitility).                                                                            |
| `system-out-in-test`      | `\bSystem\.(out\|err)\.print`                                                                                   | Advisory: printing instead of asserting.                                                                                                                        |
| `no-asserts`              | (no regex — verifier counts)                                                                                    | A test method with no `assertThat`/`assert*` call, no `assertThrows`/`assertThatThrownBy`, and no Mockito `verify(`.                                            |

### Gitignore Patterns

- `target/`
- `*.class`
- `*.log`
- `dependency-reduced-pom.xml`
- `.mvn/wrapper/maven-wrapper.jar`
- `.flattened-pom.xml`

### Coverage parsing helper

Enforces the "new or changed code comes with tests" policy from the JaCoCo CSV the `test` gate writes: pass the production files the delta added or changed (`git diff --name-only main...HEAD -- '{SRC_ROOTS}/**/*.java'`); it fails for any whose top-level class has no covered line. With no arguments it only lists classes with zero covered lines.

```python
import csv
import sys
from pathlib import Path

REPORT = Path("target/site/jacoco/jacoco.csv")
changed = {Path(p).with_suffix("").as_posix() for p in sys.argv[1:]}

covered: dict[str, int] = {}
with REPORT.open(encoding="utf-8") as f:
    for row in csv.DictReader(f):
        cls = row["CLASS"].split(".")[0]          # Outer.Inner -> Outer
        key = f"{row['PACKAGE'].replace('.', '/')}/{cls}"
        covered[key] = covered.get(key, 0) + int(row["LINE_COVERED"])

uncovered = sorted(k for k, n in covered.items() if n == 0)
failures = [c for c in sorted(changed) if not any(c.endswith(k) for k, n in covered.items() if n > 0)]
if uncovered:
    print("Classes with no covered line:")
    for k in uncovered:
        print(f"- {k}")
if failures:
    print("Changed production files without test coverage:")
    for c in failures:
        print(f"- {c}.java")
    sys.exit(1)
print("OK: every changed production file is exercised by a test")
```

## Platform facts

- OpenRewrite (`rewrite-maven-plugin` 6.46.1) fails on JDK 27 with `Unable to construct Java25Parser … JavaCompiler does not have member field 'boolean genEndPos'`; the same dry run passes under JDK 25. Run Maven with `JAVA_HOME` set to a JDK matching `<release>` (source: `./mvnw rewrite:dryRun` on a release-25 project, Temurin 27.0.1 vs GraalVM CE 25.0.2, 2026-11-14).
- The dry-run failure switch is the user property `failOnDryRunResults` — `-Drewrite.failOnDryRunResults=true` is silently ignored; misconfigured recipes only fail with `-Drewrite.failOnInvalidActiveRecipes=true` (source: `./mvnw help:describe -Dplugin=org.openrewrite.maven:rewrite-maven-plugin:6.46.1 -Dgoal=dryRun -Ddetail`).
- Under `npx -p prettier-plugin-java`, `prettier --plugin=prettier-plugin-java` fails with `Cannot find package 'prettier-plugin-java' imported from …/noop.js`, and a `.prettierrc.js` that calls `require.resolve("prettier-plugin-java")` cannot see npx's install either; passing the resolved absolute path and exporting `NODE_PATH` fixes both (source: running the format-check gate in a scratch project and in the LinkLift repo, 2026-11-14).
- `-Djacoco.skip=true` still lets a Surefire `<argLine>@{argLine}</argLine>` resolve, so the fast baseline run needs no pom change (source: `./mvnw -q test -Djacoco.skip=true` on the smoke project, 2026-11-14).
- JUnit configuration parameters passed as `-D` on the Maven command line reach the Surefire fork; `junit.jupiter.execution.timeout.testable.method.default` times out test methods but not `@BeforeAll` (source: a 2 s `@BeforeAll` + 3 s `@Test` run with the key at `1s` — only the test timed out, 2026-11-14).
- `pre-commit run prettier` in this repo runs with `--write`; it reformats instead of checking, so it is not a usable gate (source: `.pre-commit-config.yaml`, `args: [--write, --list-different, --ignore-unknown]`).
