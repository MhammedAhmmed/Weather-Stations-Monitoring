# Use official Maven image with Temurin JDK 21 (verified tag)
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package

# Use official Eclipse Temurin JRE image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/RainDetection-1.0-SNAPSHOT.jar /app/processor.jar
ENTRYPOINT ["java", "-jar", "processor.jar"]