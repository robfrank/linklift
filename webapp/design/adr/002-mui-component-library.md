# Material UI as the component and theming library

## Status

Accepted (retroactively recorded for the baseline)

## Context

The webapp needs form controls, dialogs, navigation, notifications and a consistent look without a design system of its own.

## Decision

Pages use Material UI (`@mui/material`, `@mui/icons-material`, major version 9 per `webapp/package.json`) with Emotion. Styling is the `sx` prop, `styled()` and one `createTheme` call in `webapp/src/App.js`; plain CSS remains only for globals (`webapp/src/index.css`) and the content modal. See [theming](../specifications.md#theming).

## Consequences

- One light theme for the whole UI (**RQ-641**).
- MUI 9 ships extensionless ESM imports, which required a webpack `fullySpecified: false` rule ([build-config](../specifications.md#build-config)).
- The content modal is a hand-built overlay rather than MUI's `Dialog`, so it does not get MUI's dialog focus handling.
