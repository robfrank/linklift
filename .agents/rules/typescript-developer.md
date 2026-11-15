---
description: TypeScript Developer rule (LinkLift webapp) — React 19 + TypeScript in `webapp/`, npm with package-lock.json, prettier 3.3.3 via npx (lint + format), tsc --noEmit (types), Jest 30 + babel-jest + jsdom + React Testing Library (tests), Jest coverage report (no threshold). Chosen at Step 1.5 as the readiness lens for webapp deltas.
paths:
  - "webapp/**/*.ts"
  - "webapp/**/*.tsx"
  - "webapp/**/*.js"
  - "webapp/**/*.jsx"
  - "webapp/package.json"
  - "webapp/package-lock.json"
  - "webapp/tsconfig*.json"
  - "webapp/jest.config.*"
  - "webapp/.babelrc"
  - "webapp/babel.config.*"
  - "webapp/webpack.config.*"
smoke_tested: 2026-11-15 · cd · ts-smoke
---

# TypeScript Developer Rule

This rule targets the LinkLift webapp: React 19 with TypeScript 7 (new code) and JavaScript (legacy code being migrated), living in `webapp/` beneath the repository root, with npm + `package-lock.json` (packaging) + prettier 3.3.3 through npx with explicit CLI options (lint + format) + `tsc --noEmit` against `webapp/tsconfig.json` (types) + Jest 30 with babel-jest, jest-environment-jsdom and React Testing Library (tests) + Jest's built-in coverage (report only, no threshold). The bundle is built by webpack + Babel 8.

## Purpose

The **TypeScript Developer (TSD)** writes production-ready, maintainable TypeScript for the LinkLift single-page app: strict types, one formatter, and behaviour-first tests that query the DOM the way a user would. This repo-local rule shadows the skill's bundled `typescript-developer` (Bun + Biome + vitest), which does not match this project: here the toolchain is npm, prettier, Jest and Babel, and the project is not at the repository root.

Every gate runs from the **repository root** and enters `webapp/` itself (`cd webapp && …`), because that is how the skill runs gates. The gate commands are the ones the user approved for the webapp in `webapp/design/rule-overrides.yaml`.

"Green" means every gate below passes in order: `npm ci` installs exactly the lockfile, prettier finds nothing to reformat under `webapp/src`, `tsc --noEmit` reports no type error, the Jest suite passes, and the coverage run writes its report to `webapp/coverage/`. Coverage is reported, not thresholded: new or changed production code must arrive with tests (see Testing policy), which the verifier and the reviewers enforce.

## Core Principles

### 1. Architecture: hexagonal on the frontend too

- `domain/` holds models (`domain/models`), ports (`domain/ports`, repository interfaces) and use cases (`domain/usecases`) with no React, Axios or browser imports.
- `application/` implements the ports (`application/services/Api*Repository.ts`, Axios-based), wires them in `application/di-container.ts`, and holds the Zustand store slices (`application/state/*Slice.ts`). Import repositories and use cases from the DI container, never straight from a service file.
- `infrastructure/ui/` holds pages, reusable components, layouts and hooks. Components call use cases or store actions; they do not call Axios.
- Legacy code in `components/`, `hooks/`, `services/` and `contexts/` is being migrated: new code goes in the hexagonal folders; a change that touches legacy code does not have to migrate it, but must not add new legacy-style modules.

### 2. Strict types

- `strict: true` in `webapp/tsconfig.json` is the floor. New code is `.ts`/`.tsx`; JavaScript is accepted only in legacy files being edited in place.
- No `any` without a written justification; prefer `unknown` and narrow it. No non-null `!` on values that can legitimately be absent. No `@ts-ignore` — use `@ts-expect-error` with a reason, and only when the runtime contract genuinely outruns the types.
- Model data with discriminated unions and `readonly` fields, not optional-property soup. Parse at the boundary (API responses) into a typed shape once, then trust the type inward.
- `tsconfig.json` must stay compatible with TypeScript 7: options TypeScript 7 removed (`target: "es5"`, `moduleResolution: "node"`/`node10`) fail the `type-check` gate (see Platform facts). Babel, not `tsc`, produces the bundle, so `tsc` only checks (`noEmit`).

### 3. React, state and I/O

