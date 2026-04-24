# MediBook Auth Service

This service implements the MediBook case-study auth domain using Java, Spring Boot, and MySQL.

## Requirements covered

- User registration and login
- JWT-based authentication with refresh tokens
- Optional OAuth2 hooks for Google and GitHub login
- Role-based access for `PATIENT`, `PROVIDER`, and `ADMIN`
- Profile read/update
- Password change
- Account deactivation
- Admin-only user management endpoints
- Internal service-to-service user/token lookup endpoints
- Health endpoint and OpenAPI docs

## Endpoints

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/validate`
- `GET /api/v1/auth/profile`
- `PUT /api/v1/auth/profile`
- `PUT /api/v1/auth/password`
- `PUT /api/v1/auth/deactivate`
- `GET /api/v1/auth/admin/users`
- `GET /api/v1/auth/admin/users/{userId}`
- `PATCH /api/v1/auth/admin/users/{userId}/status`
- `POST /api/v1/auth/internal/tokens/validate`
- `GET /api/v1/auth/internal/users/{userId}`
- `GET /api/v1/auth/internal/users/by-email`

## Local run

Set these environment variables if you do not want the defaults from `application.yml`:

- `MYSQL_URL`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`
- `EUREKA_SERVER_URL`
- `JWT_SECRET`
- `INTERNAL_API_KEY`
- `INTERNAL_API_KEY_HEADER`
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `GITHUB_CLIENT_ID`
- `GITHUB_CLIENT_SECRET`
- `OAUTH2_REDIRECT_URI`

OAuth2 is optional for local startup. If you want Google/GitHub login, copy `src/main/resources/application-oauth.yml.example`
to your own config source and provide the required client IDs and secrets.

Build and run with the local Maven tool downloaded into the repo:

```powershell
..\.tools\apache-maven-3.9.9\bin\mvn.cmd clean test
..\.tools\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run
```

OpenAPI is available at `/swagger-ui.html`.

For internal service calls, send the configured API key header, for example:

```http
X-Internal-Api-Key: medibook-internal-key-change-me
```

## Notes

- Public self-registration allows `PATIENT` and `PROVIDER`. `ADMIN` is seeded from configuration.
- OAuth2 logins default to `PATIENT`; provider verification remains the responsibility of the provider-service/admin flow from the MediBook case study.
- Refresh tokens are persisted in MySQL and revoked on logout, password change, and deactivation.
- Other MediBook services should prefer the `/api/v1/auth/internal/*` endpoints instead of public user-facing endpoints.

Jar output:

- `target/auth-service-0.0.1-SNAPSHOT-exec.jar`
