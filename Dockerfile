# Stage 1: Build the provider
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

# Build the project
RUN mvn clean package -DskipTests

# Stage 2: Create the Keycloak image
FROM quay.io/keycloak/keycloak:26.0.0

# Copy the provider JAR from the builder stage
COPY --from=builder /app/target/keycloak-2fa-email-authenticator-*.jar /opt/keycloak/providers/

# Build Keycloak to include the provider
RUN /opt/keycloak/bin/kc.sh build

# Expose the port
EXPOSE 8080

# Entrypoint is inherited from the base image
