# Baseline Specifications

Mechanism of the LinkLift webapp as built. Behaviour and literal values live in [requirements.md](requirements.md); this document cites them by id.

## System context {#system-context}

The webapp is a single-page React 19 application (`webapp/src/index.tsx` mounts `App` inside a `BrowserRouter`). It talks to one backend, the LinkLift Java API, only through relative `/api/v1` URLs (**NF-101**). In development webpack-dev-server forwards those to the API process (**NF-102**); in the Docker image nginx forwards them to the `linklift` host (**NF-104**). The backend's behaviour is owned by the backend design root (`design/`); this document only names the endpoints the UI calls.

No boundary-level decisions are introduced by this baseline beyond those recorded in [adr/](adr/).

## Component tree {#component-tree}

`webapp/src/App.js` nests, outermost first: `ErrorBoundary` (`webapp/src/components/ErrorBoundary.js`, **RQ-508**) → `AuthProvider` (`webapp/src/contexts/AuthContext.js`) → MUI `ThemeProvider` with the theme built in `App.js` (**RQ-641**) → `SnackbarProvider` (`webapp/src/contexts/SnackbarContext.js`, **RQ-640**) → `CssBaseline` → `<main>` → `Routes`.

Protected pages render through a layout route whose element is `ProtectedRoute` (`webapp/src/components/ProtectedRoute.js`) wrapping `MainLayout` (`webapp/src/infrastructure/ui/layouts/MainLayout.tsx`) wrapping an `Outlet` (**RQ-502**, **RQ-504**). `MainLayout` composes `TopBar` and `Sidebar` from `webapp/src/infrastructure/ui/components/Navigation/` (**RQ-504**, **RQ-505**, **RQ-507**, **RQ-542**, **RQ-586**, **RQ-590**). `MainLayout` keeps a `mobileOpen` flag toggled by the top-bar menu button, but `Sidebar` is a permanent `Drawer` that never reads it (**RQ-507**).

Pages and the components they own:

| Route              | Page module                                             | Main children                                                                         | Requirements                                                                      |
| ------------------ | ------------------------------------------------------- | ------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------- |
| `/`                | `webapp/src/infrastructure/ui/pages/HomePage.tsx`       | `LinkList`, `ContentList`, `GraphView`, `ContentViewerModal`, `AddToCollectionDialog` | **RQ-540**–**RQ-550**, **RQ-560**, **RQ-565**, **RQ-591**, **RQ-592**, **RQ-610** |
| `/add`             | `webapp/src/infrastructure/ui/pages/AddLinkPage.tsx`    | `AddLink` (`webapp/src/infrastructure/ui/components/AddLink.tsx`)                     | **RQ-561**–**RQ-564**                                                             |
| `/collections`     | `webapp/src/components/Collections/CollectionList.js`   | MUI dialog                                                                            | **RQ-580**–**RQ-582**                                                             |
| `/collections/:id` | `webapp/src/components/Collections/CollectionDetail.js` | `ContentViewerModal`                                                                  | **RQ-583**, **RQ-584**                                                            |
| `/search`          | `webapp/src/infrastructure/ui/pages/SearchPage.tsx`     | `ContentViewerModal`                                                                  | **RQ-593**                                                                        |
| `/graph`           | `webapp/src/infrastructure/ui/pages/GraphPage.js`       | `GraphView`                                                                           | **RQ-611**                                                                        |
| `/ask`             | `webapp/src/infrastructure/ui/pages/AskPage.tsx`        | `ConversationEntryCard` (same file)                                                   | **RQ-620**–**RQ-622**                                                             |
| `/admin`           | `webapp/src/infrastructure/ui/pages/AdminPage.tsx`      | —                                                                                     | **RQ-630**                                                                        |
| `/login`           | `webapp/src/components/Login.js`                        | —                                                                                     | **RQ-520**–**RQ-522**                                                             |
| `/register`        | `webapp/src/components/Register.js`                     | —                                                                                     | **RQ-523**, **RQ-524**                                                            |
| `*`                | `webapp/src/components/NotFound.js`                     | —                                                                                     | **RQ-503**                                                                        |

