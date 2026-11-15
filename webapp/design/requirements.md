# Baseline Requirements

Behaviour of the LinkLift React webapp (`webapp/`) as built. Functional requirements use `RQ-501`..`RQ-999`; non-functional requirements use `NF-101`..`NF-199`. Paths in requests are relative to the browser origin; every API call goes through the `/api/v1` base (**NF-101**).

## Application shell and routing

- **RQ-501** The paths `/login` and `/register` render the sign-in and sign-up pages to a visitor who is not authenticated.
  - **Tags**: `routing`, `auth`
- **RQ-502** The paths `/`, `/add`, `/collections`, `/collections/:id`, `/search`, `/graph`, `/ask` and `/admin` are protected: while the stored session is being restored a centred progress indicator is shown, and when no user is authenticated the browser is redirected (history replace) to `/login`.
  - **Tags**: `routing`, `auth`
- **RQ-503** Any path that matches no route renders a page showing `404`, `Page Not Found`, `The page you are looking for does not exist.` and a `Return to Home` button that navigates to `/`; this page is shown without authentication.
  - **Tags**: `routing`
- **RQ-504** Every protected page renders inside a layout made of a fixed top bar whose `LinkLift` title navigates to `/` when clicked, and a permanent left drawer 240px wide.
  - **Tags**: `routing`, `layout`
- **RQ-505** The left drawer offers exactly these navigation targets: `All Links` (`/`), `Ask AI` (`/ask`), one entry per collection (`/?collection=<id>`) and `Manage Collections` (`/collections`); no rendered control navigates to `/search`, `/graph` or `/admin`, which are reachable only by entering the URL.
  - **Tags**: `routing`, `layout`
- **RQ-506** The `collection` query parameter on `/` only marks the matching drawer entry as selected; the link list shown and the list request sent are the same as without it.
  - **Tags**: `routing`, `links`
- **RQ-507** The top-bar menu button (shown only below the `sm` breakpoint) and the account icon change nothing visible when clicked; no rendered page offers a sign-out control.
  - **Tags**: `layout`, `auth`
- **RQ-508** When a render error escapes a component, the whole application is replaced by a panel reading `Something went wrong` with a `Go to Home` button that performs a full page load of `/`; the error text is printed in the panel only when `NODE_ENV` is `development` or `test`.
  - **Tags**: `errors`

## Authentication

- **RQ-520** The sign-in form has the fields `Username or Email`, `Password` and a `Remember me` checkbox, and submitting it sends `POST /api/v1/auth/login` with the JSON body `{loginIdentifier, password, rememberMe}`.
  - **Tags**: `auth`
- **RQ-521** After a successful sign-in the webapp stores `accessToken`, `refreshToken` and `user` (a JSON object of `userId`, `username`, `email`, `firstName`, `lastName`) in `localStorage`, sends `Authorization: Bearer <accessToken>` on every later API request, and navigates to `/` regardless of the page that triggered the redirect to `/login`.
  - **Tags**: `auth`
- **RQ-522** A failed sign-in shows an error alert holding the response body's `message`, or `Login failed` when the response has none; a 401 answer is first routed through the refresh of **RQ-526**/**RQ-527**, and when that refresh fails the alert reads `Login failed`; the form fields and submit button are disabled while the request is pending.
  - **Tags**: `auth`, `errors`
- **RQ-523** The sign-up form has the fields `First Name`, `Last Name`, `Username`, `Email Address`, `Password` and `Confirm Password`; it sends nothing and shows `Passwords do not match` when the two passwords differ, or `Password must be at least 8 characters long` when the password is shorter than 8 characters.
  - **Tags**: `auth`
- **RQ-524** A valid sign-up sends `POST /api/v1/auth/register` with `{username, email, password, firstName, lastName}`; on success it shows `Registration successful! Please log in with your new account.` and navigates to `/login` 2000 ms later without signing the user in; on failure it shows the response `message` or `Registration failed`.
  - **Tags**: `auth`, `errors`
- **RQ-525** On page load, when `localStorage` holds both `accessToken` and `user`, the user is treated as authenticated from the stored `user` object without any request to the server.
  - **Tags**: `auth`
