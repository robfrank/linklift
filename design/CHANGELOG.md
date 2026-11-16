# Changelog

## Unreleased

- The API container now serves the React app at the site root, and app routes fall back to the app's index page, also after a refresh. The API answers exactly as before. One Kamal deploy ships the app and the API together at linklift.arcadebrain.ai. Without a bundle, the server behaves as it did before.
- The repository no longer uses Mergify, so pull requests stop showing failing Mergify checks. Approved Dependabot pull requests labelled for auto-merge still merge through the existing Dependabot workflow; every other bot pull request is merged by a maintainer, like any other pull request.
- Backend build green again: Testcontainers tests use the full ArcadeDB image at the library version, JavalinTest apps bind to loopback, and the Java sources pass the prettier check.