`LinkList` (`webapp/src/infrastructure/ui/components/LinkList.tsx`) is presentational: `HomePage` passes it the store's list state and the `deleteLink`, `updateLinkStatus` and `setPage` actions (**RQ-543**–**RQ-550**, **RQ-565**). Each card embeds `LinkTagManager` in compact mode (`webapp/src/infrastructure/ui/components/Tags/LinkTagManager.tsx`, using `TagChips.tsx`) (**RQ-570**–**RQ-572**). `LinkList` also owns its own `ContentViewerModal` for the card's view button (**RQ-600**); `HomePage` owns a second one for search results (**RQ-591**).

`GraphView` (`webapp/src/components/Graph/GraphView.tsx`) wraps `react-force-graph-2d`, sizes itself with a `ResizeObserver`, and computes node colours from an optional `highlightIds` list (**RQ-592**, **RQ-610**). Graph data comes from the `useGraph` hook (`webapp/src/hooks/useGraph.js`) over `webapp/src/services/GraphService.js`, held in React component state, not in the store (**RQ-610**, **RQ-611**).

## Routing {#routing}

Routing uses React Router 7 (`react-router-dom`) with a `BrowserRouter`, so paths are real URL paths and both the dev server and nginx fall back to `index.html` (**NF-102**, **NF-104**). Route definitions live only in `webapp/src/App.js` (**RQ-501**–**RQ-503**). Query parameters are read with `URLSearchParams(location.search)`: `view` and `collection` on `/` (**RQ-540**, **RQ-506**), `q` on `/search` (**RQ-593**). `ProtectedRoute` passes `state.from` to `/login`, but `Login.js` ignores it (**RQ-521**).

## Authentication flow {#auth-flow}

`AuthContext.js` is the only owner of session state: a React context holding `user` and `loading`, plus `login`, `register`, `logout` and `refreshToken` functions, all calling the shared axios instance (**RQ-520**–**RQ-527**). Tokens are written to and read from `localStorage` directly; there is no storage adapter (**NF-105**). The bearer header is set on `api.defaults.headers.common`, so every module using the shared instance sends it (**RQ-521**).

Session restore happens in a mount effect that copies the stored `user` into state without a server round trip and then clears `loading` (**RQ-525**, **RQ-502**).

Token refresh is an axios response interceptor registered by `AuthProvider` on mount and ejected on unmount. It marks the failed request `_retry`, awaits `refreshToken()`, copies the new default header onto the original request and replays it through the instance (**RQ-526**). `refreshToken()` calls `logout()` on any failure and throws a fresh `Error` (**RQ-527**). Because the interceptor wraps every request including `/auth/login`, a 401 sign-in passes through it before `login()` maps the error (**RQ-522**).

