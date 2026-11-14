# Webpack with Babel, no type-check in the build

## Status

Accepted (retroactively recorded for the baseline)

## Context

The codebase mixes JavaScript (older components) and TypeScript (newer layers). It needs a dev server with an API proxy and a static production bundle.

## Decision

`webapp/webpack.config.js` bundles `src/index.tsx` with `babel-loader`; `webapp/.babelrc` strips TypeScript types with `@babel/preset-typescript`. `webapp/tsconfig.json` sets `noEmit` and serves editors only; no script runs `tsc`. See [build-config](../specifications.md#build-config).

## Consequences

- JavaScript and TypeScript files compile through one pipeline (**NF-103**).
- Type errors never fail a build or CI run (**NF-103**, **NF-108**); `AddLinkPage.tsx` and the TypeScript `ContentViewerModal.tsx` use `// @ts-ignore` around imports of untyped JavaScript modules.
- The bundle file name carries no content hash (**NF-103**).
