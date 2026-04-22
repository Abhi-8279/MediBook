# MediBook Payment Service

This service implements the MediBook payment domain from your case study. It owns appointment payment processing,
refund handling, invoice generation, payment status tracking, and provider revenue aggregation.

## Requirements covered

- Appointment-linked payment processing with one payment record per appointment
- Support for `CARD`, `UPI`, `WALLET`, and `CASH` payment modes
- Payment status tracking with `PENDING`, `PAID`, `REFUNDED`, and `FAILED`
- Internal refund trigger endpoint for `appointment-service` cancellation flow
- Patient payment lookup by appointment and personal payment history
- Admin payment history filtering and manual status updates
- Invoice generation for completed appointments
- Provider revenue aggregation for dashboard use
- Health endpoint and OpenAPI docs

## Endpoints

- `POST /api/v1/payments/process`
- `GET /api/v1/payments/appointments/{appointmentId}`
- `GET /api/v1/payments/me`
- `GET /api/v1/payments/patients/{patientId}`
- `GET /api/v1/payments/history`
- `POST /api/v1/payments/{paymentId}/refund`
- `GET /api/v1/payments/{paymentId}/status`
- `PUT /api/v1/payments/{paymentId}/status`
- `GET /api/v1/payments/{paymentId}/invoice`
- `GET /api/v1/payments/providers/me/revenue`
- `GET /api/v1/payments/providers/{providerId}/revenue`
- `POST /api/v1/payments/internal/appointments/{appointmentId}/refund`
- `GET /api/v1/payments/internal/appointments/{appointmentId}`
- `GET /api/v1/payments/internal/providers/{providerId}/revenue`

## Notes

- IDs use UUID strings so this service stays consistent with the rest of your MediBook microservices.
- Online payment modes are recorded as `PAID` immediately in this implementation.
- `CASH` is treated as pay-at-clinic and starts in `PENDING` until an admin updates it.
- Invoice generation is allowed only after the linked appointment is marked `COMPLETED`.
- Revenue summaries include total paid revenue, pending amount, refunded amount, and monthly paid breakdowns.

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

- `target/payment-service-0.0.1-SNAPSHOT-exec.jar`
