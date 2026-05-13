# MediBook Appointment Service

This service implements the MediBook appointment lifecycle domain from your case study. It owns appointment booking,
retrieval, cancellation, rescheduling, completion, and status transitions while orchestrating slot reservation with
`schedule-service`.

## Requirements covered

- Patient appointment booking against provider schedule slots
- Patient appointment history and upcoming appointment views
- Provider appointment views, including today's schedule
- Appointment rescheduling to another available slot for the same provider
- Appointment cancellation by patient, provider, or admin
- Provider/admin completion and no-show status transitions
- Provider appointment counts for dashboard analytics
- Internal hooks for later `review-service` and `record-service` integration
- Best-effort refund trigger hook for later `payment-service` integration
- Health endpoint and OpenAPI docs

## Endpoints

- `POST /api/v1/appointments`
- `GET /api/v1/appointments/{appointmentId}`
- `GET /api/v1/appointments/me`
- `GET /api/v1/appointments/me/upcoming`
- `GET /api/v1/appointments/provider/me`
- `GET /api/v1/appointments/provider/me/today`
- `GET /api/v1/appointments/provider/me/date`
- `GET /api/v1/appointments/admin`
- `GET /api/v1/appointments/providers/{providerId}`
- `GET /api/v1/appointments/providers/{providerId}/date/{date}`
- `GET /api/v1/appointments/providers/{providerId}/count`
- `PUT /api/v1/appointments/{appointmentId}/cancel`
- `PUT /api/v1/appointments/{appointmentId}/reschedule`
- `PUT /api/v1/appointments/{appointmentId}/complete`
- `PUT /api/v1/appointments/{appointmentId}/status`
- `GET /api/v1/appointments/internal/{appointmentId}`
- `GET /api/v1/appointments/internal/providers/{providerId}/count`
- `GET /api/v1/appointments/internal/patients/{patientId}/providers/{providerId}/completed/exists`

## Notes

- Booking requires a patient account and an available slot from `schedule-service`.
- Rescheduling is limited to another slot for the same provider, matching the case study.
- Cancellation releases the booked slot back to `schedule-service`.
- Refund triggering is implemented as a best-effort internal hook so this service remains runnable before
  `payment-service` is added.

## Local run

Set these environment variables if you do not want the defaults from `application.yml`:

- `MYSQL_URL`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`
- `AUTH_SERVICE_URL`
- `PROVIDER_SERVICE_URL`
- `SCHEDULE_SERVICE_URL`
- `PAYMENT_SERVICE_URL`
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

- `target/appointment-service-0.0.1-SNAPSHOT-exec.jar`
