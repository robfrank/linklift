[![Codacy Badge](https://app.codacy.com/project/badge/Grade/78972e21471a44e794375fe00ac862ea)](https://app.codacy.com/gh/robfrank/linklift/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)

# Linklift

Linklift is a link management service built using a hexagonal (ports and adapters) architecture. It allows you to store, manage, and retrieve links with metadata.

## Getting Started

### Prerequisites

- Java 24 or later
- Maven 3.8 or later
- Docker and Docker Compose (for containerized testing)

## Building the Application

### Build JAR file

```bash
# Clean and package the application
mvn clean package
```

### Build Docker Image

```bash
# Build Docker image
mvn clean package -Pdocker
```

## Running with Docker Compose

Docker Compose provides an easy way to run the application with its dependencies.

```bash
# Start all services
docker-compose up -d
```

The application will be available at http://localhost:7070.

## Testing the API

You can test the API using curl:

```bash
# Create a new link
curl -X POST http://localhost:7070/api/v1/link \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://example.com",
    "title": "Example Website",
    "description": "This is an example website"
  }'
```

A successful response will look like:

```json
{
  "link": {
    "id": "...",
    "url": "https://example.com",
    "title": "Example Website",
    "description": "This is an example website",
    "createdAt": "..."
  },
  "status": "Link received"
}
```

## Stopping the Services

```bash
# Stop all services
docker-compose down
```
