# LinkLift Web Application

This is the frontend application for LinkLift, a link management system built with React.

## Technologies Used

- React 19
- React Router v7
- Material UI v7
- Axios for API communication
- Webpack for bundling
- Babel for transpilation

## Development

### Prerequisites

- Node.js (v18+)
- npm (v9+)

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

- A Dockerfile is provided to build the application into an Nginx container
- The Nginx configuration proxies API requests to the backend service
- The application is integrated into the main docker-compose.yml file

## API Integration

The application communicates with the LinkLift backend API:

- Endpoint: `/api/v1/link`
- Request format:
  ```json
  {
    "url": "https://example.com",
    "title": "Example Website",
    "description": "A brief description of the website"
  }
  ```

## Available Pages

- **Home**: Landing page with an introduction to LinkLift
- **Add Link**: Form to add a new link to the system
- **404 Not Found**: Custom page for invalid routes
