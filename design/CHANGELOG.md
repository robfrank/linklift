# Changelog

## Unreleased

- The repository no longer uses Mergify, so pull requests stop showing failing Mergify checks. Approved Dependabot pull requests labelled for auto-merge still merge through the existing Dependabot workflow; every other bot pull request is merged by a maintainer, like any other pull request.
- Backend build green again: Testcontainers tests use the full ArcadeDB image at the library version, JavalinTest apps bind to loopback, and the Java sources pass the prettier check.
