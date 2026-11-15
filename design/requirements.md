# Baseline Requirements

Retrofit baseline of the LinkLift Java backend as built (Maven module at the repository root). Every requirement below states behaviour the code has today; where the code and an older document disagree, the code was taken as the truth (see `implementation.md#relationship-to-legacy-documents`). Frontend requirements live in the sibling `webapp/design/` root (RQ-501+, NF-101+).

## Runtime and health

- **RQ-001** Started through `it.robfrank.linklift.Application.main`, the API server listens for HTTP on port `7070`.
  - **Tags**: `api`, `config`
- **RQ-002** `GET /up` answers HTTP 200 with an empty body and needs no credentials.
  - **Tags**: `api`, `observability`, `guide:deploy`
- **RQ-003** `GET /` answers HTTP 200 with the plain-text body `LinkLift` and needs no credentials.
  - **Tags**: `api`
- **RQ-004** `GET /metrics` answers HTTP 200 with a Prometheus text exposition that includes JVM memory, garbage-collection, thread, class-loader and processor meters, and needs no credentials.
  - **Tags**: `api`, `observability`
- **RQ-005** Every route answers CORS requests from any origin.
  - **Tags**: `api`

## Authentication and authorization

- **RQ-010** `POST /api/v1/auth/register` with a JSON body `{username, email, password, firstName?, lastName?}` creates an active user and answers 201 with `{id, username, email, firstName, lastName, message}` where `message` is `User registered successfully`; the response carries no password hash or salt.
  - **Tags**: `api`, `auth`
- **RQ-011** Registration trims and lower-cases `username` and `email` before validating and storing them, and trims `firstName` and `lastName`.
  - **Tags**: `auth`
- **RQ-012** Registration answers 400 with error code `1001` when the username does not match `^[a-zA-Z0-9_]{3,30}$`, when the email does not match `^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$`, when the password is shorter than 8 or longer than 128 characters or contains fewer than three of the four classes upper-case, lower-case, digit, special character, or when `firstName` or `lastName` is present but empty after trimming or longer than 50 characters (these name errors carry no field errors). A null or blank `username`, `email` or `password` is rejected while the request body is deserialised (`IllegalArgumentException` in `CreateUserCommand`), before these checks.
  - **Tags**: `api`, `auth`
- **RQ-013** Registration with a username or an email that already exists answers 409 with error code `2101`.
  - **Tags**: `api`, `auth`
- **RQ-014** A stored password is the BCrypt hash (cost 12) of the password concatenated with a per-user random 32-byte salt (Base64); the hash and the salt are stored on the User record.
  - **Tags**: `auth`, `security`
- **RQ-015** `POST /api/v1/auth/login` with `{loginIdentifier, password, rememberMe}` looks the user up by email when the lower-cased identifier contains `@` and by username otherwise, and on success answers 200 with `{userId, username, email, firstName, lastName, accessToken, refreshToken, accessTokenExpiresIn, refreshTokenExpiresIn, message}` where `accessTokenExpiresIn` is `900` and `message` is `Login successful`.
  - **Tags**: `api`, `auth`
- **RQ-016** A login's `refreshTokenExpiresIn` is `604800` (7 days) when `rememberMe` is false and `2592000` (30 days) when it is true; the issued refresh token expires at that offset and the access token 15 minutes after issue.
  - **Tags**: `auth`
- **RQ-017** Login with an unknown identifier or a wrong password answers 401 with error code `2200`; login with the right password for an inactive user answers 401 with error code `2105`.
  - **Tags**: `api`, `auth`
- **RQ-018** A successful login sets the user's `lastLoginAt` and publishes a `UserAuthenticatedEvent`.
  - **Tags**: `auth`, `events`
- **RQ-019** Access and refresh tokens are HMAC-SHA256 JWTs with issuer `linklift`, subject the user id, and claims `username`, `email`, `firstName`, `lastName`, `token_type` (`access` or `refresh`), a random `nonce`, issued-at and expiry.
  - **Tags**: `auth`, `security`
