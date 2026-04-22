FROM eclipse-temurin:21-jre

WORKDIR /app
COPY target/record-service-0.0.1-SNAPSHOT-exec.jar app.jar

EXPOSE 8088

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
