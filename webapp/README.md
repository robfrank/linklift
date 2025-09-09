# LinkLift Web Application

This is the frontend application for LinkLift, a link management system built with React.

## Technologies Used

-   React 18
-   React Router v6
-   Material UI v7
-   Axios for API communication
-   Webpack for bundling
-   Babel for transpilation
-   Jest and React Testing Library for testing

## Development

### Prerequisites

-   Node.js (v18+)
-   npm (v9+)

### Installation

```bash
# Install dependencies
npm install
```

### Development Server

```bash
# Start development server
npm start
```

This will start the development server at [http://localhost:3000](http://localhost:3000).

### Building for Production

```bash
# Build for production
npm run build
```

This will create a `dist` directory with the compiled application.

## Docker Integration

This frontend application is designed to work in a Docker environment:

-   A Dockerfile is provided to build the application into an Nginx container
-   The Nginx configuration proxies API requests to the backend service
-   The application is integrated into the main docker-compose.yml file

## API Integration

The application communicates with the LinkLift backend API:

-   Endpoint: `/api/v1/link`
-   Request format:
    ```json
    {
        "url": "https://example.com",
        "title": "Example Website",
        "description": "A brief description of the website"
    }
    ```

## Available Pages

-   **Home**: Landing page with an introduction to LinkLift
-   **Add Link**: Form to add a new link to the system
-   **404 Not Found**: Custom page for invalid routes

## Testing

The webapp uses Jest and React Testing Library for tests. To run tests:

```bash
# Run all tests
npm test

# Run tests in watch mode
npm run test:watch

# Generate test coverage report
npm run test:coverage
```

The test suite includes:

-   Component tests for all UI components
-   API service tests
-   Integration tests for key user flows
-   Route testing

Test files are located alongside the components in `__tests__` directories.

## Continuous Integration

This project uses GitHub Actions for continuous integration. The webapp-ci workflow:

1. Runs on every push or pull request that affects the webapp directory
2. Installs dependencies
3. Runs all tests
4. Builds the project

You can view the CI status on the GitHub repository or check the badge in the main README.
