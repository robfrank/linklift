# Specification: User Authentication Flow

## Overview

Implement a secure user authentication system for LinkLift. This includes user registration, login, and JWT-based session management. The system will follow the project's hexagonal architecture, ensuring a clean separation between domain logic and infrastructure adapters.

## User Stories

- **Registration:** As a new user, I want to create an account with a unique username and password so that I can start saving links.
- **Login:** As a registered user, I want to sign in securely to access my personal knowledge base.
- **Session Management:** As a logged-in user, I want to remain authenticated across requests so that I don't have to log in repeatedly.

## Technical Requirements

### Domain Layer

- `User` entity with fields for ID, username, password hash, and timestamps.
- `AuthToken` value object for JWT representation.
- `UserRepository` port for persisting user data.
- `PasswordEncoder` port for secure password hashing.
- `TokenProvider` port for JWT generation and validation.

### Application Layer

- `RegisterUserUseCase`: Handles user registration logic, including username uniqueness checks and password hashing.
- `LoginUserUseCase`: Validates credentials and returns a JWT.
- `ValidateTokenUseCase`: Verifies the authenticity and expiration of a JWT.

### Infrastructure Layer (Adapters)

- **Persistence:** `ArcadeDBUserRepository` using ArcadeDB for storage.
- **Security:** `BCryptPasswordEncoder` using the BCrypt library.
- **Security:** `JJWTTokenProvider` using the `java-jwt` library.
- **Web:** REST endpoints for `/api/v1/auth/register` and `/api/v1/auth/login`.

## Acceptance Criteria

- Users can register with a unique username.
- Passwords are encrypted before storage using BCrypt.
- Users can log in and receive a valid JWT.
- Protected API endpoints reject requests without a valid JWT.
- Comprehensive unit and integration tests cover all new components.
- Frontend includes Login and Registration pages with basic validation.
