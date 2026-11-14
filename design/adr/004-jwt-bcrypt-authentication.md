# Stateless JWT access tokens, stored refresh tokens, BCrypt passwords

## Status

Accepted (recorded retroactively in the retrofit baseline).

## Context

The API serves a single-page webapp and must identify the user on every request without server-side sessions.

## Decision

Issue short-lived HMAC-SHA256 access tokens and longer-lived refresh tokens (RQ-016, RQ-019) signed with a secret from the environment (RQ-150). Authenticate each request from the bearer token in a global Javalin before-filter that reloads the user, so a deactivated user loses access (RQ-024). Store refresh tokens in ArcadeDB and make them single-use (RQ-020 to RQ-022). Hash passwords with BCrypt over the password plus a per-user random salt (RQ-014). Authorise routes by permission strings checked in before-filters (RQ-026).

## Consequences

- Access tokens cannot be revoked before they expire; logout revokes nothing (RQ-023).
- The filter does not distinguish token types, so a refresh token also works as a bearer credential (RQ-024).
- Permissions are a constant list rather than the stored roles (RQ-025), which leaves the admin route unreachable (RQ-075).
- Without a configured secret outside development the process does not start (RQ-150).
