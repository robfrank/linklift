# Hexagonal architecture with manual wiring

## Status

Accepted (recorded retroactively in the retrofit baseline; the decision predates this design root).

## Context

The backend needs to swap infrastructure (ArcadeDB, Ollama, HTTP fetching, JWT) without touching business rules, and to test business rules with fakes. The project chose not to adopt an application framework with dependency injection.

## Decision

Organise the code as ports and adapters: use-case interfaces and command records in `src/main/java/it/robfrank/linklift/application/port/in/`, repository and gateway interfaces in `src/main/java/it/robfrank/linklift/application/port/out/`, services in `src/main/java/it/robfrank/linklift/application/domain/service/`, and framework code only under `src/main/java/it/robfrank/linklift/adapter/`. Construct every object by hand in `src/main/java/it/robfrank/linklift/Application.java` (NF-003). Use Javalin as a thin HTTP layer: controllers parse input, call one use case and map the result; routes, filters and exception handlers are registered in `src/main/java/it/robfrank/linklift/config/WebBuilder.java`.

## Consequences

- Services are unit-tested with Mockito or hand-written fakes such as `FakeEmbeddingGenerator`; controllers with `JavalinTest`.
- Wiring is explicit and greppable, at the cost of a long `Application.start` that must be edited for every new component; classes can exist unwired (`specifications.md#layering`).
- Nothing enforces the dependency rule at build time (the ArchUnit plugin in `pom.xml` is commented out); one violation exists today (NF-002).
