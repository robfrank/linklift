# Jest and React Testing Library with module mocks

## Status

Accepted (retroactively recorded for the baseline)

## Context

Components call the backend through one axios instance and read auth state from a context. Tests need to run without a backend.

## Decision

Tests run on Jest in jsdom with React Testing Library (`webapp/jest.config.js`, `webapp/src/setupTests.js`). They isolate the network with `jest.mock` on `webapp/src/infrastructure/api/axios-instance.ts` and replace `useAuth` with a Jest mock; `react-force-graph-2d` is mapped to a stub. MSW is declared in `webapp/package.json` but not used. See [Testing](../implementation.md#testing).

## Consequences

- Tests need no network (**NF-107**).
- Mocks replace whole modules, so no test asserts request paths, bodies or query parameters.
- Documents that describe MSW-based tests do not match the code ([legacy documents](../implementation.md#legacy-documents)).
