# Delta Requirements: Remove the Mergify configuration

## Added

- **NF-RM-001** The repository carries no Mergify configuration: none of `.mergify.yml`, `.mergify/config.yml` <!-- external-ref --> or `.github/mergify.yml` <!-- external-ref --> exists at the repository root, so no Mergify rule merges a pull request and Mergify posts no check for an invalid configuration; an approved pull request from `dependabot[bot]` or `github-actions[bot]` is merged by a maintainer, like any other pull request.
  - **Tags**: `guide:general`, `ci`
  - **Falsifier**: a change that empties `.mergify.yml` instead of deleting it keeps a Mergify configuration in the repository — ruled out by test row `T-no-mergify-config`.