- **RQ-526** When an API response has status 401, the request URL does not contain `/auth/refresh`, and the request has not already been retried, the webapp sends `POST /api/v1/auth/refresh` with `{refreshToken}`, stores the returned tokens and user as in **RQ-521**, and re-sends the original request once with the new bearer token.
  - **Tags**: `auth`
  - **Falsifier**: an interceptor that refreshes but returns the original 401 instead of re-sending passes a "refresh is called" check; no automated test rules it out today.
- **RQ-527** When the refresh in **RQ-526** fails or no refresh token is stored, the webapp sends `POST /api/v1/auth/logout` (ignoring its outcome), removes `accessToken`, `refreshToken` and `user` from `localStorage`, drops the bearer header and clears the user, so protected routes redirect to `/login` (**RQ-502**); the original request fails with `Session expired. Please login again.`
  - **Tags**: `auth`, `errors`

## Link list

- **RQ-540** The page `/` shows `List` and `Graph` tabs; the query `view=graph` selects `Graph`, any other or no value selects `List`, and choosing a tab writes the matching `view` value into the URL.
  - **Tags**: `links`
- **RQ-541** With the `List` tab active, `/` requests `GET /api/v1/links` with the query parameters `page` and `size` taken from the client store (initially `0` and `20`) and no sort parameters, and re-requests whenever the page or page size changes.
  - **Tags**: `links`
- **RQ-542** The drawer's `Sort by` (`Date Added`, `Title`, `URL`), `Order` (`Ascending`, `Descending`) and `Per page` (`5`, `10`, `20`, `50`) selectors update the client store, initially `extractedAt` / `DESC` / `20`; any change resets the page to the first one, and the sort values are sent only by the list reload that follows a deletion (**RQ-565**).
  - **Tags**: `links`
- **RQ-543** The list is headed `My Links` with a count reading `<N> links total` (`link` when N is 1), taken from the response `totalElements`.
  - **Tags**: `links`
- **RQ-544** Each link card shows the title, a chip with the URL's host name (the raw URL when it does not parse), the description, `Added <date>` formatted in `en-US` with date and time, and the chips `Unread`, `Reading` and `Archived` when the link's read status or archived flag says so.
  - **Tags**: `links`
- **RQ-545** A pagination control with first/last buttons appears only when the response reports more than one page; it shows 1-based page numbers and choosing page _p_ requests page _p − 1_.
  - **Tags**: `links`
- **RQ-546** While the list is loading and empty a single centred progress indicator replaces the list; while it is loading and already holds links a small progress indicator appears below them.
  - **Tags**: `links`, `loading`
- **RQ-547** An empty, settled list shows `No links found` and `Start by adding your first link!`.
  - **Tags**: `links`
- **RQ-548** A failed list request shows the error alert `Failed to load links. Please try again.` above the list.
  - **Tags**: `links`, `errors`
- **RQ-549** Each card's `Visit` button opens the link URL in a new tab, and its `Edit` button is always disabled.
  - **Tags**: `links`
- **RQ-550** Each card's favourite, archive and read toggles send `PATCH /api/v1/links/:id/status` with one field — `{favorited: !current}`, `{archived: !current}`, or `{readStatus: "UNREAD"}` when the link is `READ` and `{readStatus: "READ"}` otherwise — and replace the card's link with the response `data`; a failed toggle shows nothing to the user.
  - **Tags**: `links`, `errors`

## Adding and deleting links

- **RQ-560** The page `/` shows a floating add button that navigates to `/add`.
  - **Tags**: `links`
- **RQ-561** The page `/add` shows `Add New Link` with the fields `URL`, `Title` and `Description`; the URL is required (`URL is required`) and must parse as an absolute URL (`Please enter a valid URL`), title and description are optional, and an invalid form sends nothing.
  - **Tags**: `links`
- **RQ-562** A valid submission sends `PUT /api/v1/link` with `{url, title, description}`; while pending the fields and both buttons are disabled and the submit button reads `Adding...`.
  - **Tags**: `links`, `loading`
- **RQ-563** On success the webapp shows the success notification `Link added successfully!` and navigates to `/` 1500 ms later; `Cancel` navigates to `/` immediately.
  - **Tags**: `links`
- **RQ-564** A failed submission shows an error notification chosen in this order: status 409 → `This URL already exists`; a response body `message` → that message; status 400 → `Invalid link data. Please check your input.`; status ≥ 500 → `Server error. Please try again later.`; no response → `Unable to connect to server. Please check your connection.`; otherwise `Failed to add link`.
  - **Tags**: `links`, `errors`
