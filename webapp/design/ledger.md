# Ledger

A manager-readable history of what changed in this system and why: one entry per folded delta, newest first. Notes keep accumulating after the fold via note-delta.py.

## 2026-11-16 · Retire the separate webapp nginx image and deployment (`retire-webapp-image`)

### What changes

The webapp stops having a Docker image, nginx config, Kamal deployment and docker-compose service of its own. Its production bundle ships inside the API image, which the backend design root owns. The build-and-deploy guide, three specification sections describe the single deployment.

### Why

Once `serve-frontend-from-api` lands, the API container serves the bundle, and the separate webapp image and deploy (which never succeeded) describe a deployment that does not exist.

### Impact and risk

- No webapp source, test, dev-server or production-build change.
- Applying before `serve-frontend-from-api` would leave the app without a server; the apply precondition in [implementation.md](implementation.md) guards it.
- The webapp can no longer be deployed independently of the API.

### Changelog

The webapp no longer has its own nginx image or Kamal deployment: its production bundle now ships inside the API container and is deployed with the API. The local Docker Compose stack no longer starts a separate webapp container; run the UI locally with the webpack dev server next to the API.

### Notes

- 2026-11-15 · author — `webapp/.kamal/` (secrets template and sample hooks) is removed with `webapp/config/deploy.yml`: it exists only for that Kamal service.
- 2026-11-15 · author — the `docker-build` script in `webapp/package.json` is removed because it builds the deleted `webapp/Dockerfile`; this is the only `package.json` change and does not touch `npm run build`.
- 2026-11-15 · author — `webapp/README.md`'s "Docker Integration" section is updated, since it describes the removed nginx container.
- 2026-11-15 · author — the spec sections `system-context`, `routing` and `build-config` and all four sections of the build-and-deploy guide are modified, because each states that nginx or the webapp image serves the app.
- 2026-11-15 · author — no ADR is added (the light lane forbids one), so trunk ADR-004 (nginx serves the bundle) stays as written; the owner may want to mark it superseded.
- 2026-11-15 · author — the apply-order constraint is carried by the implementation's apply precondition and test row `T-api-image-builds-webapp`, not by a requirement, because it is an ordering rule rather than observable behaviour.
- 2026-11-15 · author — the commented-out `run docker-build` execution in `pom.xml` is left alone: it is inert and the root file belongs to the backend design root.
- 2026-11-15 · review r1 — trunk `webapp/design/implementation.md` goes stale with this delta and is not in its footprint: the file-table row for `webapp/Dockerfile`, `webapp/nginx.conf`, `webapp/config/deploy.yml`, the `npm run docker-build` row of the commands table (**NF-104**), and the Testing Plan sentence naming `webapp/Dockerfile`; correct them at fold, as for ADR-004.
- 2026-11-15 · gate — review: 3 round(s), lane full — blockers in round(s) 1, 2 — 1 finding(s) were damage from a fix pass.
- 2026-11-16 · round 01 conductor — Removed webapp/Dockerfile, nginx.conf, config/deploy.yml, the 10 tracked webapp/.kamal files, .github/workflows/kamal-deploy-webapp.yml, the compose webapp service, the docker-build npm script and the dependabot docker /webapp entry; README.md now points the local UI at the dev server (http://localhost:3000), webapp/README.md says the bundle ships in the API image.
- 2026-11-16 · round 01 conductor — Gates: npm ci, prettier lint/format-check and npm test / test:coverage (8 suites, 34 tests) green. type-check is owner-run; tsc still reports the known MUI 9 source errors in untouched files.
- 2026-11-16 · round 01 conductor — docker compose warns that the version attribute is obsolete; this predates the delta and was left alone.


### Effort
- implementation rounds: 1
- review rounds: 3
- attempts: 1
- coverage: 29%
- tokens: 918k
- wall-clock elapsed (includes idle, not a work measure): 1h19m
- main agent: 9k generated over 14 messages
- implementers: 6k generated over 2/2 dispatch(es)

- 2026-11-16 · fold — provenance gap: changed on trunk since the last fold with no delta declaring them: `webapp/.kamal/hooks/docker-setup.sample`, `webapp/.kamal/hooks/post-app-boot.sample`, `webapp/.kamal/hooks/post-deploy.sample`, `webapp/.kamal/hooks/post-proxy-reboot.sample`, `webapp/.kamal/hooks/pre-app-boot.sample`, `webapp/.kamal/hooks/pre-build.sample`, `webapp/.kamal/hooks/pre-connect.sample`, `webapp/.kamal/hooks/pre-deploy.sample`, `webapp/.kamal/hooks/pre-proxy-reboot.sample`, `webapp/.kamal/secrets`


## 2026-11-15 · Fix webapp Jest suites broken by Babel 8 (ESM-only presets) (`fix-webapp-tests`)

### What changes
Jest transforms through the project's own Babel 8 core, React DOM is locked to the same version as React, webapp CI moves to Node 24 LTS, and the TypeScript config drops the two option values TypeScript 7 removed. No application source or test file changes.

### Why
Trunk's webapp build is red: every Jest suite fails, which blocks every webapp pull request.

### Impact and risk
Configuration only. The production bundle changes only through the `react-dom` alignment the user approved. `npx tsc` still reports source type errors after this delta; those move to a separate delta and the type-check gate stays owner-run meanwhile.

### Changelog
Webapp tests run again on Babel 8: Jest loads the project's own `babel-jest`, `react-dom` matches `react`, CI runs on Node 24, and the TypeScript config is accepted by TypeScript 7.

### Notes
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


### Effort
- implementation rounds: 1
- review rounds: 1
- attempts: 1
- coverage: 29%
- tokens: 147k
- wall-clock elapsed (includes idle, not a work measure): 1h25m
- main agent: 21k generated over 43 messages
- implementers: 5k generated over 2/2 dispatch(es)

- 2026-11-15 · fold — the design changed DURING implementation: reviewed on the shelf as `7feeb8cba1693b17`, built as `e382515cb2256dd0` (re-reviewed in 1 round(s))
