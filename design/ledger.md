# Ledger

A manager-readable history of what changed in this system and why: one entry per folded delta, newest first. Notes keep accumulating after the fold via note-delta.py.

## 2026-11-15 · Fix the red backend build (prettier, Testcontainers image, graph 401 test) (`fix-backend-build`)

### What changes

Test sources and formatting only. The Testcontainers tests start the published full ArcadeDB image at the library's version, every JavalinTest-based test app listens on loopback through one shared helper, and the Ask tests are reformatted so the prettier check in CI passes.

### Why

Trunk's backend build is red: CI stops at the prettier check before Maven runs, the Testcontainers tests ask for an image tag that is not published, and the graph controller's unauthenticated test fails in the full suite while passing alone.

### Impact and risk

No application behaviour changes; production code is not touched. The risk is confined to the test suite: a test that relied on the headless image's defaults could behave differently on the full image, which `T-full-suite` would show.

### Changelog

Backend build green again: Testcontainers tests use the full ArcadeDB image at the library version, JavalinTest apps bind to loopback, and the Java sources pass the prettier check.

### Notes

- 2026-11-14 · author — `ArcadeAuthTokenRepositoryTest`, which used the `latest` tag rather than the headless image, is switched to the library version too, because NF-008 as written covers every ArcadeDB container.
- 2026-11-14 · author — the shared helper sets the host on an already-built app (`app.unsafe.jetty.host`) instead of wrapping `Javalin.create`, so `ObservabilityTest`'s `WebBuilder.build()` app is covered without changing production code.
- 2026-11-14 · author — the helper is named `LoopbackJavalin`, not `…Test`, so Surefire's default includes do not treat it as a test class.
- 2026-11-14 · author — NF-FBB-001's CI enforcement is already wired (pre-commit prettier hook in both workflows' `check` job); no workflow file changes.
- 2026-11-14 · author — the "passes" outcome is carried in NF-008 rather than a new requirement, since the intent's footprint lists only NF-008 and NF-FBB-001.
- 2026-11-14 · author — all work sits in round 00: its acceptance (every java-developer gate green) is the delta's outcome, so no feature round follows.
- 2026-11-14 · author — supersedes the round-00 entry above: the acceptance is every java-developer gate green except lint, which the owner runs.
- 2026-11-14 · gate — review: 2 round(s), lane light — blockers in round(s) 1 — 1 finding(s) were damage from a fix pass.
- 2026-11-14 · gate — review: 2 round(s), lane light — blockers in round(s) 1.
- 2026-11-14 · round 00 conductor — Gates green: dep-sync, format-check, type-check, test (513 run, 0 failures, 0 errors, 1 skipped); lint is owner-run per the approved override and was not run.
- 2026-11-14 · round 00 conductor — Three consecutive full runs green (runs 3-5); the graph 401 test passed in all five runs. Run 2 failed once on a DatabaseInitializerTest Testcontainers readiness timeout (90 s startup cap) with the full arcadedata/arcadedb:26.11.1 image, not reproduced in four other runs; follow-up: raise withStartupTimeout on the container declarations (own delta).
- 2026-11-14 · round 00 conductor — The 404 cause was never independently reproduced; the loopback-binding hypothesis is backed by T-loopback-stray-listener.
- 2026-11-14 · round 00 conductor — SecurityIntegrationTest (package adapter.in.web.security) needed an explicit import of LoopbackJavalin; ArcadeAuthTokenRepositoryTest needed a com.arcadedb.Constants import.


### Effort
- implementation rounds: 1
- review rounds: 2
- attempts: 1
- coverage: 64%
- tokens: 1079k
- wall-clock elapsed (includes idle, not a work measure): 11h19m
- main agent: 20k generated over 45 messages
- implementers: 17k generated over 2/2 dispatch(es)

- 2026-11-15 · fold — the design changed DURING implementation: reviewed on the shelf as `954ee2261b9e9d09`, built as `7b0f97cf521c7ae4` (re-reviewed in 2 round(s))
