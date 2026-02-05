# Technology Stack

## Backend

- **Language:** Java 25 (with preview features)
- **Framework:** Javalin 6.7.0
- **Database:** ArcadeDB 25.12.1 (Multi-model: Graph, Document, Vector)
- **AI/ML:** Ollama (Local embeddings via `all-minilm:l6-v2`)
- **JSON:** Jackson 2.21.0
- **Security:** Java-JWT 4.5.0, BCrypt 0.10.2

## Frontend

- **Framework:** React 19.1.0
- **Styling:** Material UI 7.0.2 (Emotion-based)
- **State Management:** Zustand 5.0.9
- **Routing:** React Router 7.6.0
- **Build Tool:** Webpack 5.99.6, Babel

## Infrastructure & DevOps

- **Build Tool:** Maven 3.8+
- **Containerization:** Docker, Docker Compose
- **Deployment:** Kamal
- **CI/CD:** GitHub Actions

## Testing

- **Unit/Integration:** JUnit 5, AssertJ, Mockito
- **Environment:** Testcontainers (ArcadeDB, Ollama)
- **API/HTTP:** WireMock, Javalin Testtools
- **Frontend Testing:** Jest, React Testing Library, MSW
