# LinkLift Roadmap

Based on the analysis of `ARCHITECTURE.md`, `DEVELOPER_GUIDE.md`, and the current codebase structure (Backend & Frontend).

## Project Status Analysis

The project implements a **Hexagonal Architecture** backend (Java 24, Javalin, ArcadeDB) and a **React 19** frontend (MUI 7).

**Implemented Domains:**

- **Links**:
  - Backend: Create, List, Get Related.
  - Frontend: `AddLink`, `LinkList` components.
- **Authentication**:
  - Backend: User creation, Auth, Token management.
  - Frontend: `Login`, `Register`, `ProtectedRoute`, Auth Context.
- **Content**:
  - Backend: Download, Get content.
  - Frontend: `ContentViewer`.
- **Collections**:
  - Backend: Create collection.
  - Frontend: _Missing UI_.

**Observations & Gaps:**

- **Backend Typo**: `it.robfrank.linklift.adapter.out.persitence` -> `persistence`.
- **Documentation**: `ARCHITECTURE.md` needs updates for Auth, Content, and Collections.
- **Collections**: Backend is partial (Create only); Frontend is non-existent.
- **Links**: Missing Update/Delete in both Backend and Frontend.

---

## Roadmap Phases

### Phase 1: Housekeeping & Documentation (Immediate)

- **Backend**:
  - [x] **Fix Package Typo**: Rename `it.robfrank.linklift.adapter.out.persitence` to `it.robfrank.linklift.adapter.out.persistence`.
  - [x] **Update Architecture Documentation**: Document Auth, Content, and Collection domains.
- **Frontend**:
  - [x] **Documentation**: Ensure `webapp/README.md` reflects current setup and run instructions.
  - [x] **Audit**: Verify `package.json` scripts and dependencies are clean.

### Phase 2: Core Feature Completion (Short Term)

- **Collections Domain**:
  - **Backend**:
    - [x] Implement `ListCollectionsUseCase` (List collections for a user).
    - [x] Implement `GetCollectionUseCase` (Get details + links).
    - [x] Implement `AddLinkToCollectionUseCase` and `RemoveLinkFromCollectionUseCase`.
    - [x] Implement `DeleteCollectionUseCase`.
  - **Frontend**:
    - [x] Create `CollectionList` page/component.
    - [x] Create `CollectionDetail` page (view links within a collection).
    - [x] Add "Create Collection" Modal/Form.
    - [ ] Add "Add to Collection" action in `LinkList` items.
- **Links Domain**:
  - **Backend**:
    - [ ] Implement `UpdateLinkUseCase` (Edit title, description, tags).
    - [ ] Implement `DeleteLinkUseCase`.
  - **Frontend**:
    - [ ] Add Edit Link Modal/Form.
    - [ ] Add Delete Link confirmation dialog.
    - [ ] Update `LinkList` to reflect changes immediately.
- **Content Domain**:
  - **Backend**:
    - [ ] Review `DownloadContentService` robustness (retries).
    - [ ] Implement `DeleteContentUseCase`.
  - **Frontend**:
    - [ ] Improve `ContentViewer` error states.
    - [ ] Add "Refresh Content" button (re-trigger download).

### Phase 3: Quality & Robustness (Medium Term)

- **Backend**:
  - [ ] **Testing**: Unit tests for all Services; Integration tests for Controllers/Repositories.
  - [ ] **Validation**: Standardize `ValidationException` usage.
  - [ ] **Observability**: Enhance logging and metrics.
- **Frontend**:
  - [ ] **UX**: Add Toast notifications (Snackbars) for success/error actions.
  - [ ] **UX**: Implement Loading Skeletons for data fetching.
  - [ ] **Error Handling**: Add React Error Boundaries.
  - [ ] **Testing**: Add Unit tests for Components and Integration tests for flows.

### Phase 4: Architectural Evolution (Long Term)

- **Backend**:
  - [ ] **CQRS**: Separate Read/Write models.
  - [ ] **Event Sourcing**: Capture state changes as events.
  - [ ] **Caching**: Redis for `GetContent` and `GetRelatedLinks`.
- **Frontend**:
  - [ ] **State Management**: Evaluate React Query (TanStack Query) for better server-state management and caching.
  - [ ] **Optimistic UI**: Implement optimistic updates for smoother interactions.
