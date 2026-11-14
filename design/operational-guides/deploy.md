# Operational Guide: Deploy with Kamal

## Interfaces and Usage {#interfaces-and-usage}

Production deployment uses Kamal with `config/deploy.yml` and the GitHub workflow `.github/workflows/kamal-deploy-api.yml` (manual `workflow_dispatch`; the `env` input is echoed but not used by `kamal deploy`). The workflow installs Kamal, loads the SSH key, logs in to `ghcr.io` and runs `kamal deploy` then `kamal details`.

`config/deploy.yml` declares:

- service `linklift-api`, image `robfrank/linklift-api` on registry `ghcr.io`, builder `amd64`, one `web` server at `HOST_IP`, SSH user `player`;
- kamal-proxy with `app_port: 7070` and health-check path `/up` (RQ-001, RQ-002), SSL off;
- clear env `JAVA_OPTS` (heap 2G and `-Dlinklift.arcadedb.host=linklift-arcadedb`), `LINKLIFT_OLLAMA_URL=http://linklift-ollama:11434`, `LINKLIFT_OLLAMA_MODEL`, `LINKLIFT_OLLAMA_DIMENSIONS` (RQ-152, RQ-153);
- accessories `arcadedb` (`arcadedata/arcadedb-headless`, data under `/home/player/linklift/`) and `ollama` (`ollama/ollama`, models under `/home/player/linklift/ollama`), both pinned by digest.

The released image is built by `.github/workflows/auto-release.yml` (`mvn deploy -Pdocker` on a version tag). The image runs as a non-root user on port 7070 (NF-007).

## Configuration {#configuration}

Required GitHub secrets for the workflow: `KAMAL_REGISTRY_USERNAME`, `KAMAL_REGISTRY_PASSWORD`, `HOST_IP`, `SSH_PRIVATE_KEY`. `.kamal/secrets` supplies `KAMAL_REGISTRY_PASSWORD`.

`config/deploy.yml` sets no `LINKLIFT_JWT_SECRET` and no `env.secret` list. Under RQ-150 and RQ-151 a production container needs one, or `ENVIRONMENT` containing `dev`/`local`; this baseline does not know how the running host supplies it.

<!-- verify: grep -c 'LINKLIFT_JWT_SECRET' config/deploy.yml == 0 -->

The ArcadeDB accessory sets no root password in `config/deploy.yml`, while the API connects with the fixed credentials of RQ-153; they must agree on the host.

## Verification and Smoke Tests {#verification}

- kamal-proxy only routes traffic to a container whose `/up` answers 200 (RQ-002).
- `kamal details` (run by the workflow) lists the app and accessory containers.
- After deploy, `curl https://<proxy host>/up` and an authenticated `GET /api/v1/links` (RQ-035).

## Monitoring, Failure Modes, Rollback {#monitoring}

- Container restarts at boot with "JWT secret not configured": RQ-150.
- `/metrics` exposes JVM metrics in Prometheus format (RQ-004); logs are JSON on stdout (NF-006), readable with `kamal app logs`.
- Rollback: `kamal rollback <version>` to a previously deployed image tag (standard Kamal; not scripted in this repository).
