# Delta Requirements: Fix webapp Jest suites broken by Babel 8 (ESM-only presets)

## Modified (overrides baseline)

- **NF-107** `npm test` runs Jest in a jsdom environment, transforming sources with babel-jest through the Babel 8 presets in `webapp/.babelrc`, mapping CSS imports to `identity-obj-proxy` and replacing `react-force-graph-2d` with a stub; `webapp/package-lock.json` resolves `react` and `react-dom` to the same version, `19.3.1`, and all 8 suites (34 cases) run and pass on Node 24 and on Node 26.
  - **Tags**: `test`
  - **Falsifier**: lowering `react` to `19.3.0` (the rejected alternative) also turns every suite green; ruled out by test row `T-lock-react-versions`.

- **NF-108** The `Webapp CI` workflow runs on pushes and pull requests to `main` that touch `webapp/**` and executes `npm ci`, `npm test` and `npm run build` on Node 24.
  - **Tags**: `ci`, `guide:build-and-deploy`
  - **Falsifier**: setting `node-version` to `22` or `26` also satisfies Babel 8's engine range and turns CI green; ruled out by test row `T-ci-node-24`.

## Assumes (read-only baseline dependencies)

- `NF-103` webpack build transpiles with Babel and no type check — relied upon unchanged; `npm run build` still produces `dist/bundle.js` plus `dist/index.html`.
- `NF-104` Docker image builds on `node:26-alpine` — relied upon unchanged; this delta does not touch `webapp/Dockerfile`.
