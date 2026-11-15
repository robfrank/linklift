# Retire the separate webapp nginx image and deployment
<!-- intent: confirmed 2026-11-15 sha256:2a42144c8dd56364 -->

## Problem
Once delta `serve-frontend-from-api` lands, the API container serves the built webapp. The webapp's own nginx image (`webapp/Dockerfile`, `webapp/nginx.conf`), its separate Kamal service (`webapp/config/deploy.yml`, `.github/workflows/kamal-deploy-webapp.yml`) and the `webapp` service in `docker-compose.yml` become unused, and the separate deploy never succeeded. Left in place they describe a deployment that does not exist.

## Outcome
- The webapp has no Docker image or Kamal deployment of its own; its production bundle ships inside the API image.
- The local full stack (`docker-compose.yml`) runs without a separate webapp container.
- The webapp's build-and-deploy guide describes the single deployment.

## Baseline and rules
Trunk `webapp/design/` at the current head. Rules: typescript-developer (repo-local rule and its overrides). Standards: the repo's `CLAUDE.md` and `webapp/CLAUDE.md`.

## Footprint
**Adds or changes:** NF-104 no longer describes a webapp image; the bundle is built by the root `Dockerfile` into the API image.
**Relies on:** the dev server and its `/api` proxy (NF-102); `npm run build` producing `dist/` (NF-103).
**Out of scope:** the API-side serving and image build (delta `serve-frontend-from-api`); Webapp CI (NF-108); DNS and SSL.

## Constraints and quality
No change to webapp source, tests, the dev server or `npm run build`. Applied only after `serve-frontend-from-api` has landed, so the app is never left without a server.

## Priority and lane
Priority medium. Light lane.

## Decisions
- Remove the webapp image, its Kamal deployment and its docker-compose service — keeping them unused rejected: they describe a deployment that does not exist · accepted
- A separate delta in the webapp root — folding it into `serve-frontend-from-api` rejected: one delta per design root · accepted

## Open questions
None.

## Amendments
