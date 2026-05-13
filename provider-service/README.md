# MediBook Provider Service

This service implements the MediBook provider-profile domain from your case study. It owns provider registration,
professional profile management, verification status, public directory search, and rating aggregation hooks for the
rest of the microservices system.

## Requirements covered

- Provider profile registration after auth-service signup
- Search by name, specialization, and location
- Public browsing of verified providers
- Detailed provider profile retrieval
- Provider self-service profile updates
- Admin verification and rejection flow
- Availability flag management
- Rating aggregation update hook for later review-service integration
- Internal provider lookup endpoint for later schedule/appointment/payment/review services
- Health endpoint and OpenAPI docs

## Endpoints

- `POST /api/v1/providers/register`
- `GET /api/v1/providers`
- `GET /api/v1/providers/all`
- `GET /api/v1/providers/search`
- `GET /api/v1/providers/specialization/{specialization}`
- `GET /api/v1/providers/{providerId}`
- `GET /api/v1/providers/me`
- `POST /api/v1/providers/me/sync-auth-profile`
- `PUT /api/v1/providers/me`
- `PUT /api/v1/providers/{providerId}/verify`
- `PUT /api/v1/providers/{providerId}/availability`
- `DELETE /api/v1/providers/{providerId}`
- `GET /api/v1/providers/admin/specialization-counts`
- `GET /api/v1/providers/internal/users/{userId}`
- `PUT /api/v1/providers/internal/{providerId}/rating`
- `PUT /api/v1/providers/internal/{providerId}/availability`
- `GET /api/v1/providers/internal/specialization-counts`

## Notes

- Public directory endpoints only expose verified providers.
- A provider must already exist in `auth-service` with role `PROVIDER` before profile registration succeeds.
- Name, email, phone, and profile picture are denormalized from `auth-service` so search stays fast and independent.
- If a provider updates those identity fields in `auth-service`, they can resync this service via
  `POST /api/v1/providers/me/sync-auth-profile`.
- Rating and availability internal endpoints are meant for future `review-service` and `schedule-service` integration.

## Local run

Set these environment variables if you do not want the defaults from `application.yml`:

- `MYSQL_URL`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`
- `AUTH_SERVICE_URL`
- `INTERNAL_API_KEY`
- `INTERNAL_API_KEY_HEADER`
- `SERVER_PORT`

When Maven is available, build and run with:

```powershell
..\.tools\apache-maven-3.9.9\bin\mvn.cmd clean test
..\.tools\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run
```

OpenAPI is available at `/swagger-ui.html`.

For Docker or direct jar deployment, build output is written to:

- `target/provider-service-0.0.1-SNAPSHOT-exec.jar`
