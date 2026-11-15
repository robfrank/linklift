# Remove the Mergify configuration
<!-- footprint: NF-RM-001 add -->

## What changes

The repository's Mergify configuration is deleted. The Dependabot merge workflow stays (intent Amendment 1) and keeps merging labelled, approved Dependabot pull requests with `[skip ci]`; every other bot pull request is merged by a maintainer.

## Why

Mergify dropped the `commit_message_template` option the configuration used, so the file is invalid and every pull request carries failing Mergify checks. The user no longer wants Mergify; upgrading the configuration (PR #954) was rejected.

## Impact and risk

No application code, CI workflow or Dependabot configuration changes. Bot pull requests the workflow does not merge (any from `github-actions[bot]`, and Dependabot ones without its label) no longer merge on their own, and a maintainer's merge of one runs CI on `main`. Uninstalling the Mergify GitHub App and closing PR #954 are left to the user.

## Changelog

The repository no longer uses Mergify, so pull requests stop showing failing Mergify checks. Approved Dependabot pull requests labelled for auto-merge still merge through the existing Dependabot workflow; every other bot pull request is merged by a maintainer, like any other pull request.

## Notes

- 2026-11-15 · author — no `specifications.md`: the delta adds no source file, function or CLI option, and deletes one configuration file.
- 2026-11-15 · author — `NF-RM-001` names every configuration location Mergify reads, not only the one file deleted, so the requirement is checkable after the change.
- 2026-11-15 · author — the consequence that bot merges now run CI on `main` (no `[skip ci]`) follows from removing the rule; recorded as impact, not as a new requirement.
- 2026-11-15 · fix — supersedes the entry above after intent Amendment 1: only maintainer-merged bot pull requests run CI on `main`; labelled, approved Dependabot pull requests keep merging through `.github/workflows/merge-dependabot.yml` with `[skip ci]`.
