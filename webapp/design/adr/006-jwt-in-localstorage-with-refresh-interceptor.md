# JWT session in localStorage with an axios refresh interceptor

## Status

Accepted (retroactively recorded for the baseline)

## Context

The backend issues an access token and a refresh token at sign-in. The client must keep the user signed in across reloads and recover from an expired access token.

## Decision

`webapp/src/contexts/AuthContext.js` stores both tokens and a user object in `localStorage`, sets the bearer header on the shared axios instance, trusts the stored user on reload, and installs a response interceptor that refreshes once on a 401 and replays the request. See [auth-flow](../specifications.md#auth-flow).

## Consequences

- Sessions survive reloads without a server check (**RQ-525**).
- Tokens are readable by any script on the origin (**NF-105**); HTML sanitisation and the iframe sandbox reduce that exposure (**RQ-602**).
- A stored user is shown as signed in until the first 401, even if the token was revoked (**RQ-525**, **RQ-526**).
- The rendered UI has no sign-out control although `logout()` exists (**RQ-507**).
