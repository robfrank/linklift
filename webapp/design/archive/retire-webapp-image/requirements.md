# Delta Requirements: Retire the separate webapp nginx image and deployment

## Modified (overrides baseline)

- **NF-104** The webapp has no Docker image or deployment of its own: `webapp/` holds no `Dockerfile`, `nginx.conf`, `config/deploy.yml` or `.kamal/` directory, `webapp/package.json` has no `docker-build` script, no workflow under `.github/workflows/` deploys the webapp, and the root `docker-compose.yml` defines no webapp service. The production bundle that `npm run build` produces (**NF-103**) is built by the root `Dockerfile` into the API image, and the API serves it; that serving is owned by the backend design root (`design/`).
  - **Tags**: `build`, `guide:build-and-deploy`
  - **Falsifier**: deleting `webapp/Dockerfile` and `webapp/nginx.conf` but keeping the `webapp` compose service or the `Deploy Webapp` workflow passes a "no webapp image" check; ruled out by test row `T-no-webapp-deploy`.

## Assumes (read-only baseline dependencies)

- `NF-101` relative `/api/v1` base URL — relied upon as is (cited by the system-context section and the guide).
- `NF-102` the dev server and its `/api` proxy — relied upon as is.
- `NF-103` `npm run build` producing `dist/` — relied upon as is.
- `NF-108` the `Webapp CI` workflow — relied upon as is (cited by the build-config section and the guide).
- `RQ-501`, `RQ-503`, `RQ-506`, `RQ-521`, `RQ-540`, `RQ-593` — routing behaviour cited by the routing section, relied upon as is.
- `RQ-520` sign-in — relied upon as is (the guide's smoke test).
