# Multi-stage Dockerfile for Value Investor Bot Backend

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Create directories for data and logs
RUN mkdir -p /app/data /app/logs

# Expose port
EXPOSE 8080

# Set environment variables
ENV SPRING_DATASOURCE_URL=jdbc:sqlite:/app/data/database.db
ENV SERVER_PORT=8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
