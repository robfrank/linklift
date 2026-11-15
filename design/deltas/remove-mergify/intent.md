# Remove the Mergify configuration
<!-- intent: confirmed 2026-11-15 sha256:6429a772cb5f48f1 -->

## Problem
The repository carries a `.mergify.yml` whose one rule merges approved Dependabot and `github-actions[bot]` PRs with a `[skip ci]` message. Mergify has removed `commit_message_template`, so the configuration is now invalid and every PR shows two failing Mergify checks (`Mergify Merge Queue`, `Summary`). The user does not want Mergify any more.

## Outcome
- The repository has no Mergify configuration.
- Approved Dependabot and `github-actions[bot]` PRs are merged by a maintainer, like any other PR.
- Future PRs no longer get failing Mergify checks from an invalid configuration.

## Baseline and rules
Trunk `design/` at the current head. Rules: none (a configuration-file deletion; no developer rule fits). Standards: the repo's `CLAUDE.md`.

## Footprint
**Adds or changes:** the repository carries no Mergify configuration and bot PRs are merged by a maintainer (NF-RM-001, new).
**Relies on:** none.
**Out of scope:** uninstalling the Mergify GitHub App and closing PR #954 (both outside the repository, done by the user); any other merge automation.

## Constraints and quality
No change to application code, CI workflows or Dependabot configuration.

## Priority and lane
Priority medium. Light lane.

## Decisions
- Remove Mergify entirely — upgrading the configuration to `commit_message_format` (PR #954) rejected: the user wants no Mergify · stated
- No developer rule — defining one rejected: the change is a single YAML file deletion with no build or test step · accepted

## Open questions
None.

## Amendments
