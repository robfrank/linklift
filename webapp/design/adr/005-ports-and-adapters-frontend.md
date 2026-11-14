# Ports-and-adapters layout with a singleton DI container

## Status

Accepted (retroactively recorded for the baseline)

## Context

`FRONTEND_ARCHITECTURE_REFACTOR_PLAN.md` proposed mirroring the backend's hexagonal architecture so business logic would not depend on React.

## Decision

`webapp/src/domain/` holds models, repository ports and use-case classes; `webapp/src/application/services/` holds axios adapters; `webapp/src/application/di-container.ts` is a lazily created singleton that wires them; `webapp/src/infrastructure/ui/` holds React pages, components and hooks. See [api-client](../specifications.md#api-client).

## Consequences

- Link, content, search and add-to-collection flows go through ports and use cases.
- The migration is incomplete: collections pages, tags, Ask, graph and auth call the axios instance or adapters directly, so the layering is a convention the build does not enforce.
- Use cases only delegate to repositories today and have no tests.
