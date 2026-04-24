# MediBook Eureka Server

This service provides Netflix Eureka-based service discovery for the MediBook backend. Run it first so the API gateway
and all microservices can register themselves and resolve service instances from a single registry.

## What it enables

- Central service registry on port `8761`
- Instance registration for `api-gateway` and all backend microservices
- Gateway routing through service IDs such as `lb://auth-service`
- Health endpoint for deployment checks

## Configuration

Set these environment variables if you want to override the defaults:

- `SERVER_PORT`
- `EUREKA_HOST`

## Local run

```powershell
..\.tools\apache-maven-3.9.9\bin\mvn.cmd clean test
..\.tools\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run
```

The Eureka dashboard is available at `http://localhost:8761/`.

Jar output:

- `target/eureka-server-0.0.1-SNAPSHOT-exec.jar`