- **RQ-565** A card's delete button opens a `Delete Link` dialog naming the link's title; `Cancel` closes it without a request, and `Delete` sends `DELETE /api/v1/links/:id` and then re-requests `GET /api/v1/links` with `page`, `size`, `sortBy` and `sortDirection`; a failed deletion shows nothing to the user.
  - **Tags**: `links`, `errors`

## Tags

- **RQ-570** Each link card requests `GET /api/v1/links/:id/tags` when rendered and shows the returned tags as chips; deleting a chip sends `DELETE /api/v1/links/:id/tags/:tagId` and removes the chip.
  - **Tags**: `tags`
- **RQ-571** On a link card the tag input starts collapsed; expanding it with the `Manage tags` button requests `GET /api/v1/tags` when no tags are loaded yet, and the input suggests only tags not already on that link.
  - **Tags**: `tags`
- **RQ-572** Adding a tag (Enter, the add button, or picking a suggestion) reuses a loaded tag whose name equals the lower-cased trimmed input, otherwise sends `POST /api/v1/tags` with `{name}`; it then sends `POST /api/v1/links/:id/tags` with `{tagId}` and reloads that link's tags. Tag failures show nothing to the user.
  - **Tags**: `tags`, `errors`

## Collections

- **RQ-580** The page `/collections` requests `GET /api/v1/collections` and shows `My Collections` with one card per collection (name, and the description or `No description`); it shows a progress indicator during the first load, `No collections found` when the list is empty, and `Failed to load collections. Please try again.` when the request fails.
  - **Tags**: `collections`, `loading`, `errors`
- **RQ-581** `New Collection` opens a `Create New Collection` dialog whose `Create` button is disabled while the name is blank; `Create` sends `POST /api/v1/collections` with `{name, description}`, and on success closes the dialog, reloads the list and notifies `Collection created successfully`; on failure it notifies `Failed to create collection`.
  - **Tags**: `collections`, `errors`
- **RQ-582** Clicking a collection card navigates to `/collections/:id`; its delete icon asks the browser confirmation `Are you sure you want to delete this collection?` and, when confirmed, sends `DELETE /api/v1/collections/:id`, reloads the list and notifies `Collection deleted successfully` (or `Failed to delete collection`).
  - **Tags**: `collections`, `errors`
- **RQ-583** The page `/collections/:id` requests `GET /api/v1/collections/:id` and shows breadcrumbs `Home / Collections / <name>`, the description, `Links (<N>)` and one row per member link with `Content`, `Visit` and remove actions; an empty collection shows `No links in this collection yet` with `Browse Links to Add` (to `/`), and a failed request shows `Failed to load collection details.` with `Back to Collections`.
  - **Tags**: `collections`, `errors`
- **RQ-584** Removing a member link asks the browser confirmation `Remove this link from the collection?` and, when confirmed, sends `DELETE /api/v1/collections/:id/links/:linkId` and drops the row; a failed removal shows nothing to the user.
  - **Tags**: `collections`, `errors`
- **RQ-585** A link card's `Add to Collection` button opens a dialog that requests `GET /api/v1/collections` and lists them (or `No collections found. Create a collection first.`); choosing one sends `POST /api/v1/collections/:collectionId/links` with `{linkId}` and closes the dialog with no notification; a 409 would show `Link already exists in this collection` and any other failure shows `Failed to add link to collection` inside the dialog. The backend answers 204 for a repeated add and never 409 (backend root `RQ-103`), so the 409 message is unreachable today and re-adding a member closes the dialog as a success.
  - **Tags**: `collections`, `errors`
- **RQ-586** The left drawer requests `GET /api/v1/collections` when it mounts and lists each collection by name (**RQ-505**).
  - **Tags**: `collections`, `layout`

## Search

- **RQ-590** Pressing Enter in the top bar's `Search links...` field with non-blank text sends `GET /api/v1/search` with the query parameters `q=<text>` and `limit=10`, stores the response body unchanged as the result list, and navigates to `/?view=list`.
  - **Tags**: `search`
- **RQ-591** While a search is pending or has returned at least one result, the `List` tab of `/` renders the results instead of the link list, reading each result as an object whose `data` field is a content record: the extracted title (or `Untitled Content`), the first available of summary, extracted description, or the first 200 characters of text followed by `...` (else `No summary available.`), `Link ID: <id>` and a `View Full Content` button. Because `GET /api/v1/search` answers a bare array of content records (no `data` wrapper), any non-empty answer makes this rendering throw and the error panel of **RQ-508** replaces the application. No control clears the results; a search with no results or a failed search leaves the link list in place and shows no error on `/`.
  - **Tags**: `search`