- **RQ-020** Every issued refresh token is stored as an AuthToken of type `REFRESH` with its expiry, the client IP (first `X-Forwarded-For` entry, else `X-Real-IP`, else the socket address) and the `User-Agent` header.
  - **Tags**: `auth`
- **RQ-021** `POST /api/v1/auth/refresh` with `{refreshToken}` whose JWT verifies and whose stored AuthToken is of type `REFRESH`, not revoked, not used and not expired, for an active user, marks the stored token used and answers 200 with a new token pair in the RQ-015 shape, `refreshTokenExpiresIn` `604800` and `message` `Token refreshed successfully`; it publishes a `TokenRefreshedEvent`.
  - **Tags**: `api`, `auth`, `events`
- **RQ-022** A refresh request whose token fails any RQ-021 check, including a token already used once, answers 401 with error code `2202`; a valid token of an inactive user answers 401 with error code `2105`.
  - **Tags**: `api`, `auth`
- **RQ-023** `POST /api/v1/auth/logout` answers 200 with `{"message":"Logged out successfully"}` and revokes no token.
  - **Tags**: `api`, `auth`
- **RQ-024** For every request, a bearer token from `Authorization: Bearer <token>` that verifies (signature, issuer, expiry) and whose subject is an existing active user makes the request authenticated as that user; any other request, including one with a missing, malformed, invalid or expired token, proceeds as anonymous without an error from the filter itself. The `token_type` claim is not checked, so a valid refresh token also authenticates a request.
  - **Tags**: `auth`, `security`
- **RQ-025** Every authenticated user holds exactly the permissions `CREATE_LINK`, `CREATE_COLLECTION`, `UPDATE_OWN_COLLECTION`, `DELETE_OWN_COLLECTION`, `READ_OWN_LINKS`, `UPDATE_OWN_LINKS`, `DELETE_OWN_LINKS`; the Role vertices and `HasRole` edges in the database are not consulted.
  - **Tags**: `auth`, `security`
- **RQ-026** An anonymous request to any `/api/v1/*` route other than the four `/api/v1/auth/*` routes answers 401 with error code `2204`; an authenticated request lacking a route's required permission answers 403 with error code `2205`.
  - **Tags**: `api`, `auth`, `errors`

## Links

- **RQ-030** `PUT /api/v1/link` (permission `CREATE_LINK`) with `{url, title?, description?}` stores a new link owned by the caller and answers 201 with `{link, status}` where `status` is `Link received`; the link has a new UUID `id`, `contentType` `text/html`, `readStatus` `UNREAD`, `archived` false, `favorited` false, empty `extractedUrls` and `extractedAt` set to the creation time.
  - **Tags**: `api`, `links`
- **RQ-031** `PUT /api/v1/link` with an empty or missing `url` answers 400 with Javalin's validation body (key `REQUEST_BODY`, message `Url cannot be empty`), not an ErrorResponse.
  - **Tags**: `api`, `links`, `errors`
- **RQ-032** `PUT /api/v1/link` with a `url` that is not a syntactically valid URI answers 400 with error code `1001` and field error `url`; a syntactically valid but relative URI (for example `example`) answers 500 with error code `1000`.
  - **Tags**: `api`, `links`, `errors`
- **RQ-033** `PUT /api/v1/link` with a `url` already stored for any user answers 409 with error code `2001`.
  - **Tags**: `api`, `links`
- **RQ-034** Creating a link also creates an `OwnsLink` edge from the caller's User, upserts a `Domain` vertex named after the URL host with a leading `www.` removed, links it with a `belongsToDomain` edge, and publishes a `LinkCreatedEvent`, which starts the content download of RQ-050 before the response is sent.
  - **Tags**: `links`, `graph`, `events`
- **RQ-035** `GET /api/v1/links` (permission `READ_OWN_LINKS`) answers 200 with `{data, message}` where `message` is `Links retrieved successfully` and `data` is a page `{content, page, size, totalElements, totalPages, hasNext, hasPrevious}` containing only links the caller owns.
  - **Tags**: `api`, `links`
- **RQ-036** Link listing defaults to `page` 0, `size` 20, `sortBy` `extractedAt` and `sortDirection` `DESC`.
  - **Tags**: `links`
