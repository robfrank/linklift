# Baseline Implementation

How the webapp described in [requirements.md](requirements.md) and [specifications.md](specifications.md) is laid out, built and tested today.

## Module & File Layout {#layout}

| Path                                                                            | Contents                                                                                                                                                | Spec section                                       |
| ------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------- |
| `webapp/src/index.tsx`                                                          | entry point, mounts `App` in a `BrowserRouter`                                                                                                          | [routing](specifications.md#routing)               |
| `webapp/src/App.js`                                                             | provider stack, theme, route table                                                                                                                      | [component-tree](specifications.md#component-tree) |
| `webapp/src/domain/models/`                                                     | TypeScript types: `Link`, `Page`, `Content`, `Collection`, `Tag`, `QA`, `GraphData`                                                                     | [api-client](specifications.md#api-client)         |
| `webapp/src/domain/ports/`                                                      | repository interfaces (`ILinkRepository`, `IContentRepository`, `ICollectionRepository`, `ITagRepository`)                                              | [api-client](specifications.md#api-client)         |
| `webapp/src/domain/usecases/`                                                   | one class per use case, each delegating to a repository                                                                                                 | [api-client](specifications.md#api-client)         |
| `webapp/src/application/services/`                                              | axios repository adapters                                                                                                                               | [api-client](specifications.md#api-client)         |
| `webapp/src/application/state/`                                                 | Zustand store and slices                                                                                                                                | [state-store](specifications.md#state-store)       |
| `webapp/src/application/di-container.ts`                                        | singleton wiring repositories to use cases                                                                                                              | [api-client](specifications.md#api-client)         |
| `webapp/src/infrastructure/api/axios-instance.ts`                               | the shared HTTP client                                                                                                                                  | [api-client](specifications.md#api-client)         |
| `webapp/src/infrastructure/ui/pages/`                                           | Home, AddLink, Search, Graph, Ask, Admin pages                                                                                                          | [component-tree](specifications.md#component-tree) |
| `webapp/src/infrastructure/ui/components/`                                      | LinkList, AddLink, AddToCollectionDialog, ContentList, ContentViewerModal, Navigation, Tags                                                             | [component-tree](specifications.md#component-tree) |
| `webapp/src/infrastructure/ui/hooks/`                                           | store-selector hooks                                                                                                                                    | [state-store](specifications.md#state-store)       |
| `webapp/src/infrastructure/ui/layouts/MainLayout.tsx`                           | top bar + drawer layout                                                                                                                                 | [component-tree](specifications.md#component-tree) |
| `webapp/src/components/`                                                        | older JavaScript components still routed (Login, Register, NotFound, ProtectedRoute, ErrorBoundary, Collections) or reused (ContentViewer parts, Graph) | [component-tree](specifications.md#component-tree) |
| `webapp/src/contexts/`                                                          | `AuthContext`, `SnackbarContext`                                                                                                                        | [auth-flow](specifications.md#auth-flow)           |
| `webapp/src/hooks/useGraph.js`, `webapp/src/services/GraphService.js`           | graph data fetching outside the store                                                                                                                   | [component-tree](specifications.md#component-tree) |
| `webapp/src/utils/formatters.js`                                                | byte and relative-date formatting                                                                                                                       | [content-viewer](specifications.md#content-viewer) |
| `webapp/public/index.html`                                                      | HTML template                                                                                                                                           | [theming](specifications.md#theming)               |
| `webapp/webpack.config.js`, `webapp/.babelrc`, `webapp/tsconfig.json`           | build                                                                                                                                                   | [build-config](specifications.md#build-config)     |
| `webapp/jest.config.js`, `webapp/src/setupTests.js`, `webapp/src/test-utils.js` | test harness                                                                                                                                            | [Testing](#testing)                                |
| `webapp/Dockerfile`, `webapp/nginx.conf`, `webapp/config/deploy.yml`            | image and deployment                                                                                                                                    | [build-config](specifications.md#build-config)     |

### Code present but not reachable from the running app {#unreachable-code}

These files are compiled or tested but no route or rendered component uses them. They are recorded so a delta does not mistake them for live behaviour.

- `webapp/src/components/Header.js` — an older top bar with navigation buttons, a `/search?q=` search box and a user menu with sign-out. `App.js` does not render it; only `webapp/src/components/__tests__/Header.test.js` does.
- `webapp/src/components/ContentViewer/ContentViewerModal.js` — the older JavaScript modal. It imports `../../hooks/useContent`, a path that does not exist (`webapp/src/hooks/` holds only `useGraph.js`), so it would fail to resolve if imported. Nothing imports it.
- `webapp/src/components/Skeletons/LinkListSkeleton.js` — only its test renders it.
- `webapp/src/services/__mocks__/api.js` — a Jest manual mock for a `services/api` module that no longer exists.
- `webapp/src/types/content.js` — JSDoc types plus the `DownloadStatus` constant used by `DownloadingState.js`.
- Store actions and adapter methods with no caller in the UI: `deleteContent`, `fetchSuggestedTags`, `deleteTag`, and `ApiCollectionRepository.create` / `delete` / `removeLink`.
- `webapp/test-api.js` — a manual Node script that calls `http://localhost:8080/api/v1`; not part of the build or tests.

## Commands {#commands}

Run from `webapp/` (scripts from `webapp/package.json`):

| Command                 | Effect                                                             | Requirement |
| ----------------------- | ------------------------------------------------------------------ | ----------- |
| `npm ci`                | install exact dependencies from `package-lock.json`                | —           |
| `npm start`             | dev server with browser open                                       | **NF-102**  |
| `npm run build`         | production bundle in `dist/`                                       | **NF-103**  |
| `npm test`              | Jest, all tests                                                    | **NF-107**  |
| `npm run test:watch`    | Jest watch mode                                                    | **NF-107**  |
| `npm run test:coverage` | Jest with coverage (excludes `src/index.tsx`, `src/setupTests.js`) | **NF-107**  |
| `npm run docker-build`  | `docker build -t robfrank/linklift-webapp .`                       | **NF-104**  |

The [dev-server guide](operational-guides/dev-server.md) and [build-and-deploy guide](operational-guides/build-and-deploy.md) hold the procedures.

## Testing Plan {#testing}

### Strategy as built

Tests are Jest + React Testing Library component tests in jsdom (**NF-107**). External calls are isolated with `jest.mock` on the axios instance module (`webapp/src/infrastructure/api/axios-instance.ts`) and by replacing `useAuth` with a mock; MSW is a declared dev dependency but no test imports it.

<!-- verify: rg -l "from \"msw|require\(\"msw" webapp/src | wc -l == 0 -->

`webapp/src/test-utils.js` provides `renderWithTheme`, `renderWithRouter` and `renderApp` (MUI theme + snackbar provider, optionally a `MemoryRouter`). `webapp/src/setupTests.js` loads `@testing-library/jest-dom`, polyfills `TextEncoder`, mocks `window.matchMedia`, and filters some React, React Router and MUI console warnings.

There are no tests for the store slices, use cases, repositories, `AuthContext`, the Home/Search/Ask/Admin/Graph pages, `LinkList`, tags, or the content modal container. CI runs the suite and the build on Node 18 (**NF-108**).

### Existing test files

The suite has 8 test files holding 34 `it`/`test` cases.

<!-- verify: find webapp/src -path '*__tests__*' -type f | wc -l == 8 -->
<!-- verify: rg -c "^\s*(it|test)\(" webapp/src --glob '**/__tests__/**' | awk -F: '{s+=$2} END {print s}' == 34 -->

| Test file                                                              | Covers                                                                                               | Requirements                                   |
| ---------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- | ---------------------------------------------- |
| `webapp/src/__tests__/App.test.js`                                     | top bar title, `/` shows `My Links`, `/add` form, unknown route shows 404 (auth mocked as signed in) | **RQ-503**, **RQ-504**, **RQ-543**, **RQ-561** |
| `webapp/src/components/__tests__/NotFound.test.js`                     | 404 page text and `Return to Home` navigation                                                        | **RQ-503**                                     |
| `webapp/src/components/Collections/__tests__/CollectionList.test.js`   | loading indicator, list, empty state, create dialog opens                                            | **RQ-580**, **RQ-581**                         |
| `webapp/src/components/ContentViewer/__tests__/ContentDisplay.test.js` | text view, HTML toggle, sandboxed iframe, missing text                                               | **RQ-602**                                     |
| `webapp/src/components/ContentViewer/__tests__/formatters.test.js`     | `formatBytes`, `formatDate`                                                                          | **RQ-604**                                     |
| `webapp/src/contexts/__tests__/SnackbarContext.test.js`                | snackbar shows a filled alert                                                                        | **RQ-640**                                     |
| `webapp/src/components/__tests__/Header.test.js`                       | the unrouted `Header` component ([unreachable-code](#unreachable-code))                              | —                                              |
| `webapp/src/components/__tests__/LinkListSkeleton.test.js`             | the unused skeleton ([unreachable-code](#unreachable-code))                                          | —                                              |

These tests were not executed while writing this baseline (no `node_modules` in the worktree); the [checklist](requirement-checklist.md) reports test status accordingly.

## Known gaps {#known-gaps}

Mismatches between the webapp and the backend it calls, recorded as built. The backend facts are owned by the backend design root and cited by its ids.

- **Search response shape.** `ApiContentRepository.search` returns the response body as `ContentResponse[]`, and `HomePage.tsx` (highlight ids), `ContentList.tsx` and `SearchPage.tsx` read `result.data.<field>`. The backend's `GET /api/v1/search` answers a bare JSON array of content records (`SearchContentController.search` calls `ctx.json(results)`), so for any non-empty answer `result.data` is undefined, rendering throws, and the error panel replaces the app (**RQ-591**–**RQ-593**, **RQ-508**). Only an empty result list renders correctly.
- **Embedding backfill is always refused.** `POST /api/v1/admin/backfill-embeddings` requires `ADMIN_ACCESS`, which no user holds (backend root **RQ-075**, **RQ-025**); the admin page therefore always shows its failure alert, and the 403 is not handled by the 401-only interceptor (**RQ-630**, **RQ-526**).
- **Duplicate collection membership.** The add-to-collection dialog maps 409 to a duplicate message, but the backend answers 204 without a duplicate check (backend root **RQ-103**), so that branch is unreachable (**RQ-585**).

## Relationship to legacy documents {#legacy-documents}

Code wins over every document below. Status values: _still accurate_, _historical plan_ (describes intent at the time, partly realised), _superseded_ (replaced by this baseline), _conflicts with code_.

| Document                                                                                             | Status                                                 | Concrete conflicts with code                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| ---------------------------------------------------------------------------------------------------- | ------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `FRONTEND_ARCHITECTURE_REFACTOR_PLAN.md`                                                             | historical plan, partly realised                       | Proposes a `views/` folder under `infrastructure/ui` (code: `webapp/src/infrastructure/ui/pages/`), a `contexts/` folder under `infrastructure/ui` (code: `webapp/src/contexts/`), a `LocalStorageAdapter` in an `infrastructure/storage` folder (absent; `AuthContext.js` uses `localStorage` directly) and an `infrastructure/__tests__` folder (absent). Says DI "avoids singletons" (code: `DIContainer.getInstance()` singleton). Says use cases update the store (code: slices call use cases, use cases only delegate). Plans domain unit tests and repository tests against MSW (none exist).                                                                                                                                                                                                      |
| `FRONTEND_REFACTOR_IMPLEMENTATION_PLAN.md`                                                           | historical plan; every box ticked, but not all true    | 1.1 offers `ts-loader` (code: `babel-loader` only). 3.1 says `getAll(): Promise<Link[]>` (code: `getAll(request: PageRequest): Promise<Page<Link>>`). 4.3 "remove unused React Contexts" and "all new files in the proper structure": collections pages in `src/components/Collections/` call axios directly, `tagSlice.ts` and `qaSlice.ts` bypass the DI container, and graph code lives in `src/hooks/` and `src/services/`.                                                                                                                                                                                                                                                                                                                                                                            |
| `webapp/README.md`                                                                                   | conflicts with code                                    | Says Material UI v7 (`package.json`: `@mui/material` `^9.0.0`). Says Node v18+ (Docker build: `node:26-alpine`; CI: Node 18). Lists `Home` as a landing page and `Link List` as a separate page (code: `/` is the link list). Lists only `/api/v1/link` (code: endpoints in [api-client](specifications.md#api-client)). Omits collections, search, graph, ask and admin pages. Claims API service tests and integration tests for user flows (none exist). Install/start/build/test commands are still accurate.                                                                                                                                                                                                                                                                                          |
| `webapp/CLAUDE.md` <!-- external-ref --> (untracked file in the main checkout; not in this worktree) | conflicts with code                                    | Dev server "on localhost:8080" (code: port 3000, **NF-102**). MUI 7 (code: `^9.0.0`). TypeScript 5.9 (code: `typescript` `^7.0.2`). Jest + RTL + "MSW 2", "mock API responses at the network level via service workers" (code: `msw` `^3.0.3` declared, never imported; tests use `jest.mock`). "No raw CSS files" (code: `src/index.css`, `ContentViewerModal.css`). Slice list omits `tagSlice` and `qaSlice`. "Import from [the DI container], not directly from service files" (code: `tagSlice.ts` imports `ApiTagRepository`). Says the axios instance "handles … auth headers … and error interceptors" (code: the instance sets only base URL and content type; the header and 401 interceptor live in `AuthContext.js`). Folder map, Zustand-slice pattern and DOMPurify rule are still accurate. |
| Root `CLAUDE.md` <!-- external-ref --> (frontend parts; untracked, main checkout)                    | conflicts with code                                    | Tech-stack row "React 19 + TypeScript + MUI 7 + Zustand" (code: MUI 9). Its endpoint table lists `POST /api/v1/user/register`, `POST /api/v1/user/authenticate`, `GET /api/v1/content/:id`, `POST /api/v1/search`, `GET /api/v1/related/:id`, `GET /api/v1/link/:id`, `DELETE /api/v1/link/:id`; the webapp calls `/auth/register`, `/auth/login`, `GET /links/:id/content`, `GET /search`, `GET /links/:id/related` and `DELETE /links/:id` instead (whether the backend serves both forms is for the backend root to state).                                                                                                                                                                                                                                                                             |
| `conductor/code_styleguides/typescript.md`                                                           | conflicts with code                                    | Requires named exports only (pages such as `HomePage.tsx` and `AddLinkPage.tsx` use `export default`), single quotes (code and `.prettierrc.js` use double quotes), no `public` modifier and no `_` prefixes (`di-container.ts` has `public static getInstance` and `_instance`, `_linkRepository`), avoiding `any` (slices use `catch (error: any)`), and says it is enforced by `gts` (not a dependency).                                                                                                                                                                                                                                                                                                                                                                                                |
| `conductor/code_styleguides/javascript.md`                                                           | conflicts with code                                    | Requires lower-case dashed file names (code: `Login.js`, `CollectionList.js`), mandatory `.js` extensions in imports (code: extensionless), named exports only (code: default exports), single quotes, an 80-column limit (`.prettierrc.js` sets `printWidth: 160`), and JSDoc on all members (mostly absent).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| `conductor/code_styleguides/html-css.md`                                                             | conflicts with code (minor; HTML rules still accurate) | `public/index.html` follows the HTML rules checked (`<!doctype html>`, UTF-8 meta, HTTPS font link). `ContentViewerModal.css` does not alphabetise declarations (its first rule, `.modal-overlay`, starts `position`, `top`, `left`). The JS-in-CSS styling via MUI `sx` is outside this guide's scope.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `conductor/tracks/collections_20260206/spec.md` (frontend parts)                                     | historical plan, mostly unrealised in the UI           | Plans rename, bulk add/remove, merge, AI summary, drag-and-drop and a user-ordered sidebar (none in the webapp: the drawer lists collections in response order, **RQ-586**). Plans "a filtered view of links belonging to a selected collection" (the drawer's `?collection=` link does not filter, **RQ-506**; the separate `/collections/:id` page lists members, **RQ-583**). Plans collection nodes in the graph (not in `GraphView`). Its "Zustand store for … the current selection" exists only as a collection list; selection is not stored.                                                                                                                                                                                                                                                      |

## Fix webapp Jest suites broken by Babel 8 (ESM-only presets) (`fix-webapp-tests`)

## Delta Implementation: Fix webapp Jest suites broken by Babel 8 (ESM-only presets)

### Resolved Document Set

<!-- GENERATED by scripts/gen-docs.py from the manifest and this delta's files; the one Resolved Document Set of a pinned delta. Do not edit. Regenerate with: python3 scripts/gen-docs.py <design-root> <slug> -->

- Baseline: `webapp/design/requirements.md`, `webapp/design/specifications.md`, `webapp/design/implementation.md`
- Delta Requirements: `webapp/design/archive/fix-webapp-tests/requirements.md`
- Delta Requirement Checklist: `webapp/design/archive/fix-webapp-tests/requirement-checklist.md`
- Rounds:
  - `ROUND-00`: `webapp/design/archive/fix-webapp-tests/phases/round-00-toolchain.md`
- Footprint (from the manifest):
  - assumes: `NF-103`, `NF-104`
  - changes: `NF-107` (modify), `NF-108` (modify), `NF-FWT-001` (add)
- Development Rules: `typescript-developer`
- Project Standards: CLAUDE.md, webapp/CLAUDE.md

### Module & File Layout

| File | Responsibility | Key Functions | Entry Points |
|------|----------------|---------------|--------------|
| `webapp/jest.config.js` | The `transform` entry names `require.resolve("babel-jest")`, so Jest loads the project's top-level `babel-jest` and with it the top-level `@babel/core` 8 instead of the copy nested under `jest-config` (**NF-107**) | — | `npm test`, `npm run test:coverage` |
| `webapp/package-lock.json` | `node_modules/react-dom` aligned with `react` (**NF-107**) by `npm update react-dom --package-lock-only --ignore-scripts` from `webapp/`; `webapp/package.json` is not edited, its `^19.1.0` range already allows it | — | `npm ci` |
| `webapp/tsconfig.json` | The two compiler options **NF-FWT-001** names | — | `npx tsc` |
| `.github/workflows/webapp-ci.yml` | `actions/setup-node` `node-version` for the `Webapp CI` job (**NF-108**) | — | `Webapp CI` workflow |

No test file, `webapp/.babelrc`, `webapp/webpack.config.js`, `webapp/Dockerfile` or source file changes.

### Implementation Rounds

| Round ID  | File | Summary | Goal | Requirement IDs | Depends on |
|-----------|------|---------|------|-----------------|------------|
| `ROUND-00` | [phases/round-00-toolchain.md](phases/round-00-toolchain.md) | [summaries/round-00-toolchain-summary.md](summaries/round-00-toolchain-summary.md) | Four-file toolchain change; the whole delta is this round | `NF-107`, `NF-108`, `NF-FWT-001` | none |

### Testing Plan

Behavioral, per the Developer Rule's testing-plan convention. This table is the ONLY place the delta's tests are stated.

| Test | Requirement IDs | Case | Assertion |
|------|-----------------|------|-----------|
| `T-lock-react-versions` | `NF-107` | Read `packages["node_modules/react"].version` and `packages["node_modules/react-dom"].version` from `webapp/package-lock.json` | Both equal the version **NF-107** names; no nested `node_modules/*/node_modules/react-dom` entry exists |
| `T-jest-transform-path` | `NF-107` | `cd webapp && npx jest --showConfig` | The `transform` path is `webapp/node_modules/babel-jest/build/index.js`, not one under `node_modules/jest-config/` |
| `T-jest-all-suites-local` | `NF-107` | `cd webapp && npm ci && npx jest --clearCache && npm test` on Node 26, then the same Jest run under the Node 24 **NF-107** names | Each run reports the suite and case counts **NF-107** names, all passed, none failed |
| `T-tsconfig-options` | `NF-FWT-001` | Read `webapp/tsconfig.json`, then `cd webapp && npx tsc` | The two options hold the values **NF-FWT-001** names, every other option keeps its trunk value, and the `tsc` output has no `TS5107` or `TS5108` (config-option) diagnostic |
| `T-ci-node-24` | `NF-108` | Read the `node-version` input of `actions/setup-node` in `.github/workflows/webapp-ci.yml` | It is the Node major **NF-108** names |
| `T-ci-green` | `NF-107`, `NF-108` | The `Webapp CI` run on the delta's pull request | `Install dependencies`, `Run tests` and `Build project` steps all succeed, and the `Run tests` log shows the counts **NF-107** names |
| `T-build-output` | `NF-103` | `cd webapp && npm run build` | `dist/bundle.js` and `dist/index.html` are produced; `git diff` of `webapp/package-lock.json` against the baseline commit changes only the `version`, `resolved`, `integrity` and `peerDependencies` lines of the `node_modules/react-dom` entry |

Delta-specific acceptance: `T-ci-green` on the pull request is the proof of done; deploy follows the existing procedure (see [operational-guides/build-and-deploy.md](../../operational-guides/build-and-deploy.md)).
