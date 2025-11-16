# LinkLift Architecture Documentation

## Overview

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

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Adapters (Infrastructure)                         │
├─────────────────────────────────────────────────────────────────────────────┤
│  Web Layer              │  Persistence Layer      │  Event Layer           │
│  ┌───────────────────┐  │  ┌─────────────────────┐ │  ┌───────────────────┐ │
│  │ NewLinkController │  │  │LinkPersistenceAdapter│ │  │SimpleEventPublisher││
│  │ListLinksController│  │  │ArcadeLinkRepository  │ │  │                   │ │
│  │  GlobalException  │  │  │     LinkMapper      │ │  │                   │ │
│  │     Handler       │  │  │                     │ │  │                   │ │
│  └───────────────────┘  │  └─────────────────────┘ │  └───────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Ports (Interfaces)                              │
├─────────────────────────────────────────────────────────────────────────────┤
│  Input Ports             │  Output Ports                                   │
│  ┌───────────────────┐   │  ┌─────────────────────┐ ┌───────────────────┐  │
│  │ NewLinkUseCase    │   │  │   SaveLinkPort      │ │DomainEventPublisher│  │
│  │ListLinksUseCase   │   │  │   LoadLinksPort     │ │                   │  │
│  └───────────────────┘   │  └─────────────────────┘ └───────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Application Layer                                │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌───────────────────┐   ┌─────────────────────┐   ┌───────────────────┐   │
│  │  NewLinkService   │   │  ListLinksService   │   │ Future Services   │   │
│  │                   │   │                     │   │                   │   │
│  └───────────────────┘   └─────────────────────┘   └───────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
┌─────────────────────────────────────────────────────────────────────────────┐
│                             Domain Layer                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│  Domain Model           │  Domain Events          │  Domain Exceptions     │
│  ┌───────────────────┐  │  ┌─────────────────────┐ │  ┌───────────────────┐ │
│  │      Link         │  │  │   DomainEvent       │ │  │LinkLiftException  │ │
│  │    LinkPage       │  │  │LinkCreatedEvent     │ │  │ValidationException│ │
│  │ ListLinksQuery    │  │  │LinksQueryEvent      │ │  │DatabaseException  │ │
│  │ NewLinkCommand    │  │  │                     │ │  │                   │ │
│  └───────────────────┘  │  └─────────────────────┘ │  └───────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Package Structure

```
src/main/java/it/robfrank/linklift/
├── Application.java                    # Main application entry point
├── adapter/                           # Infrastructure adapters
│   ├── in/                           # Inbound adapters (drivers)
│   │   └── web/                      # Web layer (REST controllers)
│   │       ├── NewLinkController.java
│   │       ├── ListLinksController.java
│   │       └── error/                # Error handling
│   │           ├── GlobalExceptionHandler.java
│   │           └── ErrorResponse.java
│   └── out/                          # Outbound adapters (driven)
│       ├── persistence/              # Database adapters
│       │   ├── ArcadeLinkRepository.java
│       │   ├── LinkPersistenceAdapter.java
│       │   └── LinkMapper.java
│       └── event/                    # Event publishing
│           └── SimpleEventPublisher.java
├── application/                       # Application layer
│   ├── domain/                       # Domain model and business logic
│   │   ├── model/                    # Domain entities
│   │   │   ├── Link.java
│   │   │   └── LinkPage.java
│   │   ├── service/                  # Domain services
│   │   │   ├── NewLinkService.java
│   │   │   └── ListLinksService.java
│   │   ├── event/                    # Domain events
│   │   │   ├── DomainEvent.java
│   │   │   ├── LinkCreatedEvent.java
│   │   │   └── LinksQueryEvent.java
│   │   └── exception/                # Domain exceptions
│   │       ├── LinkLiftException.java
│   │       ├── ErrorCode.java
│   │       ├── ValidationException.java
│   │       └── DatabaseException.java
│   └── port/                         # Port interfaces
│       ├── in/                       # Input ports (use cases)
│       │   ├── NewLinkUseCase.java
│       │   ├── NewLinkCommand.java
│       │   ├── ListLinksUseCase.java
│       │   └── ListLinksQuery.java
│       └── out/                      # Output ports (repositories)
│           ├── SaveLinkPort.java
│           ├── LoadLinksPort.java
│           └── DomainEventPublisher.java
└── config/                           # Configuration
    ├── WebBuilder.java
    └── DatabaseInitializer.java
```

## Layer Responsibilities

### Domain Layer (Core Business Logic)

**Purpose**: Contains the core business logic, domain models, and business rules.

**Key Components**:

- **Entities**: `Link`, `LinkPage` - Core business objects with behavior and constraints
- **Value Objects**: `ListLinksQuery`, `NewLinkCommand` - Immutable data containers
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

- **Use Case Interfaces**: `NewLinkUseCase`, `ListLinksUseCase` - Define business operations
- **Service Implementations**: `NewLinkService`, `ListLinksService` - Implement use cases
- **Port Interfaces**: `SaveLinkPort`, `LoadLinksPort` - Abstract external dependencies