- **RQ-037** Link listing answers 400 with error code `1001` and one field error per offending parameter when `page` is negative, `size` is outside 1..100, `sortBy` is not one of `id`, `url`, `title`, `description`, `extractedAt`, `contentType`, or `sortDirection` is not `ASC` or `DESC` (case-insensitive).
  - **Tags**: `api`, `links`, `errors`
- **RQ-038** Link listing accepts the optional filters `readStatus` (`UNREAD`, `READING`, `READ`, case-insensitive), `archived` and `favorited` (the value `true`, case-insensitive, means true and any other value false) and `tagId` (links carrying a `HasTag` edge to that tag), combined with AND; `totalElements` counts the filtered set. An unknown `readStatus` answers 400 with the plain-text body `Invalid read status: <value>`.
  - **Tags**: `api`, `links`, `tags`
- **RQ-039** In a link page, `totalPages` is `ceil(totalElements / size)`, `hasNext` is `page < totalPages - 1` and `hasPrevious` is `page > 0`.
  - **Tags**: `links`
- **RQ-040** Every successful link listing publishes a `LinksQueryEvent` carrying the query and `totalElements`.
  - **Tags**: `links`, `events`
- **RQ-041** `PATCH /api/v1/links/{id}` (permission `UPDATE_OWN_LINKS`) with `{title?, description?}` replaces each non-null field of a link the caller owns and answers 200 with `{data}` holding the updated link; all other link fields are unchanged.
  - **Tags**: `api`, `links`
- **RQ-042** `PATCH /api/v1/links/{id}/status` (permission `UPDATE_OWN_LINKS`) with `{readStatus?, archived?, favorited?}` replaces each non-null field of a link the caller owns and answers 200 with `{data}` holding the updated link; an unknown `readStatus` answers 400 with the plain-text body `Invalid read status: <value>`.
  - **Tags**: `api`, `links`
- **RQ-043** `PATCH /api/v1/links/{id}`, `PATCH /api/v1/links/{id}/status` and `DELETE /api/v1/links/{id}` on a link that does not exist or that the caller does not own answer 404 with error code `2000`.
  - **Tags**: `api`, `links`, `errors`
- **RQ-044** `DELETE /api/v1/links/{id}` (permission `DELETE_OWN_LINKS`) deletes a link the caller owns and answers 204.
  - **Tags**: `api`, `links`
- **RQ-045** `GET /api/v1/graph` (permission `READ_OWN_LINKS`) answers 200 with `{data: {nodes, edges}, message}` where `message` is `Graph data retrieved successfully`, `nodes` has one `{id, label, url}` per link the caller owns (`label` is the title, or the URL when the title is null) and `edges` has one `{source, target}` per `linksTo` edge whose both ends the caller owns.
  - **Tags**: `api`, `graph`

## Content download and extraction

- **RQ-050** A content download fetches the link URL with HTTP GET, header `User-Agent: LinkLift/1.0 (Content Extractor)`, a 30-second request timeout and a 10-second connect timeout, and publishes a `ContentDownloadStartedEvent` when it starts.
  - **Tags**: `content`, `events`
- **RQ-051** The HTTP downloader makes up to 3 attempts in total for a 5xx response or a network I/O error and fails at once on any other non-2xx status; the download service retries any failed download up to 3 more times, 1 second apart.
  - **Tags**: `content`
- **RQ-052** A downloaded body larger than 10485760 bytes (10 MiB) is treated as a failed download.
  - **Tags**: `content`
- **RQ-053** A successful download stores one Content record with status `COMPLETED`, the raw HTML, the body text, the byte length, the `Content-Type` header (default `text/html`) as `mimeType`, the download time and the extracted metadata of RQ-054, then creates a `HasContent` edge from the Link and publishes a `ContentDownloadCompletedEvent`.
  - **Tags**: `content`, `events`
