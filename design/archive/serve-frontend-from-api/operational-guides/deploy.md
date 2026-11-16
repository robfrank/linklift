# Operational Guide: Deploy with Kamal

## Interfaces and Usage {#interfaces-and-usage}

Production deployment uses Kamal with `config/deploy.yml` and the GitHub workflow `.github/workflows/kamal-deploy-api.yml` (manual `workflow_dispatch`; the `env` input is echoed but not used by `kamal deploy`). The workflow installs Kamal, loads the SSH key, logs in to `ghcr.io` and runs `kamal deploy` then `kamal details`.

`kamal deploy` builds the repository-root `Dockerfile` into one image that carries both the React app and the API (NF-SFFA-001), and the container serves both on the same host (RQ-003, RQ-SFFA-001). No separate frontend service is deployed.

`config/deploy.yml` declares:

- service `linklift-api`, image `robfrank/linklift-api` on registry `ghcr.io`, builder `amd64`, one `web` server at `HOST_IP`, SSH user `player`;
- kamal-proxy for the host `linklift.arcadebrain.ai` with `app_port: 7070` and health-check path `/up` (RQ-002), SSL off (NF-SFFA-001);
- clear env `JAVA_OPTS` (heap 2G and `-Dlinklift.arcadedb.host=linklift-arcadedb`), `LINKLIFT_OLLAMA_URL=http://linklift-ollama:11434`, `LINKLIFT_OLLAMA_MODEL`, `LINKLIFT_OLLAMA_DIMENSIONS` (see `operational-guides/configuration.md`);
- accessories `arcadedb` (`arcadedata/arcadedb-headless`, data under `/home/player/linklift/`) and `ollama` (`ollama/ollama`, models under `/home/player/linklift/ollama`), both pinned by digest.

The image sets `LINKLIFT_WEBAPP_DIR` itself (NF-SFFA-001), so `config/deploy.yml` does not. The Docker Hub image `robfrank/linklift` built by `.github/workflows/auto-release.yml` (`mvn deploy -Pdocker` on a version tag) carries no frontend and is not what Kamal deploys. DNS for `linklift.arcadebrain.ai` is managed outside this repository.

## Frontend smoke test {#frontend-smoke}

After a deploy, beside the checks in `#verification`: `curl -H 'Host: linklift.arcadebrain.ai' http://$HOST_IP/` and the same for an app route such as `/collections/42` both answer with the app's `index.html` (RQ-003, RQ-SFFA-001), and `/up` still answers 200 (RQ-002).
