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
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/reset-password`
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
- `NOTIFICATION_SERVICE_URL`
- `PASSWORD_RESET_FRONTEND_BASE_URL`
- `PASSWORD_RESET_EXPIRATION_MS`
- `INTERNAL_API_KEY`
- `INTERNAL_API_KEY_HEADER`
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `GOOGLE_OAUTH_REDIRECT_URI`
- `OAUTH2_REDIRECT_URI`

OAuth2 is optional for local startup. Google login turns on automatically when `GOOGLE_CLIENT_ID`
and `GOOGLE_CLIENT_SECRET` are set, or when you fill those values in
`src/main/resources/application-oauth.yml`.

Recommended local values:

- `GOOGLE_OAUTH_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google`
- `OAUTH2_REDIRECT_URI=http://localhost:5173/oauth2/redirect`

`src/main/resources/application-oauth.yml` is imported automatically when present, so you can keep
your local Google OAuth values there without needing an extra Spring profile.

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
