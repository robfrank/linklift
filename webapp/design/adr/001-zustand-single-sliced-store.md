# Zustand single store composed of slices

## Status

Accepted (retroactively recorded for the baseline)

## Context

The webapp needs shared client state for the link list, content viewer, collections, search, tags and the Ask conversation across pages and the persistent layout. `FRONTEND_ARCHITECTURE_REFACTOR_PLAN.md` proposed a framework-agnostic store and named Zustand as a candidate.

## Decision

One Zustand store, `useAppStore` in `webapp/src/application/state/store.ts`, is built by spreading per-concern slice creators. Slices own their async actions, call use cases or adapters, and store user-facing error strings. Components read the store through small hooks in `webapp/src/infrastructure/ui/hooks/`. Session state stays in a React context (`AuthContext`), and some pages (collections, graph) keep local component state. See [state-store](../specifications.md#state-store).

## Consequences

- Pages and the persistent drawer share list, sort and collection state without prop drilling (**RQ-542**, **RQ-586**).
- The store is not persisted; state lasts for one page load (**RQ-621**).
- State is split across three mechanisms (store, auth context, component state), so the drawer's collection list and `/collections` can disagree until the store reloads collections.
- Slice actions have no unit tests.
