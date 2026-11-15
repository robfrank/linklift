# Delta Requirements: Fix webapp Jest suites broken by Babel 8 (ESM-only presets)

## Added

- **NF-FWT-001** Running `npx tsc` in `webapp/` under TypeScript 7 (the config sets `noEmit`) reports no error against `webapp/tsconfig.json` itself: `compilerOptions.target` is `ES2022` and `compilerOptions.moduleResolution` is `bundler`, and every other compiler option keeps its value. Type errors reported against source files are out of scope here, and no npm script or CI step runs `tsc`.
  - **Tags**: `build`
  - **Falsifier**: deleting `target` and `moduleResolution` also silences both config errors but changes the effective resolution; ruled out by test row `T-tsconfig-options`.

## Modified (overrides baseline)

- **NF-107** `npm test` runs Jest in a jsdom environment, transforming sources with the project's own `babel-jest` and `@babel/core` 8 through the presets in `webapp/.babelrc`, mapping CSS imports to `identity-obj-proxy` and replacing `react-force-graph-2d` with a stub; `webapp/package-lock.json` resolves `react` and `react-dom` to the same version, `19.3.1`, and all 8 suites (34 cases) run and pass on Node 24 (24.11 or later, Babel 8's engine floor) and on Node 26.
  - **Tags**: `test`
  - **Falsifier**: aligning only `react-dom` leaves Jest on its nested Babel 7 core, so the suite importing a TypeScript enum still fails; ruled out by test rows `T-jest-transform-path` and `T-jest-all-suites-local`.

- **NF-108** The `Webapp CI` workflow runs on pushes and pull requests to `main` that touch `webapp/**` and executes `npm ci`, `npm test` and `npm run build` on Node 24.
  - **Tags**: `ci`, `guide:build-and-deploy`
  - **Falsifier**: setting `node-version` to `22` or `26` also satisfies Babel 8's engine range and turns CI green; ruled out by test row `T-ci-node-24`.

## Assumes (read-only baseline dependencies)

- `NF-103` webpack build transpiles with Babel and no type check — relied upon as stated.
- `NF-104` Docker image builds on `node:26-alpine` — relied upon as stated; this delta does not touch `webapp/Dockerfile`.
