# Operational Guide: Dev Server

## Interfaces and Usage

- From `webapp/`: `npm ci`, then `npm start`. Behaviour of the server (port, history fallback, API proxy) is **NF-102**.
- The API must be running locally for anything past the sign-in page; the root `docker-compose.yml` defines it as the `linklift` service (with `arcadedb` and `ollama`), and the root `README.md` describes running it.
- Sign in or register through the UI (**RQ-520**, **RQ-524**); there is no dev-only bypass.

## Configuration

- The proxy target and port are set in `webapp/webpack.config.js` under `devServer` (**NF-102**). There are no environment variables; the API base path is fixed in source (**NF-101**).
- `webapp/test-api.js` is a separate manual script hard-coded to a different port; it does not use the dev server.

## Verification and Smoke Tests

- Open the dev server URL: an unauthenticated visit to `/` must land on `/login` (**RQ-502**).
- After sign-in, `/` must show `My Links` (**RQ-543**) and the drawer must list collections (**RQ-586**).
- `npm test` runs the automated suite (**NF-107**).

## Monitoring, Failure Modes, Rollback

- Every protected page showing empty lists or error alerts usually means the API is not reachable on the proxy target; check the dev-server terminal for proxy errors.
- Being sent back to `/login` after an action means the refresh in **RQ-526** failed (**RQ-527**); sign in again.
- Stale session data lives in the browser's `localStorage` (**NF-105**); clearing site data resets it.
