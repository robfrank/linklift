# nginx serves the bundle and reverse-proxies the API on the same origin

## Status

Accepted (retroactively recorded for the baseline)

## Context

The browser app must reach the Java API without CORS configuration or a build-time API URL.

## Decision

The production image (`webapp/Dockerfile`) copies the webpack output into `nginx:alpine`. `webapp/nginx.conf` falls back to `index.html` for client-side routes and proxies `/api/` to the `linklift` host. The client always calls the relative base `/api/v1`, and the dev server mirrors the proxy to a local API.

## Consequences

- No CORS and no per-environment build (**NF-101**, **NF-104**).
- The API location is fixed in `webapp/nginx.conf`; a different backend host needs a config change and an image rebuild.
- The upstream host name must resolve where the container runs (the root `docker-compose.yml` names its API service `linklift`; see [build-and-deploy](../operational-guides/build-and-deploy.md)).