- **RQ-054** Metadata extraction takes the title from `og:title`, else `twitter:title`, else `article:title`, else `<title>`; the description from `og:description`, else `twitter:description`, else `meta[name=description]`; the author from `article:author`, else `meta[name=author]`, else the `content` attribute of `[itemprop=author]`, else the text of `a[rel=author]`; the published date from `article:published_time`, else `[itemprop=datePublished]`, else `time[datetime]`, parsed as an ISO date-time or ISO date (else null); and the hero image from `og:image`, else `twitter:image`, else the first `article img`, `main img` or `img[src]`, resolved to an absolute URL.
  - **Tags**: `content`
- **RQ-055** The stored `summary` is an extractive summary of the extracted main text, at most 500 characters: text of 500 characters or fewer is used whole; longer text yields up to five highest-scoring sentences (word-frequency score normalised by sentence length) in their original order, stopping before the limit would be exceeded, or the first sentence when none fit.
  - **Tags**: `content`, `ai`
- **RQ-056** A download collects up to 200 distinct anchor URLs from the page (each `a[href]` resolved to an absolute URL and kept when it starts with `http`); when at least one is collected, it replaces the link's outgoing `linksTo` edges with one edge to every stored Link whose `url` equals one of them. The collected URLs are not persisted on the Link (its stored `extractedUrls` stays as written at creation), and a download that collects none leaves the existing `linksTo` edges unchanged.
  - **Tags**: `content`, `graph`
- **RQ-057** A download generates an embedding of the extracted main text and stores it on the Content record; when embedding generation fails the Content record is still stored, without an embedding.
  - **Tags**: `content`, `search`, `ai`
- **RQ-058** A failed download publishes a `ContentDownloadFailedEvent` with the error message and attempts to store a Content record with status `FAILED`.
  - **Tags**: `content`, `events`
- **RQ-059** `GET /api/v1/links/{linkId}/content` (permission `READ_OWN_LINKS`) answers 200 with `{data, message}` where `data` is the full Content record (including `htmlContent` and `embedding`) and `message` is `Content retrieved successfully`; when no Content record exists it answers 404 with error code `2300`.
  - **Tags**: `api`, `content`
- **RQ-060** `POST /api/v1/links/{linkId}/content/refresh` (permission `UPDATE_OWN_LINKS`) starts a new content download for the link and answers 202 with `{"message":"Content refresh triggered"}`; for an unknown link id it answers 404 with error code `2000`.
  - **Tags**: `api`, `content`
- **RQ-061** `DELETE /api/v1/links/{linkId}/content` (permission `DELETE_OWN_LINKS`) deletes the Content records of that link id and answers 204.
  - **Tags**: `api`, `content`
- **RQ-062** The three content routes of RQ-059 to RQ-061 do not check that the caller owns the link: any authenticated caller holding the route permission can read, refresh or delete the content of any link id.
  - **Tags**: `content`, `security`
- **RQ-063** A Link in a JSON response has exactly the fields `id`, `url`, `title`, `description`, `extractedAt`, `contentType`, `extractedUrls`, `readStatus`, `archived`, `favorited`, with `readStatus` one of `UNREAD`, `READING`, `READ`.
  - **Tags**: `api`, `links`
- **RQ-064** A Content record in a JSON response (RQ-059, RQ-073) has exactly the fields `id`, `linkId`, `htmlContent`, `textContent`, `contentLength`, `downloadedAt`, `mimeType`, `status`, `summary`, `heroImageUrl`, `extractedTitle`, `extractedDescription`, `author`, `publishedDate`, `embedding`, with `status` one of `PENDING`, `IN_PROGRESS`, `COMPLETED`, `FAILED`.
  - **Tags**: `api`, `content`

## Embeddings and semantic search

- **RQ-070** An embedding is requested with `POST {LINKLIFT_OLLAMA_URL}/api/embeddings` and body `{"model", "prompt"}`, where the prompt is the first 512 characters of the text; a non-200 status, unparseable JSON, or a missing or non-list `embedding` field raises an error.
  - **Tags**: `search`, `ai`
- **RQ-071** When the first embedding returned by the running process has a length different from `LINKLIFT_OLLAMA_DIMENSIONS`, a warning is logged and the embedding is still used.
  - **Tags**: `search`, `ai`, `config`
- **RQ-072** A Content record stored without an embedding carries a placeholder vector of 384 zeros and `needsEmbedding` true; an all-zero stored vector reads back as no embedding.
  - **Tags**: `search`, `schema`
