# Retire the separate webapp nginx image and deployment
<!-- footprint: NF-104 modify · spec:specifications#system-context modify · spec:specifications#routing modify · spec:specifications#build-config modify · doc:operational-guides/build-and-deploy#interfaces-and-usage modify · doc:operational-guides/build-and-deploy#configuration modify · doc:operational-guides/build-and-deploy#verification-and-smoke-tests modify · doc:operational-guides/build-and-deploy#monitoring-failure-modes-rollback modify -->

## What changes

The webapp stops having a Docker image, nginx config, Kamal deployment and docker-compose service of its own. Its production bundle ships inside the API image, which the backend design root owns. The build-and-deploy guide, three specification sections describe the single deployment.

## Why

Once `serve-frontend-from-api` lands, the API container serves the bundle, and the separate webapp image and deploy (which never succeeded) describe a deployment that does not exist.

## Impact and risk

- No webapp source, test, dev-server or production-build change.
- Applying before `serve-frontend-from-api` would leave the app without a server; the apply precondition in [implementation.md](implementation.md) guards it.
- The webapp can no longer be deployed independently of the API.

## Changelog

The webapp no longer has its own nginx image or Kamal deployment: its production bundle now ships inside the API container and is deployed with the API. The local Docker Compose stack no longer starts a separate webapp container; run the UI locally with the webpack dev server next to the API.

## Notes

- 2026-11-15 · author — `webapp/.kamal/` (secrets template and sample hooks) is removed with `webapp/config/deploy.yml`: it exists only for that Kamal service.
- 2026-11-15 · author — the `docker-build` script in `webapp/package.json` is removed because it builds the deleted `webapp/Dockerfile`; this is the only `package.json` change and does not touch `npm run build`.
- 2026-11-15 · author — `webapp/README.md`'s "Docker Integration" section is updated, since it describes the removed nginx container.
- 2026-11-15 · author — the spec sections `system-context`, `routing` and `build-config` and all four sections of the build-and-deploy guide are modified, because each states that nginx or the webapp image serves the app.
- 2026-11-15 · author — no ADR is added (the light lane forbids one), so trunk ADR-004 (nginx serves the bundle) stays as written; the owner may want to mark it superseded.
- 2026-11-15 · author — the apply-order constraint is carried by the implementation's apply precondition and test row `T-api-image-builds-webapp`, not by a requirement, because it is an ordering rule rather than observable behaviour.
- 2026-11-15 · author — the commented-out `run docker-build` execution in `pom.xml` is left alone: it is inert and the root file belongs to the backend design root.
- 2026-11-15 · review r1 — trunk `webapp/design/implementation.md` goes stale with this delta and is not in its footprint: the file-table row for `webapp/Dockerfile`, `webapp/nginx.conf`, `webapp/config/deploy.yml`, the `npm run docker-build` row of the commands table (**NF-104**), and the Testing Plan sentence naming `webapp/Dockerfile`; correct them at fold, as for ADR-004.
- 2026-11-15 · gate — review: 3 round(s), lane full — blockers in round(s) 1, 2 — 1 finding(s) were damage from a fix pass.
- 2026-11-16 · round 01 conductor — Removed webapp/Dockerfile, nginx.conf, config/deploy.yml, the 10 tracked webapp/.kamal files, .github/workflows/kamal-deploy-webapp.yml, the compose webapp service, the docker-build npm script and the dependabot docker /webapp entry; README.md now points the local UI at the dev server (http://localhost:3000), webapp/README.md says the bundle ships in the API image.
- 2026-11-16 · round 01 conductor — Gates: npm ci, prettier lint/format-check and npm test / test:coverage (8 suites, 34 tests) green. type-check is owner-run; tsc still reports the known MUI 9 source errors in untouched files.
- 2026-11-16 · round 01 conductor — docker compose warns that the version attribute is obsolete; this predates the delta and was left alone.
