# Delta Requirements: Remove the Mergify configuration

## Added

- **NF-RM-001** The repository carries no Mergify configuration, so no Mergify rule merges a pull request and Mergify posts no check for an invalid configuration. An approved `dependabot[bot]` pull request labelled `dependency_approved` is still merged by the workflow `.github/workflows/merge-dependabot.yml`, which stays as it is (its merge body keeps `[skip ci]`); every other approved bot pull request, from `github-actions[bot]` or from `dependabot[bot]` without that label, is merged by a maintainer, like any other pull request.
  - **Locations**: none of the paths Mergify reads exists in the repository: `.mergify.yml`, `.mergify/config.yml` <!-- external-ref --> and `.github/mergify.yml` <!-- external-ref -->.
  - **Tags**: `guide:general`, `ci`
  - **Falsifier**: a change that empties `.mergify.yml` instead of deleting it keeps a Mergify configuration in the repository — ruled out by test row `T-no-mergify-config`.
  - **Falsifier**: a change that also deletes `.github/workflows/merge-dependabot.yml`, treating it as part of the Mergify setup, stops labelled Dependabot pull requests from merging — ruled out by test row `T-ci-untouched`.