- **RQ-073** `GET /api/v1/search?q=<text>&limit=<n>` (permission `READ_OWN_LINKS`) answers 200 with a JSON array of at most `limit` Content records of links the caller owns, ordered by vector similarity to the embedding of `q`; `limit` defaults to 10, is capped at 1000, and a value of 0 or less yields an empty array.
  - **Tags**: `api`, `search`
- **RQ-074** `GET /api/v1/search` with a missing or blank `q` answers 400 with the plain-text body `Search query cannot be empty`; when the embedding of `q` cannot be generated it answers 500 with error code `1000`.
  - **Tags**: `api`, `search`, `errors`
- **RQ-075** `POST /api/v1/admin/backfill-embeddings` requires the permission `ADMIN_ACCESS`; because no user holds it (RQ-025), every authenticated caller receives 403 with error code `2205`.
  - **Tags**: `api`, `search`, `auth`
- **RQ-076** The embedding backfill, when invoked, repeatedly loads up to 100 Content records with `needsEmbedding` true and non-null text, stores a generated embedding on each with non-blank text, skips for the rest of the run any record whose embedding generation failed, and ignores a second invocation while a run is in progress.
  - **Tags**: `search`, `ai`

## Question answering

- **RQ-080** `POST /api/v1/ask` (permission `READ_OWN_LINKS`) with a missing or blank `question` answers 400 with error code `1001` and field error `question`; a question longer than 2000 characters answers 400 with error code `1001`.
  - **Tags**: `api`, `ai`
- **RQ-081** `POST /api/v1/ask` retrieves the 5 Content records of caller-owned links most similar to the question's embedding; when there are none it answers 200 with `{question, answer, sources: []}` where `answer` is `I could not find any relevant content in your saved links to answer this question.`
  - **Tags**: `api`, `ai`, `search`
- **RQ-082** Otherwise `POST /api/v1/ask` sends `POST {LINKLIFT_OLLAMA_URL}/api/generate` with `{model: LINKLIFT_OLLAMA_MODEL, prompt, stream: false}`, a 60-second timeout and a source context truncated to 4000 characters, and answers 200 with `{question, answer, sources}`, one source `{linkId, title, url, excerpt}` per retrieved content whose link loads, where `title` is the link title or its URL and `excerpt` is the first 300 characters of the text (with `...` appended when longer) or null when the text is empty.
  - **Tags**: `api`, `ai`
- **RQ-083** When the generation call answers non-200 or lacks a `response` field, `POST /api/v1/ask` still answers 200 with `answer` `I was unable to generate an answer at this time.`; on a network error the answer is `I was unable to generate an answer due to a network error.` (also when the call is interrupted); when the request or the response JSON cannot be processed the answer is `I was unable to generate an answer due to a request error.`
  - **Tags**: `api`, `ai`

## Related links

- **RQ-090** `GET /api/v1/links/{linkId}/related` (permission `READ_OWN_LINKS`) answers 200 with `{data}` holding a list of links: when the link's Content has an embedding and its 10 most similar caller-owned Content records include links other than itself, those links; otherwise up to 10 caller-owned links, other than the link itself, reached from it through a shared `belongsToDomain` Domain or a shared `hasTag` edge.
  - **Tags**: `api`, `graph`, `search`

## Collections

- **RQ-100** `POST /api/v1/collections` with `{name, description?, query?}` creates a collection owned by the caller and answers 201 with `{data}` holding `{id, name, description, userId, query, summary}` with `summary` null; a blank `name` answers 400 with error code `1001`.
  - **Tags**: `api`, `collections`
- **RQ-101** `GET /api/v1/collections` answers 200 with `{data}` holding the caller's collections ordered by name ascending.
  - **Tags**: `api`, `collections`
- **RQ-102** `GET /api/v1/collections/{id}` answers 200 with `{data: {collection, links}}`, the links reached through `ContainsLink` edges ordered by `extractedAt` descending.
  - **Tags**: `api`, `collections`
