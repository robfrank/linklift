---
trigger: always_on
---

LinkLift is built using **Hexagonal Architecture** (also known as Ports and Adapters), which promotes clean separation of concerns, high testability, and maintainability. The system manages web links with full CRUD operations while maintaining strict architectural boundaries.

## Architectural Principles

### Hexagonal Architecture Benefits

- **Domain Independence**: Business logic is isolated from external concerns
- **Testability**: Easy to test with mock adapters
- **Flexibility**: Easy to swap implementations (database, web framework, etc.)
- **Maintainability**: Clear boundaries reduce coupling between layers

### Key Design Patterns

- **Ports and Adapters**: Define interfaces in the domain, implement in adapters
- **Domain Events**: Loosely coupled communication between components
- **Repository Pattern**: Abstract data access layer
- **Use Case Pattern**: Encapsulate business operations
- **Builder Pattern**: Flexible object construction

## Layer Responsibilities

### Domain Layer (Core Business Logic)

**Purpose**: Contains the core business logic, domain models, and business rules.

**Key Components**:

- **Entities**: `Link`, `LinkPage`, `User`, `Collection`, `Content` - Core business objects with behavior and constraints
- **Value Objects**: `ListLinksQuery`, `NewLinkCommand`, `CreateUserCommand`, `CreateCollectionCommand` - Immutable data containers
- **Domain Events**: `LinkCreatedEvent`, `LinksQueryEvent` - Business event notifications
- **Exceptions**: `ValidationException`, `DatabaseException` - Domain-specific errors

**Rules**:

- ✅ No dependencies on external frameworks or libraries
- ✅ Contains pure business logic and validation
- ✅ Immutable objects using Java records
- ✅ Self-contained and framework-agnostic

**Example**:

```java
public record Link(String id, String url, String title, String description, LocalDateTime extractedAt, String contentType) {
  public Link {
    if (url == null || url.trim().isEmpty()) {
      throw new ValidationException("URL cannot be empty");
    }
    // Business rules validation
  }
}

```

### Application Layer (Use Cases)

**Purpose**: Orchestrates business operations and coordinates between domain and infrastructure.

**Key Components**:

- **Use Case Interfaces**: `NewLinkUseCase`, `ListLinksUseCase`, `CreateUserUseCase`, `CreateCollectionUseCase` - Define business operations
- **Service Implementations**: `NewLinkService`, `ListLinksService`, `CreateUserService`, `CreateCollectionService` - Implement use cases
- **Port Interfaces**: `SaveLinkPort`, `LoadLinksPort`, `SaveUserPort`, `SaveCollectionPort` - Abstract external dependencies

**Rules**:

- ✅ Depends only on domain layer and port interfaces
- ✅ Contains application-specific business logic
- ✅ Publishes domain events
- ✅ Handles transaction boundaries

**Example**:

```java
public class NewLinkService implements NewLinkUseCase {

  private final SaveLinkPort saveLinkPort;
  private final DomainEventPublisher eventPublisher;

  @Override
  public Link newLink(NewLinkCommand command) throws ValidationException {
    // Validate business rules
    Link link = new Link(/* ... */);
    Link savedLink = saveLinkPort.saveLink(link);
    eventPublisher.publish(new LinkCreatedEvent(savedLink));
    return savedLink;
  }
}

```

### Infrastructure Layer (Adapters)

**Purpose**: Implements port interfaces and handles external system integration.

**Inbound Adapters (Web Controllers)**:

- Handle HTTP requests/responses
- Convert between DTOs and domain objects
- Manage web-specific concerns (routing, serialization)

**Outbound Adapters (Persistence, Events)**:

- Implement repository interfaces
- Handle database operations
- Manage external system communication

**Rules**:

- ✅ Implements port interfaces from application layer
- ✅ Contains framework-specific code
- ✅ Handles external system integration
- ✅ No business logic

**Example**:

```java
public class NewLinkController {

  private final NewLinkUseCase newLinkUseCase;

  @PutMapping("/api/v1/link")
  public ResponseEntity<LinkResponse> processLink(@RequestBody LinkRequest request) {
    NewLinkCommand command = new NewLinkCommand(request.url(), request.title(), request.description());
    Link link = newLinkUseCase.newLink(command);
    return ResponseEntity.status(201).body(new LinkResponse(link, "Link received"));
  }
}

```

## Technology Stack Rationale

### Java 25

- **Modern Language Features**: Records, pattern matching, sealed classes
- **Performance**: Latest JVM optimizations
- **Long-term Support**: Enterprise-grade stability

### Javalin Web Framework

- **Simplicity**: Minimal boilerplate compared to Spring Boot
- **Performance**: Lightweight, fast request handling
- **Java-First Design**: Native Java API, no annotations required
- **Flexibility**: Easy to configure and customize

### ArcadeDB

- **Multi-Model**: Supports document, graph, and key-value operations
- **Performance**: Native Java implementation, optimized queries
- **ACID Compliance**: Full transaction support with rollback
- **Schema Flexibility**: Dynamic schema evolution capability

### Testing Stack

- **JUnit 5**: Modern testing with parameterized tests and extensions
- **Testcontainers** : easy integration testing
- **AssertJ**: Fluent assertions for better readability
- **Mockito**: Comprehensive mocking for isolated unit tests
