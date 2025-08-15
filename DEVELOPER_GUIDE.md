# LinkLift Developer Guide

## Getting Started

This guide will help you set up the development environment and understand the development workflow for LinkLift.

## Prerequisites

### Required Software

- **Java 17 or higher**: [Download from Oracle](https://www.oracle.com/java/technologies/downloads/) or use OpenJDK
- **Maven 3.8+**: [Installation Guide](https://maven.apache.org/install.html)
- **Docker & Docker Compose**: [Get Docker](https://docs.docker.com/get-docker/)
- **Git**: [Download Git](https://git-scm.com/downloads)

### Recommended Tools

- **IntelliJ IDEA** or **Eclipse**: Java IDE with Maven support
- **Postman** or **Insomnia**: For API testing
- **ArcadeDB Studio**: Database management GUI (accessed via browser)

## Local Development Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd linklift
```

### 2. Environment Setup

#### Using Docker Compose (Recommended)

```bash
# Start all services (database + application)
docker-compose up -d

# View logs
docker-compose logs -f linklift

# Stop services
docker-compose down
```

#### Manual Setup

```bash
# 1. Start ArcadeDB
docker run -d --name arcadedb \
  -p 2480:2480 -p 2424:2424 \
  -e JAVA_OPTS="-Darcadedb.server.rootPassword=playwithdata" \
  arcadedata/arcadedb:25.6.1

# 2. Build and run application
mvn clean package
java -jar target/linklift-1.0-SNAPSHOT.jar
```

### 3. Verify Installation

```bash
# Check application health
curl http://localhost:7070/up
# Should return: OK

# Test API endpoint
curl http://localhost:7070/api/v1/links
# Should return: {"data": {"content": [], ...}, "message": "Links retrieved successfully"}
```

## Project Structure

```
linklift/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── it/robfrank/linklift/
│   │   │       ├── Application.java          # Main entry point
│   │   │       ├── adapter/                  # Infrastructure layer
│   │   │       │   ├── in/web/              # REST controllers
│   │   │       │   └── out/                 # Database, event adapters
│   │   │       │       ├── persistence/     # ArcadeDB integration
│   │   │       │       └── event/          # Event publishing
│   │   │       ├── application/             # Application layer
│   │   │       │   ├── domain/              # Business logic
│   │   │       │   │   ├── model/          # Domain entities
│   │   │       │   │   ├── service/        # Use case implementations
│   │   │       │   │   ├── event/          # Domain events
│   │   │       │   │   └── exception/      # Domain exceptions
│   │   │       │   └── port/               # Port interfaces
│   │   │       │       ├── in/             # Input ports (use cases)
│   │   │       │       └── out/            # Output ports (repositories)
│   │   │       └── config/                 # Configuration
│   │   └── resources/
│   │       └── logback.xml                 # Logging configuration
│   └── test/
│       └── java/                           # Test classes
├── webapp/                                 # React frontend (separate module)
├── docker-compose.yml                     # Development environment
├── pom.xml                                # Maven configuration
├── CLAUDE.md                              # Project instructions
└── README.md                              # Project overview
```

## Build and Test Commands

### Maven Commands (from CLAUDE.md)

```bash
# Clean and build the project
mvn clean package

# Run tests
mvn test

# Run a single test
mvn test -Dtest=TestClassName

# Run a specific test method
mvn test -Dtest=TestClassName#testMethodName

# Start with Docker Compose
docker-compose up -d
```

### Build Artifacts

- `target/linklift-1.0-SNAPSHOT.jar`: Executable JAR file
- `target/classes/`: Compiled classes
- `target/test-classes/`: Compiled test classes
- `target/surefire-reports/`: Test execution reports

## Code Style Guidelines (from CLAUDE.md)

### Architecture Guidelines

- **Follow strict hexagonal (ports and adapters) architecture**
- **Package Structure**:
  - `adapter`: External adapters (web, persistence)
  - `application.domain`: Domain model and business logic
  - `application.port`: Interface definitions
  - `config`: Configuration classes

### Naming Conventions

- **Classes**: `XyzUseCase`, `XyzService`, `XyzController`, `XyzException`
- **Methods**: Be explicit and descriptive
- **Tests**: `methodName_shouldBehavior_whenCondition`

### Code Quality Rules

- **Immutability**: Prefer immutable domain objects using Java records
- **Error Handling**: Use centralized `GlobalExceptionHandler` with domain exceptions extending `LinkLiftException`
- **Domain Events**: Create events in domain layer, publish through port interfaces
- **Imports**: Order imports and remove unused imports
- **Testing**: Use JUnit 5, AssertJ for assertions

## Development Workflow

### 1. Feature Development Process

#### Step 1: Understand Requirements

- Review existing architecture and patterns
- Identify affected layers (domain, application, infrastructure)
- Plan integration with existing components

#### Step 2: Domain Layer First

```java
// 1. Define domain entity (if needed)
public record NewEntity(String id, String name, LocalDateTime createdAt) {
  // Business validation in constructor
}

// 2. Create domain events
public class EntityCreatedEvent extends DomainEvent {

  private final NewEntity entity;
}

// 3. Define domain exceptions
public class EntityNotFoundException extends LinkLiftException {

  public EntityNotFoundException(String id) {
    super(ErrorCode.ENTITY_NOT_FOUND, "Entity not found: " + id);
  }
}

```

#### Step 3: Application Layer

```java
// 1. Define use case interface (input port)
public interface NewEntityUseCase {
  NewEntity createEntity(CreateEntityCommand command) throws ValidationException;
}

// 2. Define repository interface (output port)
public interface NewEntityRepository {
  NewEntity save(NewEntity entity);
  Optional<NewEntity> findById(String id);
}

// 3. Implement use case
@Service
public class NewEntityService implements NewEntityUseCase {

  private final NewEntityRepository repository;
  private final DomainEventPublisher eventPublisher;

  @Override
  public NewEntity createEntity(CreateEntityCommand command) {
    // Business logic here
    NewEntity entity = new NewEntity(/*...*/);
    NewEntity saved = repository.save(entity);
    eventPublisher.publish(new EntityCreatedEvent(saved));
    return saved;
  }
}

```

#### Step 4: Infrastructure Layer

```java
// 1. Web controller (inbound adapter)
@RestController
public class NewEntityController {

  private final NewEntityUseCase useCase;

  @PostMapping("/api/v1/entities")
  public ResponseEntity<EntityResponse> createEntity(@RequestBody EntityRequest request) {
    CreateEntityCommand command = new CreateEntityCommand(request.name());
    NewEntity entity = useCase.createEntity(command);
    return ResponseEntity.status(201).body(new EntityResponse(entity));
  }
}

// 2. Repository implementation (outbound adapter)
@Repository
public class ArcadeEntityRepository implements NewEntityRepository {
  // ArcadeDB-specific implementation
}

```

#### Step 5: Testing

```java
// Unit test for service
class NewEntityServiceTest {

  @Test
  void createEntity_shouldReturnEntity_whenValidCommand() {
    // Given
    CreateEntityCommand command = new CreateEntityCommand("test");
    when(repository.save(any())).thenReturn(expectedEntity);

    // When
    NewEntity result = service.createEntity(command);

    // Then
    assertThat(result).isEqualTo(expectedEntity);
    verify(eventPublisher).publish(any(EntityCreatedEvent.class));
  }
}

// Integration test for controller
class NewEntityControllerTest {

  @Test
  void createEntity_shouldReturn201_whenValidRequest() {
    // JavalinTest integration testing
  }
}

```

### 2. Testing Strategy

#### Test Structure

```
src/test/java/
├── it/robfrank/linklift/
│   ├── adapter/
│   │   ├── in/web/              # Controller tests
│   │   └── out/persistence/     # Repository tests
│   ├── application/
│   │   ├── domain/
│   │   │   ├── model/          # Domain entity tests
│   │   │   └── service/        # Service layer tests
│   │   └── port/               # Port interface tests
│   └── integration/            # End-to-end tests
```

#### Test Naming Convention

```java
// Pattern: methodName_shouldBehavior_whenCondition
@Test
void createLink_shouldReturnLink_whenValidDataProvided() {
  // Test implementation
}

@Test
void getLink_shouldThrowException_whenLinkNotFound() {
  // Test implementation
}

```

#### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=NewLinkServiceTest

# Run specific test method
mvn test -Dtest=NewLinkServiceTest#createLink_shouldReturnLink_whenValidDataProvided

# Run tests with coverage (if configured)
mvn test jacoco:report
```

### 3. Database Development

#### ArcadeDB Setup and Management

```bash
# Start database
docker run -d --name arcadedb \
  -p 2480:2480 -p 2424:2424 \
  -e JAVA_OPTS="-Darcadedb.server.rootPassword=playwithdata" \
  arcadedata/arcadedb:25.6.1

# Access ArcadeDB Studio (Web UI)
open http://localhost:2480

# Connect via console
docker exec -it arcadedb /opt/arcadedb/bin/console.sh
```

#### Database Schema Commands

```sql
-- Connect to database
CONNECT REMOTE:localhost/linklift root playwithdata

-- Create vertex types
CREATE VERTEX TYPE Link;

-- Create properties with constraints
CREATE PROPERTY Link.id STRING (MANDATORY TRUE, NOTNULL TRUE);
CREATE PROPERTY Link.url STRING (MANDATORY TRUE, NOTNULL TRUE);
CREATE PROPERTY Link.title STRING;
CREATE PROPERTY Link.description STRING;
CREATE PROPERTY Link.extractedAt DATETIME_SECOND;
CREATE PROPERTY Link.contentType STRING;

-- Create indexes
CREATE INDEX ON Link (id) UNIQUE;
CREATE INDEX ON Link (url) UNIQUE;

-- Query examples
SELECT FROM Link ORDER BY extractedAt DESC LIMIT 10;
SELECT COUNT(*) FROM Link;
```

### 4. API Development and Testing

#### Using cURL for API Testing

```bash
# Create a new link
curl -X PUT http://localhost:7070/api/v1/link \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://example.com",
    "title": "Example Site",
    "description": "An example website"
  }'

# Get links with pagination
curl "http://localhost:7070/api/v1/links?page=0&size=10&sortBy=title&sortDirection=ASC"

# Test error handling
curl -X PUT http://localhost:7070/api/v1/link \
  -H "Content-Type: application/json" \
  -d '{"url": "", "title": "", "description": ""}'
```

#### Using Postman/Insomnia Collections

Create collections with:

- Environment variables for base URL
- Pre-request scripts for data setup
- Tests for response validation
- Examples for different scenarios

## Debugging and Troubleshooting

### Common Issues

#### 1. Application Won't Start

```bash
# Check Java version
java -version

# Check Maven version
mvn -version

# Verify port availability
lsof -i :7070

# Check application logs
tail -f logs/application.log
```

#### 2. Database Connection Issues

```bash
# Check ArcadeDB container status
docker ps | grep arcadedb

# View ArcadeDB logs
docker logs arcadedb

# Test database connectivity
curl http://localhost:2480/api/v1/server

# Restart database
docker restart arcadedb
```

#### 3. Test Failures

```bash
# Run tests with verbose output
mvn test -X

# Run single failing test with debug
mvn test -Dtest=FailingTest -Dmaven.surefire.debug

# Generate test report
mvn surefire-report:report
open target/site/surefire-report.html
```

### Debugging Tools

#### Application Debugging

```bash
# Start application with remote debugging
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
  -jar target/linklift-1.0-SNAPSHOT.jar

# Connect IDE debugger to localhost:5005
```

#### Database Debugging

- **ArcadeDB Studio**: http://localhost:2480 (GUI interface)
- **Console Access**: `docker exec -it arcadedb /opt/arcadedb/bin/console.sh`
- **Query Performance**: Use `EXPLAIN` command for query analysis

#### Logging Configuration

Edit `src/main/resources/logback.xml`:

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <logger name="it.robfrank.linklift" level="DEBUG" />
    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```

## Performance Optimization

### JVM Tuning for Development

```bash
export JAVA_OPTS="-Xmx1g -Xms512m"
export JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC"
export JAVA_OPTS="$JAVA_OPTS -XX:+PrintGCDetails" # For GC monitoring
```

### Database Performance

```sql
-- Monitor slow queries
SELECT * FROM (
    SELECT *, @rid FROM Link
    WHERE extractedAt > sysdate() - 7
) ORDER BY extractedAt DESC;

-- Analyze index usage
EXPLAIN SELECT FROM Link WHERE url = 'https://example.com';
```

### Application Performance Monitoring

- Monitor response times using Javalin request logging
- Use Java profiling tools (VisualVM, JProfiler)
- Monitor database connection pool usage

## Contributing Guidelines

### Code Review Checklist

- [ ] Follows hexagonal architecture principles
- [ ] Has appropriate test coverage (unit + integration)
- [ ] Includes proper error handling
- [ ] Uses consistent naming conventions
- [ ] No business logic in infrastructure layer
- [ ] Immutable domain objects where appropriate
- [ ] Domain events published for significant business actions
- [ ] Database operations are transactional
- [ ] API endpoints follow RESTful conventions
- [ ] Documentation updated (if applicable)

### Git Workflow

```bash
# Create feature branch
git checkout -b feature/new-feature-name

# Make changes with good commit messages
git commit -m "feat(domain): add link validation logic"

# Ensure tests pass before pushing
mvn test

# Push and create pull request
git push origin feature/new-feature-name
```

### Commit Message Format

```
type(scope): description

feat(api): add link search endpoint
fix(db): resolve connection pool exhaustion
docs(guide): update setup instructions
test(service): add link validation tests
refactor(adapter): simplify repository interface
```

## IDE Setup

### IntelliJ IDEA Configuration

1. **Import Project**: File → Open → Select `pom.xml`
2. **Java Version**: Set Project SDK to Java 17+
3. **Code Style**: Import `.editorconfig` if available
4. **Run Configuration**: Create application run config for `Application.java`
5. **Test Configuration**: JUnit 5 runner configuration

### Eclipse Configuration

1. **Import Project**: File → Import → Existing Maven Projects
2. **Java Build Path**: Ensure correct JDK version
3. **Maven Integration**: Install m2e plugin if needed
4. **Formatter**: Configure Java code formatter

### VS Code Configuration

1. **Extensions**: Install Java Extension Pack, Spring Boot Extension Pack
2. **Settings**: Configure Java home and Maven settings
3. **Launch Configuration**: Create `.vscode/launch.json` for debugging

## Advanced Development

### Adding New Aggregate Roots

When adding new business entities that don't relate to Link:

1. Create new package under `application/domain/model/`
2. Define new repository ports under `application/port/out/`
3. Define new use cases under `application/port/in/`
4. Implement services under `application/domain/service/`
5. Create adapters under `adapter/out/persistence/`
6. Add controllers under `adapter/in/web/`

### Event System Extension

```java
// 1. Create new domain event
public class NewBusinessEvent extends DomainEvent {
    private final BusinessData data;
}

// 2. Add event subscriber
eventPublisher.subscribe(NewBusinessEvent.class, event -> {
    // Handle event (logging, notifications, etc.)
});

// 3. Publish from service
eventPublisher.publish(new NewBusinessEvent(businessData));
```

### Database Migration Strategy

1. Create migration scripts in `src/main/resources/db/migration/`
2. Version migrations: `V1__initial_schema.sql`, `V2__add_indexes.sql`
3. Test migrations on copy of production data
4. Document breaking changes

This developer guide should help you navigate the LinkLift codebase and contribute effectively. For additional questions, refer to the [Architecture Documentation](ARCHITECTURE.md) or consult with the development team.
