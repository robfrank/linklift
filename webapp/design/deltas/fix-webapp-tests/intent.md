# Fix webapp Jest suites broken by Babel 8 (ESM-only presets)
<!-- intent: draft -->

## Problem
The webapp build on trunk is red. In Webapp CI every Jest suite fails before running a test: the `@babel/*` packages were bumped to 8, whose presets are ESM-only, and jest-config's nested `@babel/core` loads them synchronously (`[BABEL]: You appear to be using a native ECMAScript module preset`). CI also pins Node 18, which Babel 8 does not support (it needs Node `^22.18` or `>=24.11`).

## Outcome
- `npm test` runs all 8 existing suites and they pass, locally (Node 26) and in Webapp CI.
- Webapp CI runs on Node 24 LTS.
- `npm run build` keeps producing the same `dist/` output.

## Baseline and rules
Trunk `webapp/design/` at the current head. Rules: typescript-developer. Standards: the repo's `CLAUDE.md` and `webapp/CLAUDE.md`.

## Footprint
**Adds or changes:** Jest loads the Babel 8 configuration and every suite runs (NF-107); Webapp CI runs on Node 24 LTS (NF-108).
**Relies on:** the webpack build transpiles with Babel and no type check (NF-103); the Docker image builds on `node:26-alpine` (NF-104).
**Out of scope:** test content and coverage gaps; the search crash and other known gaps; the backend build (delta `fix-backend-build`).

## Constraints and quality
No change to application behaviour, to test content, or to the Docker image's Node version. Done means all 8 suites (34 cases) pass locally and in CI, and the build output is unchanged.

## Priority and lane
Priority high: a red trunk build that blocks every webapp PR. Light lane.

## Decisions
- One delta per design root — a single delta for both roots, or one per cause, rejected: a delta lives in one root and four is overhead for mechanical fixes · accepted
- Keep Babel 8 and fix how Jest loads it — pinning Babel back to 7 rejected: defers the migration; replacing babel-jest rejected: more change than needed · accepted
- Webapp CI on Node 24 LTS — Node 26 and Node 22 rejected: 24 is the current LTS and satisfies Babel 8 · accepted
- Project standards are the committed `CLAUDE.md` and `webapp/CLAUDE.md` — the conductor style guides rejected: not chosen as standards · accepted

## Open questions
None.

## Amendments
