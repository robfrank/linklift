# Frontend Architecture Refactor Plan: Hexagonal Architecture

## 1. Executive Summary

This document proposes a strategic refactor of the LinkLift frontend to align with **Hexagonal (Ports and Adapters) Architecture**, mirroring the clean separation of concerns present in the backend.

The primary goal is to **decouple core application logic from the React framework**. This will make our business logic framework-agnostic, easier to test in isolation, and simpler to maintain or even migrate to a different UI technology in the future.

**Key Benefits:**

- **Testability**: Core logic can be unit-tested without rendering components or mocking the browser environment.
- **Maintainability**: Clear boundaries prevent business logic from leaking into UI components, making the codebase easier to understand and modify.
- **Flexibility**: The core application can be driven by different adapters. Today it's React; tomorrow it could be a command-line interface, a different web framework, or automated tests.
- **Technology Independence**: Reduces reliance on framework-specific features (e.g., React hooks for state management) for core business operations.

## 2. Current Architecture Analysis

The current frontend is a standard, well-organized React application.

- **Structure**: Component-driven, with logic encapsulated in components (`AddLink.js`), custom hooks (`useContent.js`), and services (`api.js`).
- **State Management**: React Context (`AuthContext`, `SnackbarContext`) is used for global state.
- **Coupling**: Business logic and side effects (API calls) are often initiated directly from within React components or hooks, creating a tight coupling between the logic and the UI framework.

This is a perfectly valid approach, but it does not enforce a strict separation of concerns, which is the goal of this refactor.

## 3. Proposed Hexagonal Architecture

We will restructure the application into three distinct layers, forming the "inside" and "outside" of our hexagon.

