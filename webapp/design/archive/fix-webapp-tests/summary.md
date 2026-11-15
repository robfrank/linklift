# Fix webapp Jest suites broken by Babel 8 (ESM-only presets)
<!-- footprint: NF-107 modify · NF-108 modify · NF-FWT-001 add -->

## What changes
Jest transforms through the project's own Babel 8 core, React DOM is locked to the same version as React, webapp CI moves to Node 24 LTS, and the TypeScript config drops the two option values TypeScript 7 removed. No application source or test file changes.

## Why
Trunk's webapp build is red: every Jest suite fails, which blocks every webapp pull request.

## Impact and risk
Configuration only. The production bundle changes only through the `react-dom` alignment the user approved. `npx tsc` still reports source type errors after this delta; those move to a separate delta and the type-check gate stays owner-run meanwhile.

## Changelog
Webapp tests run again on Babel 8: Jest loads the project's own `babel-jest`, `react-dom` matches `react`, CI runs on Node 24, and the TypeScript config is accepted by TypeScript 7.

## Notes
- 2026-11-14 · author — `requirements.md` from an earlier authoring pass predated amendments 2 and 3; it was rewritten to add the TypeScript-config requirement and to correct NF-107, whose earlier file plan (no Jest config change) did not turn every suite green.
- 2026-11-14 · author — measured on a scratch copy of the worktree (Node 26.13.0): with only `react-dom` aligned, 7 of 8 suites pass and `src/__tests__/App.test.js` fails inside Babel 7's traverse on a TypeScript enum, because `jest --showConfig` resolves the `babel-jest` transform to the copy nested under `node_modules/jest-config/`, which loads `@babel/core` 7.29.7. Naming `require.resolve("babel-jest")` in `webapp/jest.config.js` makes all 8 suites (34 cases) pass on Node 26 and on Node 24.22.3, with and without coverage. This keeps `babel-jest` and Babel 8 (the intent's accepted decision); a package.json `overrides` entry was the alternative not taken.
- 2026-11-14 · author — `ES2022` and `bundler` chosen for **NF-FWT-001**: they match the `typescript-developer` rule's tsconfig snippet; with them `npx tsc` reports 52 errors in 14 source files and none against the config, matching amendment 3.
- 2026-11-14 · author — the whole change is the round-00 toolchain round (required because `typescript-developer` is new to this design root); every gate runs its approved override, type-check as owner.
- 2026-11-14 · author — baseline `implementation.md` (Testing section) still says CI runs on Node 18 next to **NF-108**; the fold should carry the Node 24 wording.
- 2026-11-14 · fixer — the same file's `webapp/README.md` documentation-conflict row also says "CI: Node 18"; the fold carries Node 24 there too.
- 2026-11-14 · gate — review: 1 round(s), lane light — no blockers.
- 2026-11-15 · gate — review: 1 round(s), lane light — no blockers.
- 2026-11-15 · round 00 conductor — Gates green from webapp/: npm ci, npm run test:coverage (8/8 suites, 34/34 tests on Node 26.13.0), prettier lint/format-check; npm run build produced dist/bundle.js and dist/index.html (89 pre-existing webpack warnings). type-check is owner-run and was not run.
- 2026-11-15 · round 00 conductor — Lockfile diff vs the baseline touches only node_modules/react-dom version, resolved, integrity and peerDependencies (react ^19.3.1); package.json unchanged. jest --showConfig shows the transform at webapp/node_modules/babel-jest/build/index.js.
- 2026-11-15 · round 00 conductor — Not verified locally: the Node 24 Jest run (only Node 26 installed) and T-ci-green; both are proven by Webapp CI on the PR.
- 2026-11-15 · round 00 conductor — Follow-up: webapp/CLAUDE.md still says TypeScript 5.9 (review R1-3, outside this delta).