- **RQ-103** `POST /api/v1/collections/{id}/links` with `{linkId}` creates a `ContainsLink` edge from the collection to the link and answers 204, without checking that the link exists or that the caller owns it.
  - **Tags**: `api`, `collections`
- **RQ-104** `DELETE /api/v1/collections/{id}/links/{linkId}` removes that `ContainsLink` edge and answers 204; `DELETE /api/v1/collections/{id}` deletes the collection and answers 204.
  - **Tags**: `api`, `collections`
- **RQ-105** Every collection route that names a collection id answers 404 with error code `2400` when the collection does not exist and 401 with error code `2500` when another user owns it.
  - **Tags**: `api`, `collections`, `errors`
- **RQ-106** A collection's `query` is stored and returned but never evaluated, and no route merges, renames or summarises collections.
  - **Tags**: `collections`

## Notes

- **RQ-110** `GET /api/v1/links/{linkId}/notes` answers 200 with a JSON array of the caller's notes on that link ordered by `createdAt` ascending, each `{id, linkId, content, createdAt, updatedAt}` without the owner id.
  - **Tags**: `api`, `notes`
- **RQ-111** `POST /api/v1/links/{linkId}/notes` with `{content}` creates a note and answers 201 with it; when the caller does not own the link it answers 404 with error code `2000`; blank content or content longer than 10000 characters answers 400 with error code `1001`.
  - **Tags**: `api`, `notes`
- **RQ-112** `PATCH /api/v1/links/{linkId}/notes/{noteId}` with `{content}` replaces the content, sets `updatedAt` and answers 200 with the note; `DELETE` on the same path deletes it and answers 204; both answer 404 with error code `2600` when the note does not exist or the caller does not own it, and PATCH applies the RQ-111 content limits.
  - **Tags**: `api`, `notes`
- **RQ-113** Note and tag routes require an authenticated caller and no permission.
  - **Tags**: `notes`, `tags`, `auth`

## Tags

- **RQ-120** `GET /api/v1/tags` answers 200 with a JSON array of the caller's tags ordered by name ascending, each `{id, name, createdAt}` without the owner id.
  - **Tags**: `api`, `tags`
- **RQ-121** `POST /api/v1/tags` with `{name}` answers 201 with a tag whose name is lower-cased and stripped of surrounding whitespace; when the caller already has a tag of that normalised name, that existing tag is returned instead of a new one; a blank name or one longer than 100 characters answers 400 with error code `1001`.
  - **Tags**: `api`, `tags`
- **RQ-122** `DELETE /api/v1/tags/{tagId}` deletes the tag and its `HasTag` edges and answers 204; an unknown tag answers 404 with error code `2700`; another user's tag answers 401 with error code `2204`.
  - **Tags**: `api`, `tags`
- **RQ-123** `GET /api/v1/links/{linkId}/tags` answers 200 with the caller's tags attached to the link, ordered by name ascending.
  - **Tags**: `api`, `tags`
- **RQ-124** `POST /api/v1/links/{linkId}/tags` with `{tagId}` attaches the tag to the link and answers 204, creating no second edge when it is already attached; `DELETE /api/v1/links/{linkId}/tags/{tagId}` detaches it and answers 204. Both answer 404 with error code `2000` when the caller does not own the link, 404 with error code `2700` for an unknown tag, and 401 with error code `2204` for another user's tag.
  - **Tags**: `api`, `tags`
- **RQ-125** `GET /api/v1/links/{linkId}/tags/suggest` answers 200 with up to 10 tags: when the link has no embedding, the caller's tags in name order; otherwise the caller's tags attached to the links of the 6 caller-owned Content records most similar to the link, excluding the link itself and tags already on it, without duplicates; any internal failure yields an empty array.
  - **Tags**: `api`, `tags`, `search`

## Error responses

- **RQ-130** Error responses produced by the global exception handler are JSON `{status, code, message, fieldErrors, path, timestamp}` with null fields omitted, where `status` repeats the HTTP status and `code` is the numeric error code.
  - **Tags**: `api`, `errors`
