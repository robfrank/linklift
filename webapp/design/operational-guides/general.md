# Operational Guide: General

## Interfaces and Usage

- The webapp is a browser single-page application; its pages and behaviour are the requirements in [requirements.md](../requirements.md) without a `guide:` tag, and its mechanism is [specifications.md](../specifications.md).
- Developer procedures live in [dev-server.md](dev-server.md); packaging and deployment in [build-and-deploy.md](build-and-deploy.md).

## Configuration

- The webapp has no runtime configuration of its own (**NF-101**). User session state is held in the browser (**NF-105**).

## Verification and Smoke Tests

- Automated checks: `npm test` in `webapp/` (**NF-107**); CI repeats it with a build (**NF-108**).
- Manual check of the main path: sign in (**RQ-520**), add a link (**RQ-562**), see it listed (**RQ-541**), open its content (**RQ-600**), delete it (**RQ-565**).

## Monitoring, Failure Modes, Rollback

- The webapp sends no telemetry; errors are written to the browser console only.
- A render crash shows the error panel of **RQ-508**; its `Go to Home` button reloads `/`.
- Several failures are silent as built (**RQ-550**, **RQ-565**, **RQ-572**, **RQ-584**); check the browser network panel when an action appears to do nothing.
