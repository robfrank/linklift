[![Codacy Badge](https://app.codacy.com/project/badge/Grade/78972e21471a44e794375fe00ac862ea)](https://app.codacy.com/gh/robfrank/linklift/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Webapp CI](https://github.com/robfrank/linklift/actions/workflows/webapp-ci.yml/badge.svg)](https://github.com/robfrank/linklift/actions/workflows/webapp-ci.yml)

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

The backend API will be available at http://localhost:7070.

## Accessing the Web UI

After starting the application with Docker Compose, you can access the web UI at:

```
http://localhost:80
```

Or simply:

```
http://localhost
```

The web interface allows you to view and add new links through a user-friendly interface.

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