- Function components and hooks only. MUI components and Emotion for styling — no raw CSS files for new UI.
- Data state lives in the Zustand slices; React context is for cross-cutting concerns only (auth, snackbar).
- Prefer `async`/`await` for I/O. Every promise is awaited or has a `.catch` — no unhandled rejection from a fire-and-forget call.
- Sanitize any user-supplied or fetched HTML with DOMPurify before rendering it. Never put tokens or personal data in logs; no stray `console.log` in shipped code.
- Errors are values with context: throw `Error` subclasses carrying the offending input, do not throw strings.

### 4. Dependencies

- **npm with the committed `webapp/package-lock.json`.** `npm ci` in CI and in the `dep-sync` gate; a change to `package.json` ships with the matching lockfile change.
- Bounded ranges (`^`) in `package.json`; exact pins come from the lockfile. Dev tooling lives in `devDependencies`; never ship a dev-only package in `dependencies`.
- Babel 8 sets the floor for Node: `^22.18.0 || >=24.11.0` (checked by `env-verify`).

### 5. Code quality and formatting

- Prettier is the single source of truth for formatting, with the repo's settings (`printWidth: 160`, `trailingComma: "none"`, two-space indent). The repo-root `.prettierrc.js` requires the Java and XML plugins, so for the webapp the gates pass those settings as CLI flags with `--no-config` instead of loading it (see Platform facts). Do not add a second formatter.
- There is no separate linter configured for the webapp; the `lint` gate is the same prettier check, by the user's decision recorded in `webapp/design/rule-overrides.yaml`.
- Readability over cleverness. Small modules with one responsibility. Name things for what they mean, not their type.

### 6. Testing policy

- **Jest + React Testing Library, behaviour first.** Query by role, label or text (`getByRole`, `getByLabelText`, `findByText`), not by test id or implementation detail; interact through `@testing-library/user-event`; assert with `@testing-library/jest-dom` matchers.
- **Mock at the network, not the module.** API calls are mocked with MSW handlers; do not `jest.mock` the system under test, and avoid `jest.mock("axios")` where an MSW handler would do. Mock true collaborators only (e.g. `react-force-graph-2d`, already mapped in `jest.config.js`).
- **Every public use case, store action, repository and component** gets a happy path with realistic input and a full output assertion, at least one failure path (API error, empty result, invalid input), and the boundary cases.
- **New or changed code comes with tests.** There is no global coverage threshold; a change that adds production code without adding or changing tests is a finding. The coverage report the `test` gate writes is evidence for reviewers, not a gate.
- Asynchronous outcomes are awaited with `findBy*`/`waitFor`, never with a wall-clock sleep. Tests live in `__tests__/` folders or colocated as `*.test.(js|jsx|ts|tsx)`; global setup is `src/setupTests.js`.

## Rule Contract Sections

### Gate Commands

Placeholders `{SRC_ROOTS}`, `{TEST_ROOTS}`, `{SCRIPTS_ROOTS}` come from Layout; `{TEST_CAP}`, `{PER_TEST}`, `{OVERALL}`, `{TEST_ID}` from Quality Thresholds and the invocation. This rule is repo-local and the webapp sits at a fixed `webapp/`, so every gate runs from the repository root, changes into `webapp/` itself, and names `src` relative to it: the prettier gates therefore check exactly `{SRC_ROOTS}` (= `webapp/src`, tests included, see Layout) without substituting it, and Jest and `tsc` discover their own roots from `jest.config.js` and `tsconfig.json`. The commands are the user-approved ones from `webapp/design/rule-overrides.yaml`, verbatim after the `cd`.

#### dep-sync

Installs exactly what `webapp/package-lock.json` pins; fails when `package.json` and the lockfile disagree.

```bash
cd webapp && npm ci
```

#### lint

The webapp has no linter configured; by the user's approved override the lint gate is the prettier check (same command as `format-check`).

```bash
cd webapp && npx --yes prettier@3.3.3 --check --no-config --print-width 160 --trailing-comma none src
```

#### format-check

