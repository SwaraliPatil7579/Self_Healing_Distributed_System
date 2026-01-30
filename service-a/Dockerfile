FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the pre-built JAR file
COPY target/service-a-1.0.0.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]