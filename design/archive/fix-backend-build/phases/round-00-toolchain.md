# Round: Toolchain (java-developer gates green)

### Round Scope
- Requirement IDs: the rounds-table row for `ROUND-00` in `implementation.md`
- Deliverables: prettier layout for the Ask tests (spec `#java-formatting`); the full ArcadeDB image (spec `#test-container-image`); `LoopbackJavalin` and its callers (spec `#loopback-test-apps`). The rule's `dep-baseline` adds nothing: `pom.xml` already declares `junit-jupiter`, `assertj-core` and `mockito-core`.
- Depends on: none

### Round File Layout
The whole of `implementation.md` § Module & File Layout.

### Round Data Changes
None.

### Round Tests
Every row of `implementation.md` § Testing Plan.

### Gates

| Gate | Command | Expectation |
|------|---------|-------------|
| dep-sync | `./mvnw -q dependency:resolve` | green |
| lint | `./mvnw -q rewrite:dryRun -DfailOnDryRunResults=true -Drewrite.failOnInvalidActiveRecipes=true` | override: owner |
| format-check | java-developer `format-check` with `{SRC_ROOTS}` = `src/main/java`, `{TEST_ROOTS}` = `src/test/java` | green |
| type-check | `./mvnw -q -DskipTests compile test-compile` | green |
| test-baseline | `timeout 300s ./mvnw -q test -Djacoco.skip=true` | green |
| test | `timeout 300s ./mvnw -q test jacoco:report` | green |

The lint row follows `design/rule-overrides.yaml` (owner-run, approved by the user for this delta); it is the one gate not required green at apply, so the unused `JavalinTest` imports it would flag are checked by `T-loopback-callers`. If `test` fails only because `VectorSearchE2ETest`'s Ollama image or model pull runs past the cap or cannot reach the network, it is rerun once at the rule's `test_cap_escalated` (900 s); a pull that still fails is recorded in the round summary as the intent's out-of-scope Ollama pull, not as a failure of this delta.
