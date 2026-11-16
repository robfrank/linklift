# Operational Guide: Build and Deploy

## Interfaces and Usage {#interfaces-and-usage}

- Production bundle: `npm run build` in `webapp/` (**NF-103**).
- Deployment (**NF-104**): the webapp has no image or deployment of its own. Once `serve-frontend-from-api` has landed (this delta's apply precondition), it ships inside the API image and is deployed with the API by the `Deploy API` workflow (`.github/workflows/kamal-deploy-api.yml`, Kamal config `config/deploy.yml`). Building, running and deploying that image are procedures of the backend design root (`design/`).
- Local full stack (**NF-104**): the root `docker-compose.yml` runs the API service `linklift` on port 7070, built from `Dockerfile.local`, which holds no webapp bundle; for the UI run `npm start` in `webapp/` beside it (**NF-102**).
- CI: `.github/workflows/webapp-ci.yml` (**NF-108**).

## Configuration {#configuration}

- The client base path is fixed in source (**NF-101**); changing it needs a rebuild of the API image (**NF-104**). How the API serves the bundle is configured in the backend design root.

## Verification and Smoke Tests {#verification-and-smoke-tests}

- After the production build, `webapp/dist/` must contain `index.html` and `bundle.js` (**NF-103**, **NF-104**).
- Against a running API container (**NF-104**), the smoke test is the backend design root's; for the webapp, a deep link such as `/collections` must return the app shell and signing in must succeed (**RQ-520**).

## Monitoring, Failure Modes, Rollback {#monitoring-failure-modes-rollback}

- The app shell and the API share one container (**NF-104**): if the API is down, the app does not load either.
- Rollback is Kamal's standard redeploy of a previous API image; this repository defines no webapp-specific rollback procedure.
