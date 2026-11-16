# Stage 1: build the React webapp
FROM node:26-alpine AS webapp
WORKDIR /app
COPY webapp/package*.json ./
RUN npm ci
COPY webapp/ ./
RUN npm run build

# Stage 2: build the shaded JAR
FROM eclipse-temurin:25-alpine AS backend
WORKDIR /build
COPY mvnw ./
COPY .mvn/ .mvn/
COPY pom.xml ./
COPY src/ src/
RUN ./mvnw -B -q -DskipTests package

# Stage 3: runtime
FROM eclipse-temurin:25-alpine@sha256:0c4c6300cc86efdf6454702336a0d60352e227f3a862e8ae9861f393f8f1ede9

# Update packages
RUN apk --no-cache -U upgrade

# Create a non-root user to run the application
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Create directory for the application
RUN mkdir -p /app && chown -R appuser:appgroup /app

# Application jar and webapp bundle
COPY --from=backend /build/target/linklift-*.jar /app/app.jar
COPY --from=webapp --chown=appuser:appgroup /app/dist /app/webapp
RUN chown appuser:appgroup /app/app.jar

ENV LINKLIFT_WEBAPP_DIR=/app/webapp

# Switch to non-root user
USER appuser

# Expose the application port
EXPOSE 7070

# Run the application
WORKDIR /app
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:--Xmx512M -XX:+UseZGC -XX:+ZGenerational -XX:+UseCompactObjectHeaders -XX:+UseCompressedOops} -jar app.jar"]