- **RQ-592** While search results are present, `/` computes the highlighted node ids from each result's `data.linkId` and the `Graph` tab colours nodes whose id is among them with the highlight colour and dims every other node; with a bare-array search answer this computation throws during render, so the error panel of **RQ-508** appears on either tab.
  - **Tags**: `search`, `graph`
- **RQ-593** The page `/search?q=<q>` runs the search of **RQ-590** on load when `q` is non-empty and shows `Search Results`, `Showing results for: <q> (Vector Search)`, a progress indicator while pending, `Search failed. Please try again.` on failure, `No results found for your query.` when empty, and result cards read as in **RQ-591**, so a non-empty bare-array answer likewise ends in the error panel of **RQ-508**.
  - **Tags**: `search`, `loading`, `errors`

## Content viewer

- **RQ-600** `View Content` on a link card, `View Full Content` on a search result and `Content` on a collection member each open a modal dialog titled with the link's title that requests `GET /api/v1/links/:id/content`.
  - **Tags**: `content`
- **RQ-601** The modal body shows: `Loading content...` while loading (or `Refreshing content...` when content is already shown); on a failed request a panel titled `Error` with `An error occurred while loading content` and a `Try Again` button that repeats the request; `Content download pending...` for status `PENDING`; `Downloading content...` for `IN_PROGRESS`; `Content download failed.` with `Try Again` for `FAILED`; the content for `COMPLETED`; and `No content available` when the response holds nothing.
  - **Tags**: `content`, `loading`, `errors`
- **RQ-602** Completed content opens in `Text View`, showing the extracted text preformatted (or `No text content available`); `HTML Preview` shows the stored HTML after sanitising it to an allowlist of tags and attributes, inside an iframe whose `sandbox` attribute is `allow-same-origin`.
  - **Tags**: `content`, `security`
- **RQ-603** The modal's refresh button sends `POST /api/v1/links/:id/content/refresh` and then requests the content again; the `Try Again` of a `FAILED` download does the same.
  - **Tags**: `content`
- **RQ-604** The modal footer shows `Size:` in bytes/KB/MB/GB (base 1024, two decimals), `Type:` (the MIME type or `N/A`) and `Downloaded:` as `Just now`, `<n> minute(s) ago`, `<n> hour(s) ago` or `<n> day(s) ago` for the last 7 days, else an `en-US` short date.
  - **Tags**: `content`
- **RQ-605** The modal closes on its `×` button, on the Escape key and on a click outside its panel, and the page behind it does not scroll while it is open.
  - **Tags**: `content`

## Graph

