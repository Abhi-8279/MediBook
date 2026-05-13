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

This repo now includes a root-level [render.yaml](../render.yaml) Blueprint that deploys:

- `MediBook-Frontend` as a Render static site
- `api-gateway` as the public web service
- Eureka, MySQL, RabbitMQ, and each Spring Boot microservice as private services
- Redis-compatible cache as a Render Key Value instance

Important tradeoff: Render free instances are not available for private services, so the full microservice stack is a paid deployment.

### How to use it

1. Push this repository to GitHub, GitLab, or Bitbucket.
2. In Render, choose `New > Blueprint`.
3. Connect the repo and select the generated `render.yaml`.
4. During setup, provide `ADMIN_PASSWORD` when prompted.
5. After the first deploy, update any optional production secrets in the Render dashboard such as Google OAuth, SMTP, and Razorpay keys.

### Default public URLs

- Frontend: `https://medibook-frontend.onrender.com`
- API Gateway: `https://medibook-api.onrender.com`

If you change the Render service names or attach custom domains, update the matching environment variables in `render.yaml`.
