# Remove the Mergify configuration
<!-- footprint: NF-RM-001 add -->

## What changes

The repository's Mergify configuration is deleted. Bot pull requests from Dependabot and `github-actions[bot]` are merged by a maintainer from now on.

## Why

Mergify dropped the `commit_message_template` option the configuration used, so the file is invalid and every pull request carries failing Mergify checks. The user no longer wants Mergify; upgrading the configuration (PR #954) was rejected.

## Impact and risk

No application code, CI workflow or Dependabot configuration changes. Approved bot pull requests no longer merge on their own, and their merge commits no longer carry `[skip ci]`, so merging one runs CI on `main`. Uninstalling the Mergify GitHub App and closing PR #954 are left to the user.

## Changelog

## Notes

- 2026-11-15 · author — no `specifications.md`: the delta adds no source file, function or CLI option, and deletes one configuration file.
- 2026-11-15 · author — `NF-RM-001` names every configuration location Mergify reads, not only the one file deleted, so the requirement is checkable after the change.
- 2026-11-15 · author — the consequence that bot merges now run CI on `main` (no `[skip ci]`) follows from removing the rule; recorded as impact, not as a new requirement.