**Rules**:

- ✅ Depends only on domain layer and port interfaces
- ✅ Contains application-specific business logic
- ✅ Publishes domain events
- ✅ Handles transaction boundaries

**Example**:

```java
@Service
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
@RestController
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

### Java 24

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
- **AssertJ**: Fluent assertions for better readability
- **Mockito**: Comprehensive mocking for isolated unit tests

## Data Flow Architecture

### Create Link Flow

```
HTTP Request → NewLinkController → NewLinkService → LinkPersistenceAdapter → ArcadeDB
                     │                    │                    │
                     ▼                    ▼                    ▼
               Validation          Domain Logic         Data Persistence
                     │                    │                    │
                     ▼                    ▼                    ▼
               ErrorResponse     LinkCreatedEvent        Database Record
```

### List Links Flow

```
HTTP Request → ListLinksController → ListLinksService → LinkPersistenceAdapter → ArcadeDB
     │                   │                   │                    │
     ▼                   ▼                   ▼                    ▼
Query Parameters    Validation        Business Logic         SQL Query
     │                   │                   │                    │
     ▼                   ▼                   ▼                    ▼
Pagination Info    LinksQueryEvent      LinkPage          Database Results
```

## Domain Events Architecture

### Event Flow

```
Domain Service → DomainEventPublisher → Event Subscribers
     │                    │                    │
     ▼                    ▼                    ▼
Business Action    Event Distribution    Side Effects
```

### Event Types

- **LinkCreatedEvent**: Published when a new link is successfully created
- **LinksQueryEvent**: Published when links are queried (for analytics)

### Event Benefits

- **Loose Coupling**: Services don't directly depend on side effects
- **Extensibility**: Easy to add new event subscribers
- **Analytics**: Built-in tracking of system usage
- **Auditability**: Complete record of business events

## Error Handling Architecture

### Exception Hierarchy

```
LinkLiftException (abstract base)
├── ValidationException          # Business rule violations
├── LinkNotFoundException       # Entity not found
├── LinkAlreadyExistsException  # Duplicate entity
└── DatabaseException          # Infrastructure failures
```

### Error Processing Flow

```
Exception Thrown → GlobalExceptionHandler → ErrorResponse → HTTP Response
     │                        │                  │              │
     ▼                        ▼                  ▼              ▼
Domain/Service          Map to Status Code  Standardized     JSON Response
                                            Format
```

### Error Response Strategy

- **Consistent Format**: All errors return same JSON structure
- **Error Codes**: Numeric codes for programmatic handling
- **Field Validation**: Detailed field-level error messages
- **Security**: Sensitive details logged, generic messages returned

## Configuration and Dependency Management

### Dependency Injection Strategy

```java
// Manual wiring in Application.java (current approach)
LinkPersistenceAdapter persistenceAdapter = new LinkPersistenceAdapter(repository);

NewLinkUseCase newLinkUseCase = new NewLinkService(persistenceAdapter, eventPublisher);

NewLinkController controller = new NewLinkController(newLinkUseCase);

```

### Configuration Management

- **WebBuilder**: Fluent API for Javalin configuration
- **DatabaseInitializer**: Automatic schema setup
- **Environment Variables**: External configuration support

## Quality Attributes

### Testability

- **Unit Tests**: Domain logic tested in isolation
- **Integration Tests**: Database interactions with real DB
- **Contract Tests**: API behavior verification
- **Test Coverage**: >90% code coverage target

### Maintainability

- **Clear Boundaries**: Hexagonal architecture enforces separation
- **Single Responsibility**: Each class has one clear purpose
- **Immutable Objects**: Reduced state mutation complexity
- **Consistent Patterns**: Uniform approach across codebase

### Scalability

- **Stateless Design**: Horizontal scaling ready
- **Event-Driven**: Asynchronous processing capability
- **Connection Pooling**: Efficient database resource usage
- **Pagination**: Bounded result sets prevent memory issues

### Security

- **Input Validation**: Comprehensive validation at multiple layers
- **SQL Injection Prevention**: Parameterized queries only
- **Error Information Disclosure**: Sanitized error responses
- **Framework Security**: Javalin security best practices

## Future Architecture Evolution

### Planned Enhancements

1. **CQRS Implementation**: Separate read/write models
2. **Event Sourcing**: Event-based state reconstruction
3. **Microservices**: Service decomposition strategy
4. **Caching Layer**: Redis integration for performance
5. **Message Queues**: Asynchronous event processing

### Migration Strategy

The current hexagonal architecture provides a solid foundation for these enhancements:

- **CQRS**: Current use cases can evolve into command/query separation
- **Event Sourcing**: Existing domain events are the foundation
- **Microservices**: Clear boundaries enable service extraction
- **Caching**: Port interfaces allow transparent caching insertion

This architecture ensures LinkLift remains maintainable, testable, and adaptable to future requirements while providing a robust foundation for link management functionality.
