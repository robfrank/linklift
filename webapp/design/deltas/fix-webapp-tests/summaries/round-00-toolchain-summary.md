# Round Summary: Toolchain

### What Changed
- `webapp/package-lock.json`: `node_modules/react-dom` 19.3.0 -> 19.3.1 (version, resolved, integrity, peerDependencies only), via `npm update react-dom --package-lock-only --ignore-scripts`; package.json unchanged.
- `webapp/jest.config.js`: transform uses `require.resolve("babel-jest")`; `jest --showConfig` shows `webapp/node_modules/babel-jest/build/index.js`.
- `webapp/tsconfig.json`: `target` ES2022, `moduleResolution` bundler.
- `.github/workflows/webapp-ci.yml`: Node 24.
- Local (Node 26.13): `npm ci`, `npm run test:coverage` 8 suites / 34 tests pass, `npm run build` produces dist/bundle.js and dist/index.html, prettier check green. No tests added (none in scope).

### Requirement Checklist Updates
| Requirement ID | Validating Tests | Notes |
|----------------|------------------|-------|
| `NF-107` | `T-lock-react-versions`, `T-jest-transform-path`, `T-jest-all-suites-local`, `T-ci-green` | Local checks pass on Node 26; Node 24 run and CI pending on the PR |
| `NF-108` | `T-ci-node-24`, `T-ci-green` | node-version 24 set |
| `NF-FWT-001` | `T-tsconfig-options` | Options set; `tsc` owner-run |

### Risks, Deviations, Follow-up
- The source type errors that `npx tsc` still reports belong to a separate delta (**NF-FWT-001**); the type-check gate stays owner-run until it lands.
