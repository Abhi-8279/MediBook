# MediBook Review Service

This service implements the MediBook review and rating domain from your case study. It owns patient-written provider
reviews, per-appointment uniqueness, provider rating aggregation, anonymous review support, and moderation workflows.

## Requirements covered

- One review per completed appointment, enforced by a unique `appointmentId`
- Patient review submission with star rating, comment, and anonymous option
- Review updates and deletes for the owning patient, plus admin moderation delete
- Provider-facing review retrieval and inappropriate-review flagging for admin follow-up
- Admin review listing and moderation visibility
- Provider average rating and review-count recomputation after every write
- Public provider review browsing plus public average-rating and review-count lookup
- Health endpoint and OpenAPI docs

## Endpoints

- `POST /api/v1/reviews`
- `GET /api/v1/reviews`
- `GET /api/v1/reviews/providers/{providerId}`
- `GET /api/v1/reviews/providers/{providerId}/avg-rating`
- `GET /api/v1/reviews/providers/{providerId}/count`
- `GET /api/v1/reviews/appointments/{appointmentId}`
- `GET /api/v1/reviews/me`
- `GET /api/v1/reviews/patients/{patientId}`
- `PUT /api/v1/reviews/{reviewId}`
- `PUT /api/v1/reviews/{reviewId}/flag`
- `DELETE /api/v1/reviews/{reviewId}`
- `GET /api/v1/reviews/internal/providers/{providerId}/avg-rating`
- `GET /api/v1/reviews/internal/providers/{providerId}/count`

## Notes

- Reviews are only accepted when the linked appointment is `COMPLETED` and belongs to the authenticated patient.
- Anonymous reviews still retain the patient ID internally, but non-admin readers do not see it in API responses.
- Provider flagging is moderation-only metadata; flagged reviews remain visible until an admin deletes them.
- Average rating and review count are exposed as separate endpoints to match the case study resource contract.
- Average rating sync is pushed to `provider-service` after create, update, and delete operations.

## Local run

Set these environment variables if you do not want the defaults from `application.yml`:

- `MYSQL_URL`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`
- `AUTH_SERVICE_URL`
- `APPOINTMENT_SERVICE_URL`
- `PROVIDER_SERVICE_URL`
- `INTERNAL_API_KEY`
- `INTERNAL_API_KEY_HEADER`
- `SERVER_PORT`

Build and run with:

```powershell
..\.tools\apache-maven-3.9.9\bin\mvn.cmd clean test
..\.tools\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run
```

OpenAPI is available at `/swagger-ui.html`.

Docker or direct jar output:

- `target/review-service-0.0.1-SNAPSHOT-exec.jar`
