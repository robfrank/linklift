# Operational Guide: General

## Interfaces and Usage {#interfaces-and-usage}

Fallback guide for requirements without a specific `guide:` tag. The HTTP surface is listed in `specifications.md#http-surface`; every `/api/v1/*` route except the four authentication routes needs `Authorization: Bearer <accessToken>` (RQ-026). Errors use the format of RQ-130 except the plain-text and validator answers listed in `specifications.md#error-handling`.

Topic guides: `operational-guides/local-run.md`, `operational-guides/deploy.md`, `operational-guides/configuration.md`.

## Verification and Smoke Tests {#verification}

- Health: `/up` (RQ-002). Metrics: `/metrics` (RQ-004).
- Automated: `mvn test` (NF-008).

## Monitoring, Failure Modes, Rollback {#monitoring}

- Every domain event is logged by its subscriber (RQ-141); content-download failures are logged at ERROR with the link id (RQ-058).
- The admin backfill route answers 403 for every user (RQ-075); there is no supported way to trigger an embedding backfill over HTTP.
