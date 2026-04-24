# MediBook Schedule Service

This service implements the MediBook availability and scheduling domain from your case study. It owns provider
time slots, public slot discovery, booking-state transitions, bulk slot creation, and recurring slot generation.

## Requirements covered

- Provider self-service slot creation and updates
- Bulk slot creation for multiple dates
- Daily, weekly, and custom recurring slot generation
- Blocking and unblocking slots for leave or personal unavailability
- Public real-time lookup of unblocked and unbooked provider slots
- Internal slot booking and release hooks for later `appointment-service` integration
- Optimistic locking on slot state changes to reduce double-booking risk
- Provider availability sync hook back to `provider-service`
- Health endpoint and OpenAPI docs

## Endpoints

- `POST /api/v1/schedules/slots`
- `POST /api/v1/schedules/slots/bulk`
- `POST /api/v1/schedules/slots/recurring`
- `GET /api/v1/schedules/slots/{slotId}`
- `GET /api/v1/schedules/providers/{providerId}/slots`
- `GET /api/v1/schedules/me/slots`
- `PUT /api/v1/schedules/slots/{slotId}`
- `PUT /api/v1/schedules/slots/{slotId}/block`
- `PUT /api/v1/schedules/slots/{slotId}/unblock`
- `DELETE /api/v1/schedules/slots/{slotId}`
- `POST /api/v1/schedules/internal/slots/{slotId}/book`
- `POST /api/v1/schedules/internal/slots/{slotId}/release`
- `GET /api/v1/schedules/internal/slots/{slotId}`
- `GET /api/v1/schedules/internal/providers/{providerId}/slots/available`

## Notes

- Guests and patients only see slots that are not blocked and not booked.
- Providers can only manage their own slots.
- Slot creation and updates reject overlapping time ranges for the same provider on the same date.
- Booking and release are reserved for internal service-to-service traffic using the configured internal API key.
- Provider availability is synced as a best-effort update to `provider-service` whenever future visible slots change.

## Local run

Set these environment variables if you do not want the defaults from `application.yml`:

- `MYSQL_URL`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`
- `AUTH_SERVICE_URL`
- `PROVIDER_SERVICE_URL`
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

- `target/schedule-service-0.0.1-SNAPSHOT-exec.jar`
