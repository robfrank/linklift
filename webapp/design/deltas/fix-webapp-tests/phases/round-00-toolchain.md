# Round: Toolchain

### Round Scope
- Requirement IDs: the rounds-table row for `ROUND-00` in `implementation.md`
- Deliverables: the four configuration files in `implementation.md`'s Module & File Layout, which are this delta's whole change
- Depends on: none

### Round File Layout
| File | Responsibility | Key Functions | Entry Points |
|------|----------------|---------------|--------------|
| `webapp/jest.config.js` | see `implementation.md` Module & File Layout | — | `npm test` |
| `webapp/package-lock.json` | see `implementation.md` Module & File Layout | — | `npm ci` |
| `webapp/tsconfig.json` | see `implementation.md` Module & File Layout | — | `npx tsc` |
| `.github/workflows/webapp-ci.yml` | see `implementation.md` Module & File Layout | — | `Webapp CI` |

### Round Data Changes
None.

### Gates

Every gate of `typescript-developer` runs the command `webapp/design/rule-overrides.yaml` approves for it, from `webapp/`.

| Gate | Command | Expectation |
|------|---------|-------------|
| dep-sync | `npm ci` | override: use |
| lint | `npx --yes prettier@3.3.3 --check --no-config --print-width 160 --trailing-comma none src` | override: use |
| format-check | `npx --yes prettier@3.3.3 --check --no-config --print-width 160 --trailing-comma none src` | override: use |
| type-check | `bunx tsc --noEmit` | override: owner |
| test-baseline | `npm test` | override: use |
| test | `npm run test:coverage` | override: use |

### Round Tests
`T-lock-react-versions`, `T-jest-transform-path`, `T-jest-all-suites-local`, `T-tsconfig-options`, `T-ci-node-24`, `T-build-output`, then `T-ci-green` on the pull request (rows in `implementation.md`'s Testing Plan).
