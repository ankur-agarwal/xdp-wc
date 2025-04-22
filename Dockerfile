FROM gradle:8.5-jdk11 AS build
WORKDIR /app
COPY . .
RUN gradle build --no-daemon

FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar ./app.jar
COPY --from=build /app/src/main/resources/lorem.txt ./lorem.txt

ENTRYPOINT ["java", "-jar", "app.jar"] 