# Implementation Plan: User Authentication Flow

This plan outlines the steps to implement user registration and login using hexagonal architecture.

## Phase 1: Domain and Infrastructure Foundation [checkpoint: 6ab6efd]

- [x] Task: Define Domain Model and Ports
  - [x] Write Tests: Define `User` entity and `UserRepository` port interface tests
  - [x] Implement Feature: Create `User` entity and `UserRepository` port
- [x] Task: Implement Password Encoding
  - [x] Write Tests: `BCryptPasswordEncoder` unit tests
  - [x] Implement Feature: `BCryptPasswordEncoder` using BCrypt library
- [x] Task: Implement Token Management
  - [x] Write Tests: `JJWTTokenProvider` unit tests
  - [x] Implement Feature: `JJWTTokenProvider` using `java-jwt`
- [x] Task: Conductor - User Manual Verification 'Phase 1: Domain and Infrastructure Foundation' (Protocol in workflow.md)

## Phase 2: Persistence and Application Services [checkpoint: a427973]

- [x] Task: Implement ArcadeDB User Persistence
  - [x] Write Tests: `ArcadeDBUserRepository` integration tests with Testcontainers
  - [x] Implement Feature: `ArcadeDBUserRepository`
- [x] Task: Implement User Registration Use Case
  - [x] Write Tests: `RegisterUserUseCase` unit tests
  - [x] Implement Feature: `RegisterUserUseCase` with uniqueness check
- [x] Task: Implement Login Use Case
  - [x] Write Tests: `LoginUserUseCase` unit tests
  - [x] Implement Feature: `LoginUserUseCase`
- [x] Task: Conductor - User Manual Verification 'Phase 2: Persistence and Application Services' (Protocol in workflow.md)

## Phase 3: Web API and Security Integration [checkpoint: f13484c]

- [x] Task: Implement Auth REST Endpoints
  - [x] Write Tests: `AuthController` integration tests using JavalinTest
  - [x] Implement Feature: `register` and `login` endpoints
- [x] Task: Implement JWT Middleware/Filter
  - [x] Write Tests: Authentication middleware tests for protected routes
  - [x] Implement Feature: Javalin access manager or filter for JWT validation
- [x] Task: Conductor - User Manual Verification 'Phase 3: Web API and Security Integration' (Protocol in workflow.md)

## Phase 4: Frontend Authentication UI [checkpoint: f13484c]

- [x] Task: Create Auth State Management
  - [x] Write Tests: Zustand `useAuthStore` unit tests
  - [x] Implement Feature: `useAuthStore` for managing user state and tokens
- [x] Task: Build Registration Page
  - [x] Write Tests: `Register` component tests with React Testing Library
  - [x] Implement Feature: Registration form with validation
- [x] Task: Build Login Page
  - [x] Write Tests: `Login` component tests with React Testing Library
  - [x] Implement Feature: Login form and redirect logic
- [x] Task: Conductor - User Manual Verification 'Phase 4: Frontend Authentication UI' (Protocol in workflow.md)