- **RQ-131** The handler maps link, note and content not-found errors to 404; link-already-exists and user-already-exists to 409; validation errors to 400 with their field errors; authentication errors to 403 for code `2205` and 401 otherwise; content-download errors to 500; database errors to 500 with message `Database operation failed`; any other LinkLift error to 404 for codes `2000`, `2100`, `2300`, `2400`, `2600`, `2700`, to 401 for `2204` and `2500`, to 403 for `2205` and to 500 otherwise; and any other exception to 500 with code `1000` and message `An unexpected error occurred`.
  - **Tags**: `errors`
- **RQ-132** The error codes are: `1000` internal error, `1001` validation error, `2000` link not found, `2001` link already exists, `2002` invalid link URL, `2100` user not found, `2101` user already exists, `2102` invalid username, `2103` invalid email, `2104` weak password, `2105` user inactive, `2200` invalid credentials, `2201` token expired, `2202` token invalid, `2203` token revoked, `2204` unauthorized access, `2205` insufficient permissions, `2300` content not found, `2301` content download failed, `2302` content too large, `2400` collection not found, `2401` collection already exists, `2500` unauthorized, `2600` note not found, `2700` tag not found, `2701` tag already exists, `3000` database error, `3001` network error.
  - **Tags**: `errors`

## Domain events

- **RQ-140** A published domain event is delivered synchronously, on the publishing thread, to every subscriber registered for the event's class, in registration order.
  - **Tags**: `events`
- **RQ-141** At startup the application subscribes a handler to `LinkCreatedEvent` that logs and starts the content download, and logging-only handlers to `LinksQueryEvent`, `UserCreatedEvent`, `UserAuthenticatedEvent`, `TokenRefreshedEvent`, `ContentDownloadStartedEvent`, `ContentDownloadCompletedEvent` and `ContentDownloadFailedEvent`.
  - **Tags**: `events`
- **RQ-142** Successful registration publishes a `UserCreatedEvent` with the user id, username and email.
  - **Tags**: `events`, `auth`

## Configuration

- **RQ-150** The JWT signing secret is the value of `LINKLIFT_JWT_SECRET` when it is at least 32 UTF-8 bytes after trimming; else the trimmed contents of the file named by `LINKLIFT_JWT_SECRET_FILE` when at least 32 bytes; else, in a development environment, a secret generated at process start; else startup fails with an `IllegalStateException`.
  - **Tags**: `config`, `auth`, `guide:configuration`
- **RQ-151** The process counts as a development environment when `ENVIRONMENT` is set and contains `dev` or `local` (case-insensitive), or, when `ENVIRONMENT` is unset, when the system property `spring.profiles.active` contains `dev` or `local` or the class path contains `target/classes` or `target/test-classes`.
  - **Tags**: `config`, `guide:configuration`
- **RQ-152** `LINKLIFT_OLLAMA_URL` defaults to `http://localhost:11434`; `LINKLIFT_OLLAMA_MODEL` defaults to `all-minilm:l6-v2` and names the model for both embeddings and answer generation; `LINKLIFT_OLLAMA_DIMENSIONS` defaults to `384` and a non-integer value falls back to `384` with a warning.
  - **Tags**: `config`, `ai`, `guide:configuration`
- **RQ-153** The ArcadeDB host is the JVM system property `linklift.arcadedb.host` (default `localhost`); the port `2480`, user `root`, password `playwithdata` and database name `linklift` are fixed in code.
  - **Tags**: `config`, `schema`, `guide:configuration`

## Database schema

- **RQ-160** At startup the application creates the database `linklift` when it does not exist and then applies every file under the class-path directory `/schema`, in file-name order, each as one `sqlscript` command inside a transaction.
  - **Tags**: `schema`, `guide:local-run`
- **RQ-161** The applied schema defines the vertex types `Link`, `User`, `AuthToken`, `Role`, `Content`, `Collection`, `Domain`, `Tag`, `Note` and the edge types `HasToken`, `OwnsLink`, `HasRole`, `HasContent`, `belongsToDomain`, `hasTag`, `inCollection`, `ContainsLink`, `linksTo`, `HasTag`.
  - **Tags**: `schema`
