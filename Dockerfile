# Build stage
FROM gradle:8.5.0-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle build --no-daemon

# Runtime stage
FROM bitnami/spark:3.5.0
USER root

# Create directory for the application
WORKDIR /opt/spark/apps

# Copy the built JAR from the build stage
COPY --from=build /app/build/libs/*.jar /tmp

RUN chmod -R 777 /tmp \
    && chown -R 1001:1001 /tmp

RUN chmod -R 777 /opt/spark
RUN chown -R 1001:1001 /opt/spark

# Switch back to non-root user for security
USER 1001
