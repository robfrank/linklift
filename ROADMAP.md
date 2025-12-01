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
    - [x] Add "Add to Collection" action in `LinkList` items.
- **Links Domain**:
  - **Backend**:
    - [x] Implement `UpdateLinkUseCase` (Edit title, description, tags).
    - [x] Implement `DeleteLinkUseCase`.
  - **Frontend**:
    - [x] Add Edit Link Modal/Form.
    - [x] Add Delete Link confirmation dialog.
    - [x] Update `LinkList` to reflect changes immediately.
- **Content Domain**:
  - **Backend**:
    - [x] Review `DownloadContentService` robustness (retries) - Added 3 retries with 1s delay.
    - [x] Implement `DeleteContentUseCase` - Full implementation with all layers.
    - [x] Implement `RefreshContentUseCase` - Endpoint to re-trigger content download.
  - **Frontend**:
    - [x] Improve `ContentViewer` error states - Enhanced error messages with specific error types.
    - [x] Add "Refresh Content" button (re-trigger download) - Refresh button in modal header.

### Phase 3: Quality & Robustness (Medium Term)

- **Backend**:
  - [x] **Testing**: Unit tests for all Services (83 tests passing) - Added tests for Update/Delete Link, Collections, and Content services.
  - [x] **Testing**: Integration tests for Controllers/Repositories.
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