- **RQ-162** The schema declares UNIQUE indexes on `Link(id)`, `Link(url)`, `User(id)`, `User(username)`, `User(email)`, `AuthToken(id)`, `AuthToken(token)`, `Role(id)`, `Role(name)`, `Content(id)`, `Content(linkId)`, `Collection(id)`, `Domain(name)`, `Tag(name)`, `Tag(id)`, `Tag(name, userId)` and `Note(id)`.
  - **Tags**: `schema`
- **RQ-163** The schema declares an `LSM_VECTOR` index on `Content(embedding)` with 384 dimensions, `COSINE` similarity, `maxConnections` 16 and `beamWidth` 100.
  - **Tags**: `schema`, `search`
- **RQ-164** The schema scripts insert the roles `USER`, `ADMIN` and `MODERATOR` and an inactive user `system_user` with a `HasRole` edge to `USER`.
  - **Tags**: `schema`, `auth`

## Non-functional

- **NF-001** `mvn package` compiles for Java release 25 and produces a shaded executable JAR whose main class is `it.robfrank.linklift.Application`.
  - **Tags**: `build`, `guide:local-run`
- **NF-002** Classes under `application/` import no class from the `adapter` or `config` packages, with one exception: `NewLinkService` imports `LinkPersistenceAdapter`.
  - **Tags**: `architecture`
- **NF-003** Every service, adapter and controller is constructed explicitly in `Application.start`; the build declares no dependency-injection framework.
  - **Tags**: `architecture`
- **NF-004** A content download runs asynchronously: `PUT /api/v1/link` answers without waiting for the download, extraction or embedding to finish.
  - **Tags**: `content`, `links`
- **NF-005** Vector search inlines the query vector into the SQL text only after checking that every component is a finite float, and rejects a vector with a null, NaN or infinite component; link-list filter values are passed as bound parameters.
  - **Tags**: `search`, `security`
- **NF-006** Application logs are written to standard output as JSON (Logstash encoder), with the `it.robfrank.linklift` logger at DEBUG and the root at INFO.
  - **Tags**: `observability`
- **NF-007** The container image built from `Dockerfile.local` runs the JAR as the non-root user `appuser` on `eclipse-temurin:25-alpine`, exposes port 7070 and takes JVM options from `JAVA_OPTS`.
  - **Tags**: `deploy`, `guide:deploy`
- **NF-008** `./mvnw test` passes on a machine with a running Docker engine, and needs one: every ArcadeDB container the test sources start through Testcontainers uses the image `arcadedata/arcadedb:<v>`, where `<v>` is the ArcadeDB library version on the test classpath (`com.arcadedb.Constants.getRawVersion()`), and no ArcadeDB container in the test sources names `arcadedata/arcadedb-headless` or the `latest` tag; `VectorSearchE2ETest`, which is not excluded from the default run, starts an `ollama/ollama:latest` container and pulls `all-minilm:l6-v2`; every test app the test sources start through `JavalinTest` listens on `127.0.0.1` only; and `ListLinksControllerTest.getGraph_shouldReturn401_whenUserNotAuthenticated` passes in the full suite as it does when run on its own.
  - **Tags**: `build`, `guide:local-run`
  - **Falsifier**: switching only `ArcadeDbContainer` to the full image while `DatabaseInitializerTest` and `AuthenticationIntegrationTest` keep the headless image passes `ArcadeDbContainerTest` — ruled out by test rows `T-image-sources` and `T-full-suite`.

- **NF-FBB-001** Every Java source under `src/main/java` and `src/test/java` is left untouched by prettier's check mode with `prettier@3.3.3` and `prettier-plugin-java@2.6.4` under the repo's `.prettierrc.js` (`printWidth: 160`), and CI enforces it: the `check` job of `.github/workflows/ci.yml` and of `.github/workflows/clean-code.yml` runs the pre-commit `prettier` hook, which fails on any Java file it would reformat, and each workflow's `build` job runs only after `check` passes.
  - **Tags**: `build`, `ci`, `format`
  - **Falsifier**: formatting only `AskControllerTest` and `AskQuestionServiceTest` while another unformatted Java file stays passes a spot check of those — ruled out by test row `T-format-check`.
