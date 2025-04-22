# Build stage
FROM gradle:8.5.0-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle build --no-daemon

# Runtime stage
FROM bitnami/spark:3.5.0
USER root

# Copy the built JAR from the build stage
COPY --from=build /app/build/libs/*.jar /opt/spark/apps/spark-hello-world.jar

# Set the working directory
WORKDIR /opt/spark

# Set the entrypoint to run the Spark application
ENTRYPOINT ["spark-submit", \
            "--class", "io.acceldata.SparkApp", \
            "/opt/spark/apps/spark-hello-world.jar"]

# Default command (can be overridden)
CMD [] 