# MediBook API Gateway

This service is the single entry point for the MediBook microservices system. It now registers with Eureka and routes
to downstream services through service discovery by default, while still allowing explicit URL overrides through
environment variables.

## What it does

- Routes all `/api/v1/*` traffic to the right downstream service
- Centralizes CORS handling for your frontend or MVC layer
- Exposes health and gateway actuator endpoints
- Adds a request ID header when a client does not send one
- Uses Eureka service discovery by default and environment-variable overrides when you need fixed URLs

## Current service map

- `/api/v1/auth/**` -> `auth-service`
- `/api/v1/providers/**` -> `provider-service`
- `/api/v1/schedules/**` -> `schedule-service`
- `/api/v1/appointments/**` -> `appointment-service`
- `/api/v1/payments/**` -> `payment-service`
- `/api/v1/reviews/**` -> `review-service`
- `/api/v1/notifications/**` -> `notification-service`
- `/api/v1/records/**` -> `record-service`

Run `eureka-server` first, then start the gateway and whichever backend services you want registered.

## Default ports

- `api-gateway`: `8080`
- `auth-service`: `8081`
- `provider-service`: `8082`
- `schedule-service`: `8083`
- `appointment-service`: `8084`
- `payment-service`: `8085`
- `review-service`: `8086`
- `notification-service`: `8087`
- `record-service`: `8088`

## Important environment variables

- `SERVER_PORT`
- `EUREKA_SERVER_URL`
- `CORS_ALLOWED_ORIGINS`
- `AUTH_SERVICE_URL`
- `PROVIDER_SERVICE_URL`
- `SCHEDULE_SERVICE_URL`
- `APPOINTMENT_SERVICE_URL`
- `PAYMENT_SERVICE_URL`
- `REVIEW_SERVICE_URL`
- `NOTIFICATION_SERVICE_URL`
- `RECORD_SERVICE_URL`

## Local run

```powershell
..\.tools\apache-maven-3.9.9\bin\mvn.cmd clean test
..\.tools\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run
```

If you want to override the auth route locally instead of using Eureka:

```powershell
$env:AUTH_SERVICE_URL="http://localhost:8081"
..\.tools\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run
```

Health check:

- `GET /actuator/health`

Gateway route listing:

- `GET /actuator/gateway/routes`

## Docker

Build the image from the `api-gateway` folder:

```powershell
docker build -t medibook/api-gateway .
```

Jar output:

- `target/api-gateway-0.0.1-SNAPSHOT-exec.jar`
