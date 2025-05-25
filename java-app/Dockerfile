# Build stage
FROM maven:3.8.7-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package assembly:single

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the fat JAR with all dependencies
COPY --from=build /app/target/*-jar-with-dependencies.jar ./app.jar

# Create data directory
RUN mkdir -p /data/parquet

# Run the application
CMD ["java", "-jar", "app.jar"]
