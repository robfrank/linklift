# Delta Requirements: Remove the Mergify configuration

## Added

- **NF-RM-001** The repository carries no Mergify configuration, so no Mergify rule merges a pull request and Mergify posts no check for an invalid configuration; an approved pull request from `dependabot[bot]` or `github-actions[bot]` is merged by a maintainer, like any other pull request.
  - **Locations**: none of the paths Mergify reads exists in the repository: `.mergify.yml`, `.mergify/config.yml` <!-- external-ref --> and `.github/mergify.yml` <!-- external-ref -->.
  - **Tags**: `guide:general`, `ci`
  - **Falsifier**: a change that empties `.mergify.yml` instead of deleting it keeps a Mergify configuration in the repository — ruled out by test row `T-no-mergify-config`.