Endpoints called: `POST /api/v1/auth/login`, `POST /api/v1/auth/register`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`.

## Client store {#state-store}

`webapp/src/application/state/store.ts` builds one Zustand 5 store, `useAppStore`, by spreading six slice creators into a single state object:

<!-- verify: grep -c '^  \.\.\.create' webapp/src/application/state/store.ts == 6 -->

| Slice       | File                 | Holds                                                                 | Requirements                                              |
| ----------- | -------------------- | --------------------------------------------------------------------- | --------------------------------------------------------- |
| links       | `linkSlice.ts`       | list page, sort, page size, add/delete/status flags and error strings | **RQ-541**–**RQ-550**, **RQ-562**, **RQ-564**, **RQ-565** |
| content     | `contentSlice.ts`    | the one open content response and its loading/error state             | **RQ-600**–**RQ-603**                                     |
| collections | `collectionSlice.ts` | collection list for drawer and dialog, add-to-collection state        | **RQ-585**, **RQ-586**                                    |
| search      | `searchSlice.ts`     | search results, backfill state                                        | **RQ-590**–**RQ-593**, **RQ-630**                         |
| tags        | `tagSlice.ts`        | all tags, per-link tags keyed by link id, suggestions                 | **RQ-570**–**RQ-572**                                     |
| qa          | `qaSlice.ts`         | Ask conversation                                                      | **RQ-620**, **RQ-621**                                    |

All slice files are in `webapp/src/application/state/`. Components subscribe through hooks in `webapp/src/infrastructure/ui/hooks/` (`useLinks`, `useCollections` with `useShallow`; `useSearch`, `useContent` with per-field selectors), except `AskPage` and `LinkTagManager`, which call `useAppStore()` without a selector. The store is not persisted, so it lives for one page load (**RQ-621**).

The content slice holds a single `content` value shared by every open viewer (**RQ-600**).

The collections pages (`CollectionList.js`, `CollectionDetail.js`) do not use the store: they keep their own component state and call the axios instance directly (**RQ-580**–**RQ-584**). The drawer and the add-to-collection dialog use the store's collections slice instead (**RQ-585**, **RQ-586**), so creating or deleting a collection on `/collections` does not change the drawer until the store's `fetchCollections` runs again (when the drawer mounts after a full page load, or when the add-to-collection dialog opens).

## API client and endpoints {#api-client}

`webapp/src/infrastructure/api/axios-instance.ts` exports the single axios instance with the base URL and JSON content type (**NF-101**). Every request in the app goes through it.

Domain code follows a ports-and-adapters layout: interfaces in `webapp/src/domain/ports/`, use-case classes in `webapp/src/domain/usecases/`, and axios adapters in `webapp/src/application/services/`. `webapp/src/application/di-container.ts` is a lazily created singleton that builds one link, content and collection repository and returns a new use-case object per `resolve…` call; slices call `container.resolve…().execute(...)`. Two slices bypass it: `tagSlice.ts` instantiates `ApiTagRepository` at module load, and `qaSlice.ts` calls the axios instance directly.

Endpoints the UI calls, with the module that calls them:

| Method and path (under `/api/v1`)                                              | Caller                                                | Requirements                                  |
| ------------------------------------------------------------------------------ | ----------------------------------------------------- | --------------------------------------------- |
| `GET /links` (`page`, `size`, optional `sortBy`, `sortDirection`)              | `ApiLinkRepository.getAll`                            | **RQ-541**, **RQ-565**                        |
| `PUT /link`                                                                    | `ApiLinkRepository.create`                            | **RQ-562**                                    |
| `DELETE /links/:id`                                                            | `ApiLinkRepository.delete`                            | **RQ-565**                                    |
| `PATCH /links/:id/status`                                                      | `ApiLinkRepository.updateStatus`                      | **RQ-550**                                    |
| `GET /links/:id/content`, `DELETE /links/:id/content`                          | `ApiContentRepository`                                | **RQ-600**                                    |
| `POST /links/:id/content/refresh`                                              | `ApiContentRepository.refreshContent`                 | **RQ-603**                                    |
| `GET /search` (`q`, `limit`)                                                   | `ApiContentRepository.search`                         | **RQ-590**                                    |
| `POST /admin/backfill-embeddings`                                              | `ApiContentRepository.backfillEmbeddings`             | **RQ-630**                                    |
| `GET /collections`, `POST /collections`, `DELETE /collections/:id`             | `CollectionList.js`; `ApiCollectionRepository.getAll` | **RQ-580**–**RQ-582**, **RQ-585**, **RQ-586** |
| `GET /collections/:id`, `DELETE /collections/:id/links/:linkId`                | `CollectionDetail.js`                                 | **RQ-583**, **RQ-584**                        |
| `POST /collections/:id/links`                                                  | `ApiCollectionRepository.addLink`                     | **RQ-585**                                    |
| `GET /tags`, `POST /tags`                                                      | `ApiTagRepository`                                    | **RQ-571**, **RQ-572**                        |
| `GET /links/:id/tags`, `POST /links/:id/tags`, `DELETE /links/:id/tags/:tagId` | `ApiTagRepository`                                    | **RQ-570**, **RQ-572**                        |
| `GET /graph`, `GET /links/:id/related`                                         | `GraphService.js`                                     | **RQ-610**, **RQ-611**                        |
| `POST /ask`                                                                    | `qaSlice.ts`                                          | **RQ-620**                                    |
| `POST /auth/login`, `/auth/register`, `/auth/refresh`, `/auth/logout`          | `AuthContext.js`                                      | **RQ-520**–**RQ-527**                         |

Response envelopes the UI assumes: link list, status update, collections and graph responses are unwrapped from a top-level `data` field; content, search, tag and ask responses are used as returned. Search results are typed as `ContentResponse` (`{data: Content, message?}`, `webapp/src/domain/models/Content.ts`) and consumed as `result.data.<field>` by `HomePage.tsx`, `ContentList.tsx` and `SearchPage.tsx`, while the backend's search answer is a bare array of content records; see [Known gaps](implementation.md#known-gaps) (**RQ-591**–**RQ-593**). `DELETE /links/:id/content`, `ApiCollectionRepository.create`, `ApiCollectionRepository.delete`, `ApiCollectionRepository.removeLink` and `ApiTagRepository.deleteTag` / `suggestTags` (`GET /links/:id/tags/suggest`) exist in code but no rendered control reaches them.

## Content viewer {#content-viewer}

The live modal is `webapp/src/infrastructure/ui/components/ContentViewer/ContentViewerModal.tsx`. It is a hand-built overlay styled by `webapp/src/components/ContentViewer/ContentViewerModal.css` rather than an MUI `Dialog`, and reuses the JavaScript presentational parts in `webapp/src/components/ContentViewer/` (`LoadingSpinner`, `ErrorMessage`, `DownloadingState`, `EmptyState`, `ContentDisplay`, `ModalHeader`, `ModalFooter`) (**RQ-600**–**RQ-605**). Data comes from `useContent(linkId)`, which loads on mount and exposes `refetch` and `refreshContent` (**RQ-601**, **RQ-603**).

The store keeps errors as strings, but `ErrorMessage` expects an `Error`; with a string it falls through to its generic branch, which is why the modal shows a generic message (**RQ-601**). `ContentDisplay` sanitises HTML with DOMPurify using explicit `ALLOWED_TAGS` / `ALLOWED_ATTR` lists and renders it through `srcDoc` in a sandboxed iframe (**RQ-602**). `ModalFooter` reads `contentLength`, `mimeType` and `downloadedAt` and formats them with `webapp/src/utils/formatters.js` (**RQ-604**).

## Error, loading and notification handling {#error-handling}

There is no global HTTP error handler beyond the 401 interceptor (**RQ-526**). Each slice action catches its own errors and stores a user-facing string; `addLink`, `deleteLink`, `updateLinkStatus` and `addLinkToCollection` also re-throw (**RQ-550**, **RQ-564**, **RQ-565**, **RQ-585**). `LinkList` calls `onDelete` and `onUpdateStatus` without awaiting or catching, and no component reads `deleteLinkError` or `updateLinkStatusError`, so those failures are silent (**RQ-550**, **RQ-565**).

Notifications go through `useSnackbar().showSnackbar(message, severity)` from `SnackbarContext.js`, a single MUI `Snackbar` + `Alert` (**RQ-640**). Only `AddLinkPage` and `CollectionList` call it (**RQ-563**, **RQ-564**, **RQ-581**, **RQ-582**).

Loading indicators are MUI `CircularProgress` throughout, except the content modal's CSS spinner (**RQ-546**, **RQ-601**).

## Theming and styling {#theming}

Styling is MUI 9 with Emotion, the `sx` prop and `styled()` (top-bar search box); global CSS is `webapp/src/index.css` and the content modal uses its own stylesheet (**RQ-641**). The Roboto font link and document title are in `webapp/public/index.html` (**RQ-641**, **RQ-642**). The theme object is duplicated in `webapp/src/test-utils.js` for tests.

## Build configuration {#build-config}

`webapp/webpack.config.js` defines entry, output, `babel-loader` for `.js/.jsx/.ts/.tsx`, `style-loader` + `css-loader` for CSS, `HtmlWebpackPlugin` over `public/index.html`, and the dev-server settings (**NF-102**, **NF-103**). A rule disables `fullySpecified` for `.mjs`/`.js` so MUI 9's extensionless ESM imports resolve. `webapp/.babelrc` applies `@babel/preset-env`, `@babel/preset-react` (automatic runtime) and `@babel/preset-typescript` (**NF-103**). `webapp/tsconfig.json` sets `strict` and `noEmit`, but no npm script or CI step runs `tsc` (**NF-103**, **NF-108**).

`webapp/Dockerfile` and `webapp/nginx.conf` define the production image (**NF-104**). `webapp/config/deploy.yml` deploys that image with Kamal; see [operational-guides/build-and-deploy.md](operational-guides/build-and-deploy.md).
