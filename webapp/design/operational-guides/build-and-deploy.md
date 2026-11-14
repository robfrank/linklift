# Operational Guide: Build and Deploy

## Interfaces and Usage

- Production bundle: `npm run build` in `webapp/` (**NF-103**).
- Docker image: `npm run docker-build` in `webapp/`, which tags `robfrank/linklift-webapp` (**NF-104**). The root `docker-compose.yml` runs that image as the `webapp` service on host port 80, next to the API service named `linklift` that nginx proxies to.
- Kamal: `webapp/config/deploy.yml` deploys the webapp as Kamal service `linklift` from image `robfrank/linklift` on registry `ghcr.io`, proxy host `linklift.arcadebrain.ai`, app port 80, health check path `/up`. The `Deploy Webapp` workflow (`.github/workflows/kamal-deploy-webapp.yml`) runs `kamal deploy` from `webapp/` on manual dispatch.
- CI: `.github/workflows/webapp-ci.yml` (**NF-108**).

## Configuration

- The API upstream is fixed in `webapp/nginx.conf` (**NF-104**); the client base path is fixed in source (**NF-101**). Changing either needs a rebuild of the image.
- Kamal secrets are read from `webapp/.kamal/secrets` and the workflow's repository secrets; their values are not part of this design.

## Verification and Smoke Tests

- After `npm run build`, `webapp/dist/` must contain `index.html` and `bundle.js` (**NF-103**).
- Against a running container: a deep link such as `/collections` must return the app shell, not an nginx 404 (**NF-104**), and signing in must succeed, which proves the `/api/` proxy reaches the API (**RQ-520**).

## Monitoring, Failure Modes, Rollback

- Sign-in failing with every page otherwise loading points at the `/api/` proxy: the upstream host from **NF-104** must resolve inside the container's network.
- The Kamal health check path `/up` has no dedicated nginx location; it is answered by the `index.html` fallback of **NF-104**, so a passing check proves only that nginx serves files, not that the API is reachable.
- Rollback is Kamal's standard redeploy of a previous image; this repository defines no webapp-specific rollback procedure.
