# CLAUDE.md — Frontend (webapp/)

Guidance for Claude Code when working in the LinkLift React frontend.

## Overview

Single-page React application with hexagonal architecture mirroring the backend. TypeScript for new code, JavaScript for legacy components being migrated.

## Structure

```
webapp/src/
├── domain/
│   ├── models/           # TypeScript interfaces (Link, Collection, Content)
│   ├── ports/            # Repository interfaces (ILinkRepository, etc.)
│   └── usecases/         # Use case implementations
├── application/
│   ├── di-container.ts   # Dependency injection wiring
│   ├── services/         # API repository implementations (Axios-based)
│   │   ├── ApiLinkRepository.ts
│   │   ├── ApiCollectionRepository.ts
│   │   └── ApiContentRepository.ts
│   └── state/            # Zustand store slices
│       ├── linkSlice.ts
│       ├── collectionSlice.ts
│       ├── contentSlice.ts
│       └── searchSlice.ts
├── infrastructure/
│   ├── api/              # Axios instance configuration
│   └── ui/
│       ├── pages/        # Page components (HomePage, SearchPage, etc.)
│       ├── components/   # Reusable UI components (LinkList, AddLink, etc.)
│       ├── layouts/      # Layout components
│       └── hooks/        # Custom React hooks
├── components/           # Legacy components (Header, Login, Graph, etc.)
├── contexts/             # React contexts (Auth, Snackbar)
├── hooks/                # Legacy hooks
├── services/             # Legacy service files
├── App.js                # Root component with routing
└── index.tsx             # Entry point
```

## Tech Stack

| Concern       | Technology                                      |
| ------------- | ----------------------------------------------- |
| Framework     | React 19.1                                      |
| Language      | TypeScript 5.9 (new code) / JavaScript (legacy) |
| UI Library    | MUI 7 (Material UI) + Emotion                   |
| State         | Zustand 5 (sliced store)                        |
| Routing       | React Router DOM 7                              |
| HTTP          | Axios                                           |
| Visualization | react-force-graph-2d                            |
| Sanitization  | DOMPurify                                       |
| Testing       | Jest 30 + React Testing Library 16 + MSW 2      |
| Build         | Webpack 5 + Babel                               |

## Commands

```bash
npm start          # Dev server on localhost:8080
npm test           # Run Jest tests
npm run test:watch # Watch mode
npm run build      # Production bundle
```

## Architecture Patterns

- **Hexagonal on frontend**: `domain/ports/` defines interfaces, `application/services/` implements them with Axios, `domain/usecases/` contains business logic.
- **DI container**: `application/di-container.ts` wires repositories and use cases. Import from here, not directly from service files.
- **Zustand slices**: Each domain concern has its own slice (`linkSlice`, `collectionSlice`, etc.) combined in a single store.
- **Migration in progress**: New code goes in `domain/`, `application/`, `infrastructure/ui/`. Legacy code in `components/`, `hooks/`, `services/`, `contexts/` is being migrated.

## Conventions

- Write new components in TypeScript (`.tsx`).
- Use MUI components and Emotion for styling — no raw CSS files.
- Pages go in `infrastructure/ui/pages/`, reusable components in `infrastructure/ui/components/`.
- Use Zustand store (via slices) for state management, not React context for data state.
- Auth context (`contexts/AuthContext.js`) handles JWT tokens and login state.
- Sanitize any user/external HTML with DOMPurify before rendering.

## Testing

- **Jest** with jsdom environment.
- **React Testing Library**: Test behavior, not implementation. Query by role/text, not test IDs.
- **MSW**: Mock API responses at the network level via service workers.
- Test files go in `__tests__/` or colocated as `*.test.tsx`.
- `setupTests.js` configures global test setup.

## API Integration

Backend runs on port 7070. Webpack dev server proxies `/api` requests. Axios instance in `infrastructure/api/` handles base URL, auth headers (JWT from AuthContext), and error interceptors.
