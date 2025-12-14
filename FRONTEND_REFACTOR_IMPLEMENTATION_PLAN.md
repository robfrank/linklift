# Frontend Refactor Implementation Plan

This document outlines the step-by-step execution plan to refactor the LinkLift frontend to a Hexagonal Architecture, as described in `FRONTEND_ARCHITECTURE_REFACTOR_PLAN.md`.

## Phase 1: Foundation & Tooling (TypeScript & Infrastructure)

_Goal: Enable TypeScript, set up the build system, and establish the core architectural plumbing._

- [x] **1.1. Install TypeScript Dependencies**
  - Install `typescript`, `@types/react`, `@types/react-dom`, `@types/node`, `@babel/preset-typescript`.
  - Install `ts-loader` or configure `babel-loader` for TS.
- [x] **1.2. Configure Build System**
  - Create `webapp/tsconfig.json`.
  - Update `webapp/webpack.config.js` to handle `.ts` and `.tsx` files.
  - Smoke test: Rename `src/index.js` to `src/index.tsx` and verify build.
- [x] **1.3. Setup State Management (Zustand)**
  - Install `zustand`.
  - Create `src/application/state/store.ts` (store foundation).
- [x] **1.4. Setup Dependency Injection**
  - Create `src/application/di-container.ts` for strictly typed dependency injection.

## Phase 2: Vertical Slice - "Add Link" Feature

_Goal: Refactor the "Add Link" feature to prove the architecture pattern._

- [x] **2.1. Domain Layer (Business Logic)**
  - Create `src/domain/models/Link.ts` (Domain Entity).
  - Create `src/domain/ports/ILinkRepository.ts` (Output Port Interface).
  - Create `src/domain/usecases/AddLinkUseCase.ts` (Input Port/Interactor).
- [x] **2.2. Application Layer (Adapters)**
  - Create `src/infrastructure/api/axios-instance.ts` (Low-level HTTP client).
  - Create `src/application/services/ApiLinkRepository.ts` (Adapter Implementation).
  - Update `di-container.ts` to register the new service and use case.
  - Create `src/application/state/linkSlice.ts` to manage link state.
- [x] **2.3. Infrastructure Layer (UI Refactor)**
  - Create `src/infrastructure/ui/hooks/useLinks.ts` (Glue code: DI + Store + UI).
  - Refactor `src/components/AddLink.js` -> `src/infrastructure/ui/components/AddLink.tsx` (Dumb Component).
  - Update the parent view to use `useLinks` and wire up the component.

## Phase 3: Vertical Slice - "List Links" & Data Fetching

_Goal: Migrate the main data fetching logic._

- [x] **3.1. Domain & Application Updates**
  - Update `ILinkRepository` with `getAll(): Promise<Link[]>`.
  - Create `src/domain/usecases/GetLinksUseCase.ts`.
  - Implement `getAll` in `ApiLinkRepository`.
  - Update `linkSlice` to store the list of links.
- [x] **3.2. UI Integration**
  - Update `useLinks.ts` to expose `links` and `loading` state.
  - Refactor the main content view (e.g., `ContentViewer.js` or `App.js` logic) to consume `useLinks` instead of local `useEffect`.

## Phase 4: Remaining Features & Cleanup

_Goal: Complete the migration and remove legacy code._

- [x] **4.1. Refactor Delete Functionality**
  - Add `delete` to Repository, Use Case, and Store.
  - Wire up to UI.
- [x] **4.2. Refactor Content Viewing**
  - Ensure content viewing logic uses the clean architecture flow.
- [x] **4.3. Cleanup**
  - Remove old `src/services/api.js`.
  - Remove unused React Contexts (if replaced by Zustand).
  - Ensure all new files are in the proper `domain/application/infrastructure` structure.