![Frontend Hexagonal Architecture](https://i.imgur.com/3T0x5GG.png)

### Layer 1: Domain (The "Inside")

This is the core of our application. It contains pure, framework-agnostic business logic and has **zero dependencies** on React or any other external library.

- **Models/Entities**: Plain JavaScript/TypeScript objects representing our core concepts (`Link`, `User`, `Collection`). These contain data and validation logic.
- **Use Cases**: Functions or classes that orchestrate specific business operations (`addLinkUseCase`, `listUserCollectionsUseCase`). They contain the rules and steps for a given feature.
- **Ports (Output)**: Interfaces that define contracts for anything the application needs from the outside world, such as fetching data or storing it. For example, `ILinkRepository` would define methods like `save(link)` and `getAll()`.

### Layer 2: Application (The Bridge)

This layer acts as a bridge, orchestrating the flow of data between the **Domain** and the **Infrastructure** layers.

- **Service Implementations (Adapters)**: Concrete implementations of the output ports. For example, `ApiLinkRepository` implements the `ILinkRepository` port and uses a generic HTTP client to make the actual API calls.
- **State Management**: A centralized, framework-agnostic state store (e.g., using Zustand, Redux Toolkit, or a simple vanilla JS publisher/subscriber model). Use cases will interact with this store to update the application's state, and the UI will subscribe to it.

### Layer 3: Infrastructure (The "Outside")

This layer contains all external concerns and framework-specific code. It's the "shell" that interacts with the user and the browser.

- **UI (Input Adapter)**: The React application itself.
  - **Views/Pages**: Smart components that compose UI elements and respond to user events.
  - **Components**: Dumb, presentational components that receive data and callbacks via props.
  - **Hooks**: React-specific hooks that act as the primary glue, connecting views to the **Application** layer by calling use cases and subscribing to the state store.
- **Platform Services (Output Adapters)**:
  - **API Client**: The low-level `fetch` or `axios` wrapper that makes HTTP requests.
  - **Browser Storage**: Adapters for `localStorage` or `sessionStorage` that implement a generic storage port.
  - **Analytics, Loggers, etc.**

## 4. Proposed Directory Structure

To enforce these boundaries, we will adopt the following folder structure inside `webapp/src/`:

```
webapp/src/
│
├───domain/
│   ├───models/           # Link.ts, User.ts (TypeScript is recommended)
│   ├───ports/             # ILinkRepository.ts, IAuthService.ts (Interfaces)
│   └───usecases/          # AddLinkUseCase.ts, ListLinksUseCase.ts
│
├───application/
│   ├───services/          # Adapters: ApiLinkRepository.ts, LocalAuthService.ts
│   ├───state/             # Central state store: store.ts, linkSlice.ts
│   └───di-container.ts    # Dependency Injection: Wiring everything together
│
└───infrastructure/
    ├───ui/                # React-specific world
    │   ├───components/    # Dumb/Presentational Components (Button, Card)
    │   ├───views/         # Smart "Page" Components (LinkListView, LoginView)
    │   ├───hooks/         # Glue hooks: useLinks.ts, useAuth.ts
    │   └───contexts/      # UI-specific contexts (e.g., ThemeContext)
    │
    ├───api/               # Low-level HTTP client wrapper (axios-instance.ts)
    ├───storage/           # LocalStorageAdapter.ts
    └───__tests__/         # UI-specific integration and e2e tests
```

## 5. Step-by-Step Refactoring Plan

This refactor can be done incrementally, feature by feature.

### Phase 1: Foundation & Tooling (Prerequisite)

1.  **Introduce TypeScript**: The explicitness of types is crucial for defining clear contracts (ports) and domain models. This is the most important first step.
2.  **Choose State Management**: Select a library for our framework-agnostic state. **Zustand** is an excellent, lightweight choice that fits this model well.
3.  **Set up Dependency Injection**: Create a simple DI container (`di-container.ts`) to instantiate and wire up services and use cases. This avoids singletons and makes dependencies explicit.

### Phase 2: Refactor a Single Feature (e.g., "Add Link")

1.  **Define the Domain**:

    - Create the `Link.ts` model in `domain/models/`.
    - Define the `ILinkRepository` interface in `domain/ports/` with a `save(link: Link): Promise<Link>` method.
    - Create the `AddLinkUseCase.ts` in `domain/usecases/`. It will take the repository as a dependency in its constructor and have an `execute(url: string, ...)` method.

2.  **Implement the Application Layer**:

    - Create the `ApiLinkRepository.ts` in `application/services/`. It will implement `ILinkRepository` and use the low-level API client from `infrastructure/api/` to make the PUT request.
    - Define a "links" slice in the state store (`application/state/`).
    - The `AddLinkUseCase` will call the repository and, on success, update the state store with the new link.

3.  **Refactor the UI (Infrastructure Layer)**:
    - The `AddLink.js` component becomes a dumb form. It receives a callback like `onAddLink(url: string)` via props.
    - Create a `useLinks.ts` hook in `infrastructure/ui/hooks/`.
    - This hook will:
      - Get the `addLinkUseCase` instance from the DI container.
      - Provide the `onAddLink` callback to the view, which calls `addLinkUseCase.execute(...)`.
      - Subscribe to the links slice of the state store to get the list of links.
    - The `LinkListView.tsx` (or similar) will use the `useLinks` hook to get data and callbacks and pass them down to its child components.

### Phase 3: Incremental Rollout

- Apply the pattern from Phase 2 to all other features:
  - Authentication (Login/Register)
  - Collections
  - Content Viewing
- Continuously move logic "inwards" from components -> hooks -> use cases -> domain models.

## 6. Testing Strategy

The new architecture enables a much cleaner testing pyramid:

- **Unit Tests (Domain)**: Test use cases and models in complete isolation with a tool like Vitest or Jest. Mock the repository ports. These tests will be fast and numerous.
- **Integration Tests (Application)**: Test the service adapters (repositories) against a mocked API (`msw` or `jest.mock`).
- **UI/Component Tests (Infrastructure)**: Use React Testing Library to test that components render correctly given certain props and that they call the right callbacks from hooks on user interaction.
- **End-to-End Tests**: Use Cypress or Playwright to test full user flows, unchanged from the current strategy.
