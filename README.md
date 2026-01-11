[![Codacy Badge](https://app.codacy.com/project/badge/Grade/78972e21471a44e794375fe00ac862ea)](https://app.codacy.com/gh/robfrank/linklift/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Webapp CI](https://github.com/robfrank/linklift/actions/workflows/webapp-ci.yml/badge.svg)](https://github.com/robfrank/linklift/actions/workflows/webapp-ci.yml)

# LinkLift

A modern link management system built with Java and hexagonal architecture principles.

## Overview

LinkLift is a RESTful web service for managing web links with comprehensive CRUD operations. It features a clean, maintainable architecture that separates business logic from infrastructure concerns, making it highly testable and adaptable to changing requirements.

### Key Features

- **🔗 Link Management**: Create and list web links with metadata
  - **🔍 Vector Search**: Semantic search powered by Ollama embeddings and ArcadeDB vector index
- **🏗️ Clean Architecture**: Hexagonal architecture with strict layer separation
- **📊 Pagination & Sorting**: Efficient data retrieval with flexible sorting options
- **⚡ Event-Driven**: Domain events for loose coupling and extensibility
- **🧪 Comprehensive Testing**: Unit, integration, and optional E2E tests with real Ollama
- **🛡️ Error Handling**: Centralized exception handling with meaningful error codes
- **🔄 Database Agnostic**: Repository pattern enables flexible data storage

## Quick Start

### Prerequisites

- **Java 17+** - [Download here](https://adoptium.net/)
- **Maven 3.8+** - [Installation guide](https://maven.apache.org/install.html)
- **Docker & Docker Compose** - [Get Docker](https://docs.docker.com/get-docker/)

### Installation

1. **Clone the repository**

   ```bash
   git clone <repository-url>
   cd linklift
   ```

2. **Start with Docker Compose** (Recommended)

   ```bash
   docker-compose up -d
   ```

3. **Verify installation**

   ```bash
   curl http://localhost:7070/up
   # Expected: OK

   curl http://localhost:7070/api/v1/links
   # Expected: {"data": {"content": [], ...}, "message": "Links retrieved successfully"}
   ```

### Alternative: Manual Setup

```bash
# 1. Start ArcadeDB
docker run -d --name arcadedb \
  -p 2480:2480 -p 2424:2424 \
  -e JAVA_OPTS="-Darcadedb.server.rootPassword=playwithdata" \
  arcadedata/arcadedb:25.7.1

# 2. Build and run application
mvn clean package
java -jar target/linklift-1.0-SNAPSHOT.jar
```

## Usage Examples

### Create a Link

```bash
curl -X PUT http://localhost:7070/api/v1/link \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://github.com",
    "title": "GitHub",
    "description": "The world'\''s leading software development platform"
  }'
```

**Response:**

```json
{
  "link": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "url": "https://github.com",
    "title": "GitHub",
    "description": "The world's leading software development platform",
    "extractedAt": "2025-08-15T18:12:39",
    "contentType": "text/html"
  },
  "status": "Link received"
}
```

### List Links with Pagination

```bash
# Basic listing
curl http://localhost:7070/api/v1/links

# With pagination and sorting
curl "http://localhost:7070/api/v1/links?page=0&size=10&sortBy=title&sortDirection=ASC"
```

**Response:**

```json
{
  "data": {
    "content": [...],
    "page": 0,
    "size": 10,
    "totalElements": 25,
    "totalPages": 3,
    "hasNext": true,
    "hasPrevious": false
  },
  "message": "Links retrieved successfully"
}
```

## Architecture

LinkLift follows **Hexagonal Architecture** (Ports and Adapters) with clear separation between:

- **Domain Layer**: Pure business logic and rules
- **Application Layer**: Use cases and service coordination
- **Infrastructure Layer**: External adapters (web, database, events)

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Web Layer     │    │ Application     │    │   Persistence   │
│  (Javalin)      │◄───┤   Services      ├───►│   (ArcadeDB)    │
│                 │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                               │
                       ┌─────────────────┐
                       │  Domain Model   │
                       │  (Entities &    │
                       │   Events)       │
                       └─────────────────┘
```

### Technology Stack

- **☕ Java 17**: Modern language features and performance
- **🚀 Javalin**: Lightweight, fast web framework
- **🗄️ ArcadeDB**: Multi-model database (Graph, Document, Key-Value)
- **🔨 Maven**: Build automation and dependency management
- **🧪 JUnit 5**: Modern testing framework with comprehensive assertions
- **🐳 Docker**: Containerization for consistent environments

## Web Interface

After starting the application with Docker Compose, you can access the web UI at:

```
http://localhost:80
```

Or simply:

```
http://localhost
```

The web interface allows you to view and add new links through a user-friendly React interface.

### React Frontend Development

The web UI is built with React. If you want to develop the frontend separately:

```bash
# Navigate to the webapp directory
cd webapp

# Install dependencies
npm install

# Start development server
npm start

# Run tests
npm test

# Build for production
npm run build
```

The React app includes:

- Component tests with React Testing Library
- API mocking for isolated testing
- Material UI for component styling

## API Reference

### Endpoints Overview

| Method | Endpoint        | Description            | Status |
| ------ | --------------- | ---------------------- | ------ |
| GET    | `/up`           | Health check           | ✅     |
| PUT    | `/api/v1/link`  | Create new link        | ✅     |
| GET    | `/api/v1/links` | List links (paginated) | ✅     |

### Query Parameters for `/api/v1/links`

| Parameter       | Type    | Default       | Description                                                       |
| --------------- | ------- | ------------- | ----------------------------------------------------------------- |
| `page`          | Integer | 0             | Page number (0-based)                                             |
| `size`          | Integer | 20            | Items per page (max: 100)                                         |
| `sortBy`        | String  | "extractedAt" | Sort field: id, url, title, description, extractedAt, contentType |
| `sortDirection` | String  | "DESC"        | Sort direction: ASC, DESC                                         |

### Error Handling

All errors return a consistent JSON format:

```json
{
  "status": 400,
  "code": 1001,
  "message": "Validation error",
  "fieldErrors": {
    "url": "URL cannot be empty"
  },
  "path": "/api/v1/link",
  "timestamp": "2025-08-15T18:12:39"
}
```

**Common HTTP Status Codes:**

- `200` - Success
- `201` - Created
- `400` - Bad Request (validation errors)
- `409` - Conflict (duplicate URL)
- `500` - Internal Server Error

## Development

### Build Commands

```bash
# Clean and build
mvn clean package

# Run tests
mvn test

# Run specific test
mvn test -Dtest=NewLinkServiceTest

# Run with coverage (if configured)
mvn test jacoco:report
```

### Project Structure

```
src/
├── main/java/it/robfrank/linklift/
│   ├── Application.java         # Main entry point
│   ├── adapter/                 # Infrastructure adapters
│   │   ├── in/web/             # REST controllers
│   │   └── out/                # Database, event adapters
│   ├── application/            # Application layer
│   │   ├── domain/             # Business logic
│   │   └── port/               # Interface definitions
│   └── config/                 # Configuration
└── test/                       # Test classes (mirrors main structure)
```

### Testing

The project includes comprehensive testing at multiple levels:

**Test Categories:**

- **Unit Tests**: Fast, isolated tests for business logic
- **Integration Tests**: Tests with real database interactions using Testcontainers
- **Controller Tests**: API endpoint testing using JavalinTest
- **E2E Tests**: Optional end-to-end tests with real Ollama embeddings (slow, requires Docker)

**Running Tests:**

```bash
# Run all tests (excludes E2E tests by default)
mvn test

# Run specific test
mvn test -Dtest=BackfillEmbeddingsServiceTest

# View test reports
open target/surefire-reports/index.html
```

**E2E Testing with Real Ollama:**

The optional E2E tests validate the complete vector search workflow with real Ollama embeddings:

- Uses Testcontainers for ArcadeDB and Ollama
- Validates actual semantic similarity (not fake embeddings)
- Verifies embedding dimensions match real model output
- **Prerequisites**: Docker with ~400MB available for Ollama image
- **Execution time**: ~2-3 minutes per test suite
- **Use case**: Pre-release validation, embedding logic changes

```bash
# Run E2E tests (requires Docker)
mvn test -Pe2e-tests
```

**Testing Strategy:**

- **Development**: Use fast integration tests with fake embeddings
- **Pre-release**: Run E2E tests to validate real Ollama integration
- **CI/CD**: E2E tests run on main branch only (not on PRs)

## Configuration

### Environment Variables

| Variable                 | Default   | Description              |
| ------------------------ | --------- | ------------------------ |
| `linklift.arcadedb.host` | localhost | ArcadeDB server hostname |

### Database Configuration

**ArcadeDB Connection:**

- Host: localhost:2480 (HTTP API)
- Database: linklift
- Username: root
- Password: playwithdata

**Web UI:** http://localhost:2480 (ArcadeDB Studio)

## Documentation

- **[📖 API Reference](API.md)** - Complete API documentation with examples
- **[🏗️ Architecture Guide](ARCHITECTURE.md)** - System design and architectural decisions
- **[👨‍💻 Developer Guide](DEVELOPER_GUIDE.md)** - Setup and development workflow

## Contributing

We welcome contributions! Please follow these steps:

1. **Fork the repository**
2. **Create feature branch**: `git checkout -b feature/amazing-feature`
3. **Make changes with tests**
4. **Ensure tests pass**: `mvn test`
5. **Commit with clear message**: `git commit -m "feat: add amazing feature"`
6. **Push to fork**: `git push origin feature/amazing-feature`
7. **Create Pull Request**

### Development Guidelines

- ✅ Follow hexagonal architecture principles
- ✅ Write tests for all new features
- ✅ Use meaningful commit messages ([Conventional Commits](https://conventionalcommits.org/))
- ✅ Update documentation as needed
- ✅ Ensure code passes style checks

## Stopping the Services

```bash
# Stop all services
docker-compose down
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support & Community

**Getting Help:**

- 📚 Check the [Developer Guide](DEVELOPER_GUIDE.md) for detailed setup
- 🐛 Create an [Issue](../../issues) for bugs or feature requests
- 💬 Start a [Discussion](../../discussions) for questions

---

**Built with ❤️ using modern Java, clean architecture principles, and developer-friendly tools.**

LinkLift is designed to be a solid foundation for link management that can evolve with your needs while maintaining clean architecture and high code quality.
