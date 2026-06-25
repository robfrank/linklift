FROM eclipse-temurin:25-alpine@sha256:5ecfde8e5ecde5954ea3721155b345ef56c1d579b940c761318ad4c05959a151

# Update packages
RUN apk --no-cache -U upgrade

# Create a non-root user to run the application
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Create directory for the application
RUN mkdir -p /app && chown -R appuser:appgroup /app

# Copy the application jar
COPY maven/linklift-*.jar /app/app.jar

# Set proper permissions
RUN chown appuser:appgroup /app/app.jar

# Switch to non-root user
USER appuser

# Expose the application port
EXPOSE 7070

# Run the application
WORKDIR /app
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:--Xmx512M -XX:+UseZGC -XX:+ZGenerational -XX:+UseCompactObjectHeaders -XX:+UseCompressedOops} -jar app.jar"]
