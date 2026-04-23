FROM eclipse-temurin:21-jre

WORKDIR /app
COPY target/notification-service-0.0.1-SNAPSHOT-exec.jar app.jar

EXPOSE 8087

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
