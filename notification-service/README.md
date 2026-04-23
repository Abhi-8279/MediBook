# MediBook Notification Service

This service implements the MediBook notification domain from your case study. It owns in-app, email, and SMS
notification dispatch, read/unread state tracking, unread badge counts, follow-up reminders, scheduled appointment
reminders, and admin bulk broadcasts to patients, providers, or all users.

## Requirements covered

- Notification records with recipient, type, title, message, channel, related ID, related type, read state, and sent time
- Single notification dispatch across `APP`, `EMAIL`, and `SMS` channels
- Booking, reminder, cancellation, payment, follow-up, and broadcast notification types
- Recipient inbox retrieval and unread badge count
- Mark-as-read and mark-all-read flows
- Follow-up reminder intake endpoint used by `record-service`
- Scheduled 24-hour and 1-hour appointment reminder dispatch via persisted reminder queue
- Admin bulk broadcast to patients, providers, or all active users
- Admin full notification listing with filters
- Health endpoint and OpenAPI docs

## Endpoints

- `GET /api/v1/notifications/me`
- `GET /api/v1/notifications/me/unread-count`
- `PUT /api/v1/notifications/me/read-all`
- `GET /api/v1/notifications/recipients/{recipientId}`
- `GET /api/v1/notifications/recipients/{recipientId}/unread-count`
- `PUT /api/v1/notifications/recipients/{recipientId}/read-all`
- `PUT /api/v1/notifications/{notificationId}/read`
- `DELETE /api/v1/notifications/{notificationId}`
- `POST /api/v1/notifications/bulk`
- `GET /api/v1/notifications`
- `POST /api/v1/notifications/internal/send`
- `POST /api/v1/notifications/internal/follow-up-reminders`
- `POST /api/v1/notifications/internal/appointment-reminders`
- `DELETE /api/v1/notifications/internal/appointment-reminders/{appointmentId}`

## Notes

- Every dispatched channel is stored as its own notification row because the entity tracks a single `channel` value.
- Email delivery uses SMTP when `NOTIFICATION_EMAIL_ENABLED=true`.
- SMS delivery uses a configurable provider webhook when `NOTIFICATION_SMS_ENABLED=true`.
- When email or SMS delivery is disabled, the service still records the notification and logs the attempted dispatch.
- Bulk broadcasts resolve recipients through `auth-service` internal APIs and skip inactive users.
- Appointment reminders are persisted and dispatched by a scheduled job inside this service.

## Local run

Set these environment variables if you do not want the defaults from `application.yml`:

- `MYSQL_URL`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`
- `AUTH_SERVICE_URL`
- `INTERNAL_API_KEY`
- `INTERNAL_API_KEY_HEADER`
- `NOTIFICATION_EMAIL_ENABLED`
- `NOTIFICATION_EMAIL_FROM`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_SMTP_AUTH`
- `MAIL_SMTP_STARTTLS_ENABLE`
- `NOTIFICATION_SMS_ENABLED`
- `NOTIFICATION_SMS_PROVIDER_URL`
- `NOTIFICATION_SMS_AUTH_TOKEN`
- `NOTIFICATION_SMS_SENDER_ID`
- `NOTIFICATION_REMINDER_DISPATCH_INTERVAL_MS`
- `SERVER_PORT`

Build and run with:

```powershell
..\.tools\apache-maven-3.9.9\bin\mvn.cmd clean test
..\.tools\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run
```

OpenAPI is available at `/swagger-ui.html`.

Docker or direct jar output:

- `target/notification-service-0.0.1-SNAPSHOT-exec.jar`
