# Use official JDK image (non-Alpine for full JFR support)
FROM eclipse-temurin:17-jdk

# Create app directory inside container
WORKDIR /app

# Copy the jar file into the image
COPY target/Bitcask-1.0-SNAPSHOT.jar /app/app.jar

# Run Java with JFR enabled
CMD ["java", "-XX:StartFlightRecording=duration=60s,filename=recording.jfr", "-jar", "/app/app.jar"]

