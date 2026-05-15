# MediBook

This repository contains the MediBook backend microservices, API gateway, and Eureka service discovery server.

## Services

- `eureka-server`
- `api-gateway`
- `auth-service`
- `provider-service`
- `schedule-service`
- `appointment-service`
- `payment-service`
- `review-service`
- `notification-service`
- `record-service`

## Local Docker Startup

The repository now includes `docker-compose.yml` so you can bring the backend up as a single stack.

```powershell
docker compose up --build
```

Main endpoints:

- Gateway: `http://localhost:8080`
- Eureka: `http://localhost:8761`
- Auth Swagger: `http://localhost:8081/swagger-ui.html`

## Notes

- All services register with Eureka through `EUREKA_SERVER_URL`.
- The gateway uses direct service URLs by default for local development, and deployment manifests provide explicit
  downstream URLs for Docker and Render.
- Most internal service-to-service calls still use explicit service base URLs, and `docker-compose.yml` provides those
  environment variables so the full backend can run together cleanly.

## Deploy To Render

Set up Render services manually from the dashboard. This repository no longer includes a Render Blueprint file.
