# Fix the red backend build
<!-- footprint: NF-008 modify · NF-FBB-001 add -->

## What changes

Test sources and formatting only. The Testcontainers tests start the published full ArcadeDB image at the library's version, every JavalinTest-based test app listens on loopback through one shared helper, and the Ask tests are reformatted so the prettier check in CI passes.

## Why

Trunk's backend build is red: CI stops at the prettier check before Maven runs, the Testcontainers tests ask for an image tag that is not published, and the graph controller's unauthenticated test fails in the full suite while passing alone.

## Impact and risk

No application behaviour changes; production code is not touched. The risk is confined to the test suite: a test that relied on the headless image's defaults could behave differently on the full image, which `T-full-suite` would show.

## Changelog

Backend build green again: Testcontainers tests use the full ArcadeDB image at the library version, JavalinTest apps bind to loopback, and the Java sources pass the prettier check.

## Notes

- 2026-11-14 · author — `ArcadeAuthTokenRepositoryTest`, which used the `latest` tag rather than the headless image, is switched to the library version too, because NF-008 as written covers every ArcadeDB container.
- 2026-11-14 · author — the shared helper sets the host on an already-built app (`app.unsafe.jetty.host`) instead of wrapping `Javalin.create`, so `ObservabilityTest`'s `WebBuilder.build()` app is covered without changing production code.
- 2026-11-14 · author — the helper is named `LoopbackJavalin`, not `…Test`, so Surefire's default includes do not treat it as a test class.
- 2026-11-14 · author — NF-FBB-001's CI enforcement is already wired (pre-commit prettier hook in both workflows' `check` job); no workflow file changes.
- 2026-11-14 · author — the "passes" outcome is carried in NF-008 rather than a new requirement, since the intent's footprint lists only NF-008 and NF-FBB-001.
- 2026-11-14 · author — all work sits in round 00: its acceptance (every java-developer gate green) is the delta's outcome, so no feature round follows.
- 2026-11-14 · author — supersedes the round-00 entry above: the acceptance is every java-developer gate green except lint, which the owner runs.
- 2026-11-14 · gate — review: 2 round(s), lane light — blockers in round(s) 1 — 1 finding(s) were damage from a fix pass.
- 2026-11-14 · gate — review: 2 round(s), lane light — blockers in round(s) 1.
