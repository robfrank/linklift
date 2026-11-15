# Delta Specifications: Retire the separate webapp nginx image and deployment

Mechanism only; the observable claim lives in **NF-104**. Each section below replaces the trunk section with the same anchor whole.

## System context {#system-context}

The webapp is a single-page React 19 application (`webapp/src/index.tsx` mounts `App` inside a `BrowserRouter`). It talks to one backend, the LinkLift Java API, only through relative `/api/v1` URLs (**NF-101**). In development webpack-dev-server forwards those to the API process (**NF-102**); in production the bundle is served by the API itself from the API image, so the relative URLs reach it on the same origin (**NF-104**). The backend's behaviour, including how it serves the bundle, is owned by the backend design root (`design/`); this document only names the endpoints the UI calls.

No boundary-level decisions are introduced by this baseline beyond those recorded in [adr/](adr/).

## Routing {#routing}

Routing uses React Router 7 (`react-router-dom`) with a `BrowserRouter`, so paths are real URL paths and the dev server falls back to `index.html` (**NF-102**); in production the fallback for client-side routes is the API's, owned by the backend design root (**NF-104**). Route definitions live only in `webapp/src/App.js` (**RQ-501**–**RQ-503**). Query parameters are read with `URLSearchParams(location.search)`: `view` and `collection` on `/` (**RQ-540**, **RQ-506**), `q` on `/search` (**RQ-593**). `ProtectedRoute` passes `state.from` to `/login`, but `Login.js` ignores it (**RQ-521**).

## Build configuration {#build-config}

`webapp/webpack.config.js` defines entry, output, `babel-loader` for `.js/.jsx/.ts/.tsx`, `style-loader` + `css-loader` for CSS, `HtmlWebpackPlugin` over `public/index.html`, and the dev-server settings (**NF-102**, **NF-103**). A rule disables `fullySpecified` for `.mjs`/`.js` so MUI 9's extensionless ESM imports resolve. `webapp/.babelrc` applies `@babel/preset-env`, `@babel/preset-react` (automatic runtime) and `@babel/preset-typescript` (**NF-103**). `webapp/tsconfig.json` sets `strict` and `noEmit`, but no npm script or CI step runs `tsc` (**NF-103**, **NF-108**).

The webapp defines no image or deployment of its own (**NF-104**): the root `Dockerfile` runs the webapp build and puts its output in the API image, and the API's Kamal deployment (`config/deploy.yml`) ships it; see [operational-guides/build-and-deploy.md](operational-guides/build-and-deploy.md).
