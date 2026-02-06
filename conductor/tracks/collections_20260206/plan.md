# Implementation Plan: Collection Management System

This plan outlines the steps to implement a tag-like collection system with AI-generated summaries and graph visualization.

## Phase 1: Backend Domain and Ports [checkpoint: c69819b]

- [x] Task: Define Collection Domain Model and Ports
  - [x] Write Tests: `Collection` entity and `CollectionRepository` port tests
  - [x] Implement Feature: `Collection` record and `CollectionRepository` interface
- [x] Task: Define Summary Service Port
  - [x] Write Tests: `CollectionSummaryService` port definition tests
  - [x] Implement Feature: `CollectionSummaryService` port interface
- [x] Task: Conductor - User Manual Verification 'Phase 1: Backend Domain and Ports' (Protocol in workflow.md)

## Phase 2: Persistence Adapter (ArcadeDB Graph) [checkpoint: 1805404]

- [x] Task: Implement ArcadeDB Collection Repository
  - [x] Write Tests: `ArcadeCollectionRepository` integration tests (Testcontainers)
  - [x] Implement Feature: CRUD operations for Collection vertices and Link edges
- [x] Task: Implement Collection Merging Logic in Repository
  - [x] Write Tests: Integration tests for merging two collections
  - [x] Implement Feature: Atomic merge operation in ArcadeDB
- [x] Task: Conductor - User Manual Verification 'Phase 2: Persistence Adapter (ArcadeDB Graph)' (Protocol in workflow.md)

## Phase 3: Application Services and AI Integration

- [ ] Task: Implement Collection Management Service
  - [ ] Write Tests: `CollectionService` unit tests (mocking repository)
  - [ ] Implement Feature: Business logic for CRUD and merging
- [ ] Task: Implement AI Summary Service Adapter
  - [ ] Write Tests: `OllamaCollectionSummaryAdapter` integration tests (WireMock/Ollama)
  - [ ] Implement Feature: Prompt engineering and Ollama integration for summaries
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Application Services and AI Integration' (Protocol in workflow.md)

## Phase 4: Web API (REST Endpoints)

- [ ] Task: Implement Collection REST Endpoints
  - [ ] Write Tests: `CollectionController` integration tests (JavalinTest)
  - [ ] Implement Feature: Endpoints for GET, POST, PUT, DELETE, and /merge
- [ ] Task: Implement Bulk Link Operations Endpoints
  - [ ] Write Tests: Tests for bulk adding/removing links
  - [ ] Implement Feature: Endpoints for bulk collection updates
- [ ] Task: Conductor - User Manual Verification 'Phase 4: Web API (REST Endpoints)' (Protocol in workflow.md)

## Phase 5: Frontend State and Sidebar

- [ ] Task: Create Collection Zustand Store
  - [ ] Write Tests: `useCollectionStore` unit tests
  - [ ] Implement Feature: State management for collections and sidebar order
- [ ] Task: Build Collection Sidebar Component
  - [ ] Write Tests: `CollectionSidebar` component tests
  - [ ] Implement Feature: UI for listing and navigating collections
- [ ] Task: Implement Drag-and-Drop for Collections
  - [ ] Write Tests: Tests for drag-and-drop interactions
  - [ ] Implement Feature: Drag links into collections and reorder sidebar
- [ ] Task: Conductor - User Manual Verification 'Phase 5: Frontend State and Sidebar' (Protocol in workflow.md)

## Phase 6: Visualization and Polish

- [ ] Task: Update Graph Visualization for Collections
  - [ ] Write Tests: `GraphView` tests for collection node rendering
  - [ ] Implement Feature: Render collection nodes and edges in force-graph
- [ ] Task: Final UI Polish and "Generate Summary" Button
  - [ ] Write Tests: End-to-end tests for the summary workflow
  - [ ] Implement Feature: UI for triggering and displaying AI summaries
- [ ] Task: Conductor - User Manual Verification 'Phase 6: Visualization and Polish' (Protocol in workflow.md)
