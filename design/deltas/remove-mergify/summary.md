# Remove the Mergify configuration
<!-- footprint: NF-RM-001 add -->

## What changes

The repository's Mergify configuration is deleted. The existing Dependabot auto-merge workflow keeps merging labelled, approved Dependabot pull requests; every other bot pull request is merged by a maintainer.

## Why

Mergify dropped the `commit_message_template` option the configuration used, so the file is invalid and every pull request carries failing Mergify checks. The user no longer wants Mergify; upgrading the configuration (PR #954) was rejected.

## Impact and risk

No application code, CI workflow or Dependabot configuration changes. Bot pull requests the workflow does not merge (any from `github-actions[bot]`, and Dependabot ones without its label) no longer merge on their own, and a maintainer's merge of one runs CI on `main`. Uninstalling the Mergify GitHub App and closing PR #954 are left to the user.

## Changelog

## Notes

- 2026-11-15 · author — no `specifications.md`: the delta adds no source file, function or CLI option, and deletes one configuration file.
- 2026-11-15 · author — `NF-RM-001` names every configuration location Mergify reads, not only the one file deleted, so the requirement is checkable after the change.
- 2026-11-15 · author — the consequence that maintainer-merged bot pull requests now run CI on `main` follows from removing the rule; recorded as impact, not as a new requirement.
- 2026-11-15 · author — amendment 1: `NF-RM-001` now names the kept Dependabot workflow and the label it needs; the existing `T-ci-untouched` row already fails if the workflow is deleted or edited, so no test row was added.
