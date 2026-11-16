# Changelog

## Unreleased

- The webapp no longer has its own nginx image or Kamal deployment: its production bundle now ships inside the API container and is deployed with the API. The local Docker Compose stack no longer starts a separate webapp container; run the UI locally with the webpack dev server next to the API.
- Webapp tests run again on Babel 8: Jest loads the project's own `babel-jest`, `react-dom` matches `react`, CI runs on Node 24, and the TypeScript config is accepted by TypeScript 7.
