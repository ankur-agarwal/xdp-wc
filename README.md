# Spark Word Count Application

This is a simple Spark application that performs word count on a sample lorem ipsum text file.

## Prerequisites

- Java 11
- Gradle 8.5+
- Docker (optional, for containerized execution)

## Running Locally

1. Build the application:
```bash
./gradlew build
```

2. Run the application:
```bash
./gradlew run
```

Or to run the JAR directly:
```bash
java -jar build/libs/spark-wordcount-1.0-SNAPSHOT.jar
```

## Running with Docker

1. Build the Docker image:
```bash
docker build -t spark-wordcount .
```

2. Run the container:
```bash
docker run spark-wordcount
```

## Output

The application will output the word count of the sample lorem ipsum text file, showing each word and its frequency. 