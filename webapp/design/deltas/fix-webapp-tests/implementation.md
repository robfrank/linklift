# Delta Implementation: Fix webapp Jest suites broken by Babel 8 (ESM-only presets)

## Module & File Layout

| File | Responsibility | Key Functions | Entry Points |
|------|----------------|---------------|--------------|
| `webapp/package-lock.json` | Lock `react-dom` at the version `react` already resolves to (**NF-107**); `webapp/package.json` ranges stay as they are | — | `npm ci` |
| `.github/workflows/webapp-ci.yml` | `actions/setup-node` `node-version` for the `Webapp CI` job (**NF-108**) | — | `Webapp CI` workflow |

No Jest, Babel or webpack configuration file changes: `webapp/jest.config.js` and `webapp/.babelrc` stay as they are (**NF-107**, **NF-103**). No test file changes.

## Implementation Rounds

| Round ID  | File | Summary | Goal | Requirement IDs | Depends on |
|-----------|------|---------|------|-----------------|------------|
| `ROUND-01` | — | — | Two-file change; no round document is needed | `NF-107`, `NF-108` | none |

## Testing Plan

Behavioral, per the Developer Rule's testing-plan convention. This table is the ONLY place the delta's tests are stated.

| Test | Requirement IDs | Case | Assertion |
|------|-----------------|------|-----------|
| `T-lock-react-versions` | `NF-107` | Read `packages["node_modules/react"].version` and `packages["node_modules/react-dom"].version` from `webapp/package-lock.json` | Both equal the version **NF-107** names; no nested `node_modules/*/node_modules/react-dom` entry exists |
| `T-jest-all-suites-local` | `NF-107` | `cd webapp && npm ci && npm test` on Node 26 | Jest reports the suite and case counts **NF-107** names, all passed, none failed |
| `T-ci-node-24` | `NF-108` | Read the `node-version` input of `actions/setup-node` in `.github/workflows/webapp-ci.yml` | It is the Node major **NF-108** names |
| `T-ci-green` | `NF-107`, `NF-108` | The `Webapp CI` run on the delta's pull request | `Install dependencies`, `Run tests` and `Build project` steps all succeed, and the `Run tests` log shows the counts **NF-107** names |
| `T-build-output` | `NF-103` | `cd webapp && npm run build` | `dist/bundle.js` and `dist/index.html` are produced (bundle content changes only by the `react-dom` alignment) |

Delta-specific acceptance: `T-ci-green` on the pull request is the proof of done; deploy is unchanged (see [operational-guides/build-and-deploy.md](../../operational-guides/build-and-deploy.md)).
