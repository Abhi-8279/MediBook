# MediBook Record Service

This service implements the MediBook electronic medical record domain from your case study. It owns medical record
creation after completed appointments, patient/provider/admin record access, attachment updates, follow-up tracking,
and follow-up reminder dispatch hooks.

## Requirements covered

- Provider-created medical records linked one-to-one with completed appointments
- Record fields for diagnosis, prescription, clinical notes, optional attachment URL, and follow-up date
- Patient access to their own records
- Provider access to records they created
- Admin read-only audit access
- Record lookup by appointment, patient, provider, and record ID
- Update, attachment update, and delete within a configurable provider edit window
- Follow-up record retrieval and patient record counts
- Best-effort scheduled follow-up reminder dispatch hook for later `notification-service` integration
- Health endpoint and OpenAPI docs

## Endpoints

- `POST /api/v1/records`
- `GET /api/v1/records/{recordId}`
- `GET /api/v1/records/appointments/{appointmentId}`
- `GET /api/v1/records/me`
- `GET /api/v1/records/patients/{patientId}`
- `GET /api/v1/records/patients/{patientId}/count`
- `GET /api/v1/records/providers/me`
- `GET /api/v1/records/providers/{providerId}`
- `GET /api/v1/records/follow-ups`
- `PUT /api/v1/records/{recordId}`
- `PUT /api/v1/records/{recordId}/attachment`
- `DELETE /api/v1/records/{recordId}`
- `GET /api/v1/records/internal/appointments/{appointmentId}`
- `GET /api/v1/records/internal/follow-ups`
- `GET /api/v1/records/internal/patients/{patientId}/count`

## Notes

- A record can only be created for a completed appointment, and only by that appointment's provider.
- Provider edits, attachment updates, and deletes are limited by a configurable edit window after completion.
- Admin access is intentionally read-only, matching the case study.
- Follow-up reminder dispatch is implemented as a best-effort internal hook so this service remains runnable before
  `notification-service` is added.

## Local run

Set these environment variables if you do not want the defaults from `application.yml`:

- `MYSQL_URL`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`
- `AUTH_SERVICE_URL`
- `APPOINTMENT_SERVICE_URL`
- `PROVIDER_SERVICE_URL`
- `NOTIFICATION_SERVICE_URL`
- `INTERNAL_API_KEY`
- `INTERNAL_API_KEY_HEADER`
- `RECORD_EDIT_WINDOW_HOURS`
- `RECORD_FOLLOW_UP_CHECK_INTERVAL_MS`
- `SERVER_PORT`

Build and run with:

```powershell
..\.tools\apache-maven-3.9.9\bin\mvn.cmd clean test
..\.tools\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run
```

OpenAPI is available at `/swagger-ui.html`.

Docker or direct jar output:

- `target/record-service-0.0.1-SNAPSHOT-exec.jar`