Prettier 3.3.3 (the version the repo's pre-commit pins) with the repo's settings as CLI flags; `--no-config` because the root `.prettierrc.js` requires plugins npx does not install. Check only — never `--write`.

```bash
cd webapp && npx --yes prettier@3.3.3 --check --no-config --print-width 160 --trailing-comma none src
```

#### type-check

`tsc` from the webapp's own `node_modules/typescript` (checked by `env-verify`) against `webapp/tsconfig.json`. For the `fix-webapp-tests` delta this gate is owner-run (override `runs: owner`), because the current tsconfig still carries options TypeScript 7 removed; the gate stays declared so it runs again once that is fixed.

```bash
cd webapp && npx tsc --noEmit
```

#### test-baseline

A fast pass/fail run with no coverage, used by apply mode's STEP 0 to confirm the prior phase's main is green before mutating anything.

```bash
cd webapp && npm test
```

#### test

The full suite with Jest's coverage (`jest --coverage` via the `test:coverage` script), written to `webapp/coverage/` (`lcov.info`, `coverage-final.json`, `clover.xml`, `lcov-report/`). No coverage threshold: `{OVERALL}` is not used (see Quality Thresholds).

```bash
cd webapp && npm run test:coverage
```

#### test-targeted

For re-running one failing or timing-out test in the retry ladder. `{TEST_ID}` is a Jest path pattern, optionally with a name filter (`src/components/__tests__/Header.test.js`, or `Header -t "renders the title"`); `{PER_TEST}` is the per-test timeout in seconds, rendered in milliseconds for Jest.

```bash
cd webapp && npx jest --testTimeout={PER_TEST}000 {TEST_ID}
```

### Baseline

Test libraries the webapp MUST carry. At the apply crossing (and `env-check`) the skill runs `dep-baseline` below, which adds the missing ones, and commits what it changed as one `chore: typescript-developer baseline`; then `dep-sync`, then `env-verify`. On LinkLift's webapp every dev-dep below is already declared, so `dep-baseline` changes nothing there. The config snippets stay a pre-flight judgement.

#### dev-deps

Bare package names with ranges (the ranges LinkLift's webapp declares); `dep-baseline` adds the missing ones to `webapp/package.json` `devDependencies`.

```json
{
  "jest": "^30.0.5",
  "babel-jest": "^30.0.5",
  "jest-environment-jsdom": "^30.0.5",
  "@testing-library/react": "^16.3.0",
  "@testing-library/jest-dom": "^7.0.0"
}
```

#### dep-baseline

Adds every dev-dep above that `webapp/package.json` does not declare anywhere (dependencies, devDependencies, peer or optional) to `devDependencies` with `npm install --save-dev`, which also updates `webapp/package-lock.json`. A package the webapp already declares is never touched, whatever its range. Idempotent: with nothing missing it changes nothing and runs no npm command. A 401/403 (or E401/E403) from the registry means credentials for a private registry: check the scopes and `_authToken` entries in `webapp/.npmrc` / `~/.npmrc`.

```bash
cd webapp && node - "{DEV_DEPS_FILE}" <<'JS'
const fs = require("fs")
const { spawnSync } = require("child_process")
if (!fs.existsSync("package.json")) { console.log(`no package.json in ${process.cwd()}`); process.exit(1) }
const want = JSON.parse(fs.readFileSync(process.argv[2], "utf8"))
const pkg = JSON.parse(fs.readFileSync("package.json", "utf8"))
const have = new Set([pkg.dependencies, pkg.devDependencies, pkg.peerDependencies, pkg.optionalDependencies]
  .flatMap((o) => Object.keys(o || {})))
const missing = Object.entries(want).filter(([n]) => !have.has(n)).map(([n, r]) => `${n}@${r}`)
if (missing.length) {
  console.log("adding to webapp devDependencies:", missing.join(" "))
  const r = spawnSync("npm", ["install", "--save-dev", "--no-audit", "--no-fund", ...missing], { stdio: "inherit" })
  process.exit(r.status === null ? 1 : r.status)
}
JS
```

#### env-verify

Run after `dep-sync`; never installs anything. It stops when: `node` is missing or outside Babel 8's engines (`^22.18.0 || >=24.11.0`); `webapp/package.json` or `webapp/package-lock.json` is missing; a dev-dep or `typescript` is not installed in `webapp/node_modules`; an installed dev-dep differs from the version the lockfile pins (node_modules not synced — run `npm ci`); `npm ls --depth=0` reports a problem for the dev-deps; or `jest`, `babel-jest` or `jest-environment-jsdom` is on a different major than the rule's (the tests would run on a runner this rule never validated). Any other dev-dep on a different major only warns.

```bash
command -v node >/dev/null 2>&1 || { echo "node is not on PATH — the webapp needs Node ^22.18.0 || >=24.11.0"; exit 1; }
command -v npm >/dev/null 2>&1 || { echo "npm is not on PATH — dep-sync and the test gates run npm"; exit 1; }
[ -d webapp ] || { echo "no webapp/ directory under $(pwd) — this rule's project lives in webapp/"; exit 1; }
cd webapp && node - "{DEV_DEPS_FILE}" <<'JS'
const fs = require("fs")
const { spawnSync } = require("child_process")
const stops = [], warns = []
const [maj, min] = process.versions.node.split(".").map(Number)
if (!((maj === 22 && min >= 18) || (maj === 24 && min >= 11) || maj > 24))
  stops.push(`node ${process.versions.node} is outside Babel 8's engines ^22.18.0 || >=24.11.0`)
for (const f of ["package.json", "package-lock.json"])
  if (!fs.existsSync(f)) stops.push(`webapp/${f} is missing`)
const want = JSON.parse(fs.readFileSync(process.argv[2], "utf8"))
let locked = {}
try { locked = JSON.parse(fs.readFileSync("package-lock.json", "utf8")).packages || {} } catch {}
const installed = (n) => {
  try { return JSON.parse(fs.readFileSync(`node_modules/${n}/package.json`, "utf8")).version } catch { return null }
}
if (!installed("typescript"))
  stops.push("typescript is not installed in webapp/node_modules — the type-check gate runs the local tsc")
const runner = new Set(["jest", "babel-jest", "jest-environment-jsdom"])
for (const [n, r] of Object.entries(want)) {
  const v = installed(n)
  if (!v) { stops.push(`${n} ${r} is in the rule's dev-deps but not installed in webapp/node_modules`); continue }
  const lv = (locked[`node_modules/${n}`] || {}).version
  if (lv && lv !== v) stops.push(`${n}: installed ${v} but package-lock.json pins ${lv} — node_modules is not synced (run npm ci)`)
  const wantMajor = r.replace(/^[^0-9]*/, "").split(".")[0]
  if (v.split(".")[0] !== wantMajor) {
    const msg = `${n}: installed ${v} is a different major than the rule's ${n}@${r}`
    ;(runner.has(n) ? stops : warns).push(msg)
  }
}
const ls = spawnSync("npm", ["ls", "--depth=0", ...Object.keys(want)], { encoding: "utf8" })
if (ls.status !== 0)
  stops.push("npm ls --depth=0 reports problems: " + ((ls.stderr || "") + (ls.stdout || "")).trim().split("\n").filter((l) => /ERR|invalid|missing|extraneous/i.test(l)).slice(0, 3).join(" | "))
console.log((stops.length ? stops : warns).join("\n"))
process.exit(stops.length ? 1 : 0)
JS
```

#### package-json-snippet

The scripts the `test-baseline` and `test` gates call (`webapp/package.json`).

```json
{
  "scripts": {
    "test": "jest",
    "test:coverage": "jest --coverage"
  }
}
```

#### jest-config-snippet

`webapp/jest.config.js`: jsdom, babel-jest for every source extension, a setup file for `@testing-library/jest-dom`, and coverage collected from `src/`.

```js
module.exports = {
  testEnvironment: "jsdom",
  setupFilesAfterEnv: ["<rootDir>/src/setupTests.js"],
  testMatch: ["**/__tests__/**/*.{js,jsx,ts,tsx}", "**/?(*.)+(spec|test).{js,jsx,ts,tsx}"],
  collectCoverageFrom: ["src/**/*.{js,jsx,ts,tsx}", "!src/index.tsx", "!src/setupTests.js", "!**/node_modules/**"],
  transform: {
    "^.+\\.(js|jsx|ts|tsx)$": "babel-jest"
  }
};
```

#### babel-snippet

`webapp/.babelrc` (or `babel.config.js`): Babel transpiles for both webpack and Jest.

```json
{
  "presets": ["@babel/preset-env", ["@babel/preset-react", { "runtime": "automatic" }], "@babel/preset-typescript"]
}
```

#### tsconfig-snippet

`webapp/tsconfig.json`, TypeScript 7 compatible (no `target: "es5"`, no `moduleResolution: "node"`).

```json
{
  "compilerOptions": {
    "target": "es2022",
    "lib": ["dom", "dom.iterable", "esnext"],
    "allowJs": true,
    "skipLibCheck": true,
    "esModuleInterop": true,
    "strict": true,
    "forceConsistentCasingInFileNames": true,
    "noFallthroughCasesInSwitch": true,
    "module": "esnext",
    "moduleResolution": "bundler",
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx"
  },
  "include": ["src"]
}
```

### Layout

The project root for every gate is `webapp/` (the `{config_root}`); paths below are given from the repository root.

- **`{SRC_ROOTS}`** — `webapp/src`. Production code and colocated tests share it; the prettier gates check it as `src` after `cd webapp`. A design may declare explicit roots in `design/implementation.md` (or `webapp/design/implementation.md`) under a `## Source Roots` block; that overrides discovery for the verifier's scans, while the gates keep checking `webapp/src`.
- **`{TEST_ROOTS}`** — the `__tests__/` directories and `*.test.(js|jsx|ts|tsx)` / `*.spec.*` files under `webapp/src`, as `jest.config.js` `testMatch` defines them; there is no separate top-level test directory.
- **`{SCRIPTS_ROOTS}`** — omitted; build logic lives in `webpack.config.js` and `package.json`, which the gates do not check.
- **`{config_root}`** — `webapp/`, the directory holding `package.json`, `package-lock.json`, `jest.config.js`, `.babelrc` and `tsconfig.json`. Workflows run the gates from the repository root; the gates `cd` into it.

### Quality Thresholds

| Threshold                            | Value                                                                                                                                                                       | Where applied                                                                                                                                      |
| ------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- |
| `coverage_overall`                   | N/A — no global threshold by policy; the `test` gate writes the Jest coverage report (`webapp/coverage/lcov.info`) as evidence, and `{OVERALL}` is not substituted anywhere | `test` gate (report only)                                                                                                                          |
| `coverage_per_file`                  | N/A — instead, every production file added or changed by the delta must be exercised by a test (non-zero covered lines in `lcov.info`)                                      | Verifier, via the Coverage parsing helper                                                                                                          |
| `test_growth_floor`                  | `src_lines_added / 5`                                                                                                                                                       | If `src_lines_added > 50` and `tests_added < src_lines_added / 5`, the verifier emits a `test-growth-floor` finding.                               |
| `test_cap_initial`                   | 300 (seconds)                                                                                                                                                               | `{TEST_CAP}`: the wall-clock cap the verifier applies around `test-baseline` and `test` (the approved gate commands are not wrapped in `timeout`). |
| `test_cap_escalated`                 | 900 (seconds)                                                                                                                                                               | One-time escalation if the initial cap is hit.                                                                                                     |
| `per_test_timeout_initial`           | 10 (seconds)                                                                                                                                                                | Default `{PER_TEST}` in `test-targeted` (rendered as `10000` ms for Jest's `--testTimeout`).                                                       |
| `per_test_timeout_escalation_ladder` | `10 → 30 → 90`                                                                                                                                                              | Per the retry ladder.                                                                                                                              |

### Testing Anti-patterns

Regexes the verifier greps changed test files for (`git diff --name-only main...HEAD -- 'webapp/src/**/__tests__/**' 'webapp/src/**/*.test.*'`). Each match is a finding `{file, line, pattern_name, snippet}`. In the regex column, `|` is written as `\|` for markdown-table safety; unescape `\|` → `|` before compiling (Python flavour).

| `pattern_name`            | Regex                                                                                              | When it fires                                                                                                                      |
| ------------------------- | -------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| `empty-test-body`         | `(?ms)^\s*(it\|test)\(\s*["'`]._?["'`]\s_,\s*(async\s*)?\(\s*\)\s*=>\s*\{\s*\}\s\*\)`              | An `it(...)`/`test(...)` callback with an empty body.                                                                              |
| `skip-without-issue-link` | `^\s*(?:(?:it\|test\|describe)\.(?:skip\|todo)\|x(?:it\|test\|describe))\((?!.*(#\d+\|https?://))` | A skipped or todo test whose line carries no issue reference (`#123` or a URL).                                                    |
| `focused-test`            | `^\s*(?:(?:it\|test\|describe)\.only\|f(?:it\|describe))\(`                                        | A focused test left in: the rest of the file silently stops running.                                                               |
| `expect-true-literal`     | `expect\(\s*(true\|false)\s*\)\.toBe\(`                                                            | Tautological assertion on a literal.                                                                                               |
| `sut-mock`                | `jest\.mock\(\s*["']\.\.?/`                                                                        | Advisory: a relative project module is mocked; fires a finding when it is the module under test (mock collaborators, not the SUT). |
| `axios-mock`              | `jest\.mock\(\s*["']axios["']`                                                                     | Advisory: API calls mocked at the module level where the policy prefers an MSW handler.                                            |
| `test-id-query`           | `\b(get\|query\|find)(All)?ByTestId\(`                                                             | Advisory: querying by test id where a role, label or text query would test behaviour.                                              |
| `sleep-in-test`           | `await\s+new\s+Promise\(.*setTimeout`                                                              | A test waits on wall-clock time instead of on an observable condition (`findBy*` / `waitFor`).                                     |
| `no-asserts`              | (no regex — verifier counts)                                                                       | A test body with no `expect(` call.                                                                                                |

### Gitignore Patterns

- `webapp/node_modules/`
- `webapp/coverage/`
- `webapp/dist/`
- `*.tsbuildinfo`

### Coverage parsing helper

Enforces the "new or changed code comes with tests" policy from the `lcov.info` the `test` gate writes. Run it from the repository root and pass the production files the delta added or changed (`git diff --name-only main...HEAD -- 'webapp/src/**/*.ts' 'webapp/src/**/*.tsx' 'webapp/src/**/*.js' 'webapp/src/**/*.jsx'`, minus test files); it fails for any with no covered line. With no arguments it only lists files with zero covered lines.

```python
import sys
from pathlib import Path

REPORT = Path("webapp/coverage/lcov.info")
changed = {Path(p).as_posix().removeprefix("webapp/") for p in sys.argv[1:]}

hit: dict[str, int] = {}
current = None
for line in REPORT.read_text(encoding="utf-8").splitlines():
    if line.startswith("SF:"):
        sf = Path(line[3:])
        parts = sf.parts
        current = Path(*parts[parts.index("src"):]).as_posix() if "src" in parts else sf.as_posix()
        hit.setdefault(current, 0)
    elif line.startswith("LH:") and current is not None:
        hit[current] += int(line[3:])

uncovered = sorted(f for f, n in hit.items() if n == 0)
failures = [c for c in sorted(changed) if hit.get(c, 0) == 0]
if uncovered:
    print("Files with no covered line:")
    for f in uncovered:
        print(f"- webapp/{f}")
if failures:
    print("Changed production files without test coverage:")
    for c in failures:
        print(f"- webapp/{c}")
    sys.exit(1)
print("OK: every changed production file is exercised by a test")
```

## Platform facts

- TypeScript 7 refuses `"target": "es5"` and `"moduleResolution": "node"` with `error TS5108: Option 'target=ES5' has been removed` / `Option 'moduleResolution=node10' has been removed`; LinkLift's `webapp/tsconfig.json` still sets both, so `tsc --noEmit` fails there until they change (source: `npx tsc --noEmit -p` a tsconfig with those options, TypeScript 7.0.2 on Node 26.13.0, 2026-11-15).
- Babel 8 (`@babel/core` / `@babel/preset-env` 8.0.7, the versions the webapp lockfile pins) declares `engines.node: ^22.18.0 || >=24.11.0` (source: `webapp/package-lock.json`, 2026-11-15).
- The repo-root `.prettierrc.js` calls `require.resolve("prettier-plugin-java")` and `require.resolve("@prettier/plugin-xml")`, which a bare `npx prettier` cannot resolve; the webapp gates therefore pass `--no-config --print-width 160 --trailing-comma none`, the config's non-default settings (source: `.prettierrc.js` and the user-approved override in `webapp/design/rule-overrides.yaml`).
- `npm --prefix webapp exec -- <cmd>` runs `<cmd>` in the caller's directory, not in `webapp/`, so the gates use `cd webapp && …` instead (source: `npm --prefix webapp exec -- node -e 'console.log(process.cwd())'` printed the repo root, npm 11.20.0, 2026-11-15).
