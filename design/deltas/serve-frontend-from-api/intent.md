# Serve the frontend from the API container
<!-- intent: confirmed 2026-11-15 sha256:380ccc1ac4e24ed9 -->

## Problem
The frontend is not deployed. Kamal deploys only the API (`linklift-api` on `linklift-api.arcadebrain.ai`) and the database; the separate nginx webapp service has never deployed successfully. The webapp calls the API at the relative path `/api/v1` and keeps its token in a header, so it works unchanged when served from the same origin as the API.

## Outcome
- Opening `https://linklift.arcadebrain.ai/` (or any app route, e.g. `/collections/42`, also after a refresh) loads the React app.
- The API answers exactly as today on the same host.
- One `kamal deploy` builds and ships the app and the API together.

## Baseline and rules
Trunk `design/` at the current head. Rules: java-developer (lint owner-run per the existing override). Standards: the repo's `CLAUDE.md`.

## Footprint
**Adds or changes:** `GET /` and every non-API path serve the built React bundle, with `index.html` for unknown non-API paths (RQ-003 modified, RQ-SFFA-001 new); the image Kamal builds contains the bundle, and Kamal serves the combined app at `linklift.arcadebrain.ai` (NF-SFFA-001 new).
**Relies on:** `/up` health (RQ-002); CORS (RQ-005); `/api/v1/*` authorization and JSON errors (RQ-026).
**Out of scope:** retiring the nginx webapp image, `webapp/config/deploy.yml` and its workflow (delta `retire-webapp-image`); DNS for `linklift.arcadebrain.ai`; SSL on the proxy; tightening CORS.

## Constraints and quality
API behaviour unchanged: `/api/v1/*`, `/up` and `/metrics` answer as today, and an unknown `/api/...` path still gets the API's JSON 404, never `index.html`. Static files need no credentials. Running the JAR without a bundle (tests, local runs) still works. The image still runs as the non-root `appuser` on Temurin 25.

## Priority and lane
Priority medium. Light lane.

## Decisions
- Serve the frontend from the API container — GitHub Pages rejected: mixed content with the SSL-less proxy, no SPA fallback, cross-origin; a second Kamal service rejected: more services and config · accepted
- Two deltas, one per design root — a single backend-only delta rejected: leaves the nginx image and webapp Kamal files stale · accepted
- Serve at `linklift.arcadebrain.ai` — keeping `linklift-api.arcadebrain.ai` or serving both hosts rejected: the user chose the friendly name · accepted
- Multi-stage root Dockerfile (Node build, Maven build, Temurin runtime) — the bundle inside the JAR via frontend-maven-plugin rejected: every `mvn package` would need Node · accepted

## Open questions
None.

## Amendments
