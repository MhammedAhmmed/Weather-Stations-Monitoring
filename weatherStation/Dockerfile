# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/DDIA_project-1.0-SNAPSHOT.jar ./weather-station.jar

# default to localhost, but can be overridden at docker run
ENV BOOTSTRAP_SERVERS=localhost:9092

ENTRYPOINT ["java", "-jar", "weather-station.jar"]

