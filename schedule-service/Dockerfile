FROM eclipse-temurin:21-jre

WORKDIR /app
COPY target/schedule-service-0.0.1-SNAPSHOT-exec.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