- **RQ-610** The `Graph` tab of `/` requests `GET /api/v1/graph` and draws the returned nodes and edges (the response's `edges` drawn as links) as a force-directed graph with each node's `label` as its hover text; it shows a progress indicator while loading and `No graph data available.` when there are no nodes; clicking a node there changes nothing on screen.
  - **Tags**: `graph`, `loading`
- **RQ-611** The page `/graph` shows `Link Graph` with the same graph; clicking a node requests `GET /api/v1/links/:id/related`, adds a node (labelled by title, else URL) for each related link not yet drawn, and adds an edge from the clicked node to every related link, already drawn or not, unless that same clicked-to-related edge already exists.
  - **Tags**: `graph`

## Ask

- **RQ-620** The page `/ask` shows `Ask Your Links`; Enter (without Shift) or the send button sends `POST /api/v1/ask` with `{question}` and appends the question, answer and sources to a conversation; the send button is disabled while the input is blank or a question is pending, and `Searching your links...` shows while pending.
  - **Tags**: `ask`, `loading`
- **RQ-621** The empty conversation shows three suggestion chips that copy their text into the input; a failed question shows an error alert; the clear button empties the conversation; the conversation lives only in memory, survives navigation between pages and is lost on page reload.
  - **Tags**: `ask`, `errors`
- **RQ-622** A source in an answer is rendered as a link with an `href` only when its URL starts with `http://` or `https://` (any case), and opens in a new tab.
  - **Tags**: `ask`, `security`

## Admin

- **RQ-630** The page `/admin` shows `Admin Dashboard` with a `Trigger Embedding Backfill` button that sends `POST /api/v1/admin/backfill-embeddings`; while pending it reads `Backfilling...` and is disabled; afterwards a success alert shows the response status text (or `Started`), or an error alert shows `Failed to start backfill process.`. The backend requires the `ADMIN_ACCESS` permission, which no user holds (backend root `RQ-075`, `RQ-025`), so every press today ends in that error alert; the 403 answer is not handled by the 401-only refresh of **RQ-526**.
  - **Tags**: `admin`, `errors`

## Notifications and theming

- **RQ-640** Notifications appear as a filled alert at the bottom centre of the window, close themselves after 6000 ms, close on their close button, and ignore clicks elsewhere on the page.
  - **Tags**: `notifications`
- **RQ-641** The interface uses a single light Material UI theme with primary colour `#1976d2` and secondary colour `#dc004e`, the Roboto font loaded from Google Fonts and a page background of `#f5f5f5`; there is no dark mode or theme switch.
  - **Tags**: `theming`
- **RQ-642** The browser tab title is `LinkLift - Link Management System`.
  - **Tags**: `theming`

## Non-functional

- **NF-101** Every API request uses the relative base URL `/api/v1` and the header `Content-Type: application/json`; the base URL is fixed in source and no environment variable or runtime setting changes it.
  - **Tags**: `api`, `guide:build-and-deploy`
- **NF-102** `npm start` serves the app with webpack-dev-server on port 3000, falls back to `index.html` for unknown paths, and proxies requests under `/api` to `http://localhost:7070`.
  - **Tags**: `guide:dev-server`
- **NF-103** `npm run build` bundles `src/index.tsx` into the single file `dist/bundle.js` (no content hash) plus `dist/index.html`, transpiling JavaScript and TypeScript with Babel and no type check.
  - **Tags**: `build`, `guide:build-and-deploy`
- **NF-104** The Docker image is built in two stages — `npm ci` and `npm run build` on `node:26-alpine`, then the `dist` output served by `nginx:alpine` on port 80 — and nginx answers unknown paths with `index.html`, gzips text, CSS, JSON, JavaScript and XML responses of at least 1000 bytes, and proxies `/api/` to `http://linklift:7070`.
  - **Tags**: `build`, `guide:build-and-deploy`
- **NF-105** Session tokens are kept in `localStorage` and sent as a bearer header; the webapp sets no cookies.
  - **Tags**: `auth`, `security`
- **NF-106** Every link the webapp opens in a new tab carries `rel="noopener noreferrer"`.
  - **Tags**: `security`
- **NF-107** `npm test` runs Jest in a jsdom environment, transforming sources with the project's own `babel-jest` and `@babel/core` 8 through the presets in `webapp/.babelrc`, mapping CSS imports to `identity-obj-proxy` and replacing `react-force-graph-2d` with a stub; `webapp/package-lock.json` resolves `react` and `react-dom` to the same version, `19.3.1`, and all 8 suites (34 cases) run and pass on Node 24 (24.11 or later, Babel 8's engine floor) and on Node 26.
  - **Tags**: `test`
  - **Falsifier**: aligning only `react-dom` leaves Jest on its nested Babel 7 core, so the suite importing a TypeScript enum still fails; ruled out by test rows `T-jest-transform-path` and `T-jest-all-suites-local`.

- **NF-108** The `Webapp CI` workflow runs on pushes and pull requests to `main` that touch `webapp/**` and executes `npm ci`, `npm test` and `npm run build` on Node 24.
  - **Tags**: `ci`, `guide:build-and-deploy`
  - **Falsifier**: setting `node-version` to `22` or `26` also satisfies Babel 8's engine range and turns CI green; ruled out by test row `T-ci-node-24`.

- **NF-FWT-001** Running `npx tsc` in `webapp/` under TypeScript 7 (the config sets `noEmit`) reports no error against `webapp/tsconfig.json` itself: `compilerOptions.target` is `ES2022` and `compilerOptions.moduleResolution` is `bundler`, and every other compiler option keeps its value. Type errors reported against source files are out of scope here, and no npm script or CI step runs `tsc`.
  - **Tags**: `build`
  - **Falsifier**: deleting `target` and `moduleResolution` also silences both config errors but changes the effective resolution; ruled out by test row `T-tsconfig-options`.
