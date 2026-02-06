# Specification: Collection Management System

## Overview

Implement a robust "Collection" system that allows users to organize their saved links into flexible, tag-like groups. This feature will leverage ArcadeDB's graph capabilities to represent collections as nodes connected to link nodes, and use AI to provide automated insights into the content of these collections.

## Functional Requirements

### Core Management

- **Tag-like Organization:** Links can be assigned to multiple collections simultaneously.
- **CRUD Operations:** Users can create, rename, and delete collections.
- **Bulk Operations:** Support for adding or removing multiple links to/from a collection in a single action.
- **Collection Merging:** Ability to merge two or more collections, consolidating all unique links into a single target collection.
- **Sidebar Organization:** A dedicated sidebar for navigating collections, supporting custom reordering via drag-and-drop.

### AI Integration

- **Automated Summaries:** Use Ollama to analyze the metadata and content of links within a collection to generate a concise summary of the collection's overall theme or topic.

### UI/UX

- **Sidebar Interface:** A persistent sidebar for quick access to collections.
- **Drag-and-Drop:** Ability to drag links from the main list and drop them into collection names in the sidebar.
- **Collection View:** A filtered view of links belonging to a selected collection.

### Visualization

- **Graph Integration:** Collections will be represented as distinct central nodes in the interactive graph, with edges connecting them to their member links.

## Technical Requirements

### Domain Layer

- `Collection` entity with fields for ID, name, description, and AI-generated summary.
- `CollectionRepository` port for persistence.
- `SummaryService` port for AI operations.

### Infrastructure Layer (Adapters)

- **Persistence:** Use ArcadeDB's Graph model (`Vertex` for Collection, `Edge` for relationship to Link).
- **AI:** Integration with existing Ollama adapter to generate summaries based on aggregate link content.
- **Web:** REST endpoints for collection CRUD, bulk updates, and merging.

### Frontend

- **State:** Zustand store for managing the list of collections and the current selection.
- **Components:** Sidebar component, Collection management modal, and updated Graph view.

## Acceptance Criteria

- Users can create a collection and add multiple existing links to it.
- A link can successfully appear in two different collection views.
- Merging two collections results in one collection containing the union of links from both.
- The sidebar correctly reflects the user-defined order.
- The graph view displays collection nodes with edges correctly linked to member link nodes.
- Clicking "Generate Summary" on a collection produces a relevant text summary via Ollama.

## Out of Scope

- Shared/Public collections (all collections are private to the user for this track).
- Nested/Hierarchical collections.
