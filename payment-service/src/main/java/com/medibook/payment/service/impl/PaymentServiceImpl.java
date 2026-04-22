package com.medibook.payment.service.impl;

import com.medibook.payment.config.AppProperties;
import com.medibook.payment.dto.request.ProcessPaymentRequest;
import com.medibook.payment.dto.request.RefundPaymentRequest;
import com.medibook.payment.dto.request.UpdatePaymentStatusRequest;
import com.medibook.payment.dto.response.InvoiceResponse;
import com.medibook.payment.dto.response.MessageResponse;
import com.medibook.payment.dto.response.MonthlyRevenueItemResponse;
import com.medibook.payment.dto.response.PaymentResponse;
import com.medibook.payment.dto.response.PaymentStatusResponse;
import com.medibook.payment.dto.response.PlatformRevenueSummaryResponse;
import com.medibook.payment.dto.response.RevenueSummaryResponse;
import com.medibook.payment.entity.Payment;
import com.medibook.payment.enums.AppointmentStatus;
import com.medibook.payment.enums.PaymentMode;
import com.medibook.payment.enums.PaymentStatus;
import com.medibook.payment.enums.Role;
import com.medibook.payment.exception.PaymentConflictException;
import com.medibook.payment.exception.ResourceNotFoundException;
import com.medibook.payment.repository.MonthlyRevenueView;
import com.medibook.payment.repository.PaymentRepository;
import com.medibook.payment.security.AuthenticatedUser;
import com.medibook.payment.service.AppointmentServiceGateway;
import com.medibook.payment.service.AppointmentSummary;
import com.medibook.payment.service.PaymentService;
import com.medibook.payment.service.ProviderServiceGateway;
import com.medibook.payment.service.ProviderSummary;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final AppointmentServiceGateway appointmentServiceGateway;
    private final ProviderServiceGateway providerServiceGateway;
    private final AppProperties appProperties;
    private final Clock clock;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            AppointmentServiceGateway appointmentServiceGateway,
            ProviderServiceGateway providerServiceGateway,
            AppProperties appProperties,
            Clock clock) {
        this.paymentRepository = paymentRepository;
        this.appointmentServiceGateway = appointmentServiceGateway;
        this.providerServiceGateway = providerServiceGateway;
        this.appProperties = appProperties;
        this.clock = clock;
    }

    @Override
    public PaymentResponse processPayment(AuthenticatedUser authenticatedUser, ProcessPaymentRequest request) {
        if (!isAdmin(authenticatedUser) && !isPatient(authenticatedUser)) {
            throw new AccessDeniedException("Only patients or admins can process payments");
        }

        AppointmentSummary appointment = appointmentServiceGateway.getAppointmentById(request.appointmentId().trim());
        assertPaymentAllowedForAppointment(appointment);
        if (isPatient(authenticatedUser) && !appointment.patientId().equals(authenticatedUser.userId())) {
            throw new AccessDeniedException("You can only pay for your own appointments");
        }

        Payment payment = paymentRepository.findByAppointmentId(appointment.appointmentId()).orElseGet(Payment::new);
        if (payment.getPaymentId() != null && payment.getStatus() == PaymentStatus.PAID) {
            throw new PaymentConflictException("Payment is already completed for this appointment");
        }
        if (payment.getPaymentId() != null && payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new PaymentConflictException("Refunded appointments cannot be paid again");
        }

        Instant now = Instant.now(clock);
        PaymentStatus processedStatus = request.mode() == PaymentMode.CASH ? PaymentStatus.PENDING : PaymentStatus.PAID;

        if (payment.getPaymentId() == null) {
            payment.setPaymentId(UUID.randomUUID().toString());
        }
        payment.setAppointmentId(appointment.appointmentId());
        payment.setPatientId(appointment.patientId());
        payment.setProviderId(appointment.providerId());
        payment.setAmount(normalizeAmount(request.amount()));
        payment.setMode(request.mode());
        payment.setCurrency(resolveCurrency(request.currency()));
        payment.setStatus(processedStatus);
        payment.setTransactionId(generateTransactionId(request.mode()));
        payment.setPaidAt(processedStatus == PaymentStatus.PAID ? now : null);
        payment.setRefundedAt(null);
        payment.setNotes(blankToNull(request.notes()));

        return toResponse(paymentRepository.saveAndFlush(payment));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByAppointment(String appointmentId, AuthenticatedUser authenticatedUser) {
        Payment payment = findPaymentByAppointmentOrThrow(appointmentId);
        assertCanView(payment, authenticatedUser);
        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getMyPayments(AuthenticatedUser authenticatedUser) {
        assertPatient(authenticatedUser);
        return paymentRepository.findByPatientIdOrderByCreatedAtDesc(authenticatedUser.userId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByPatientId(String patientId, AuthenticatedUser authenticatedUser) {
        if (!isAdmin(authenticatedUser) && !(isPatient(authenticatedUser) && authenticatedUser.userId().equals(patientId))) {
            throw new AccessDeniedException("You can only access your own patient payments");
        }
        return paymentRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentHistory(
            PaymentStatus status,
            PaymentMode mode,
            String patientId,
            String providerId,
            LocalDate paidFrom,
            LocalDate paidTo,
            AuthenticatedUser authenticatedUser) {
        if (!isAdmin(authenticatedUser)) {
            throw new AccessDeniedException("Only admins can access full payment history");
        }
        validateDateRange(paidFrom, paidTo);
        return paymentRepository.searchPayments(
                        status,
                        mode,
                        blankToNull(patientId),
                        blankToNull(providerId),
                        null,
                        null)
                .stream()
                .filter(payment -> isWithinDateRange(payment, paidFrom, paidTo))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public PaymentResponse refundPayment(String paymentId, RefundPaymentRequest request, AuthenticatedUser authenticatedUser) {
        if (!isAdmin(authenticatedUser)) {
            throw new AccessDeniedException("Only admins can trigger manual refunds");
        }
        Payment payment = findPaymentOrThrow(paymentId);
        applyRefund(payment, request == null ? null : request.reason());
        return toResponse(paymentRepository.saveAndFlush(payment));
    }

    @Override
    public MessageResponse refundPaymentByAppointmentInternally(String appointmentId, RefundPaymentRequest request) {
        Payment payment = paymentRepository.findByAppointmentId(appointmentId).orElse(null);
        if (payment == null) {
            return new MessageResponse("No payment found for the appointment; refund skipped");
        }
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return new MessageResponse("Payment was already refunded");
        }
        if (payment.getStatus() != PaymentStatus.PAID) {
            return new MessageResponse("Refund skipped because the payment is not in PAID state");
        }
        applyRefund(payment, request == null ? null : request.reason());
        paymentRepository.saveAndFlush(payment);
        return new MessageResponse("Refund processed successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(String paymentId, AuthenticatedUser authenticatedUser) {
        Payment payment = findPaymentOrThrow(paymentId);
        assertCanView(payment, authenticatedUser);
        return new PaymentStatusResponse(
                payment.getPaymentId(),
                payment.getAppointmentId(),
                payment.getStatus(),
                payment.getTransactionId(),
                payment.getPaidAt(),
                payment.getRefundedAt());
    }

    @Override
    public PaymentResponse updatePaymentStatus(
            String paymentId,
            UpdatePaymentStatusRequest request,
            AuthenticatedUser authenticatedUser) {
        if (!isAdmin(authenticatedUser)) {
            throw new AccessDeniedException("Only admins can update payment status");
        }

        Payment payment = findPaymentOrThrow(paymentId);
        PaymentStatus previousStatus = payment.getStatus();
        payment.setStatus(request.status());
        payment.setNotes(mergeNotes(payment.getNotes(), request.notes()));

        if (request.status() == PaymentStatus.PAID) {
            payment.setPaidAt(payment.getPaidAt() == null ? Instant.now(clock) : payment.getPaidAt());
            payment.setRefundedAt(null);
        } else if (request.status() == PaymentStatus.REFUNDED) {
            if (previousStatus != PaymentStatus.PAID && previousStatus != PaymentStatus.REFUNDED) {
                throw new IllegalStateException("Only paid payments can be marked as refunded");
            }
            payment.setRefundedAt(payment.getRefundedAt() == null ? Instant.now(clock) : payment.getRefundedAt());
        } else {
            payment.setRefundedAt(null);
            if (request.status() == PaymentStatus.FAILED) {
                payment.setPaidAt(null);
            } else if (request.status() == PaymentStatus.PENDING) {
                payment.setPaidAt(null);
            }
        }

        return toResponse(paymentRepository.saveAndFlush(payment));
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse generateInvoice(String paymentId, AuthenticatedUser authenticatedUser) {
        Payment payment = findPaymentOrThrow(paymentId);
        assertCanView(payment, authenticatedUser);
        if (payment.getStatus() != PaymentStatus.PAID && payment.getStatus() != PaymentStatus.REFUNDED) {
            throw new IllegalStateException("Invoice can only be generated for paid or refunded payments");
        }

        AppointmentSummary appointment = appointmentServiceGateway.getAppointmentById(payment.getAppointmentId());
        if (appointment.status() != AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("Invoice is available only after the appointment is completed");
        }

        return new InvoiceResponse(
                buildInvoiceNumber(payment),
                payment.getPaymentId(),
                payment.getAppointmentId(),
                payment.getPatientId(),
                payment.getProviderId(),
                appointment.serviceType(),
                appointment.appointmentDate(),
                appointment.startTime(),
                appointment.endTime(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMode(),
                payment.getStatus(),
                payment.getTransactionId(),
                payment.getPaidAt(),
                Instant.now(clock),
                payment.getNotes());
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformRevenueSummaryResponse getPlatformRevenue(
            LocalDate paidFrom,
            LocalDate paidTo,
            AuthenticatedUser authenticatedUser) {
        if (!isAdmin(authenticatedUser)) {
            throw new AccessDeniedException("Only admins can access platform revenue");
        }
        validateDateRange(paidFrom, paidTo);
        List<Payment> payments = paymentRepository.searchPayments(null, null, null, null, null, null);
        return buildPlatformRevenueSummary(payments, paidFrom, paidTo);
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueSummaryResponse getMyProviderRevenue(
            LocalDate paidFrom,
            LocalDate paidTo,
            AuthenticatedUser authenticatedUser) {
        validateDateRange(paidFrom, paidTo);
        return buildRevenueSummary(resolveCurrentProviderId(authenticatedUser), paidFrom, paidTo);
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueSummaryResponse getProviderRevenue(
            String providerId,
            LocalDate paidFrom,
            LocalDate paidTo,
            AuthenticatedUser authenticatedUser) {
        validateDateRange(paidFrom, paidTo);
        if (!isAdmin(authenticatedUser)) {
            String currentProviderId = resolveCurrentProviderId(authenticatedUser);
            if (!currentProviderId.equals(providerId)) {
                throw new AccessDeniedException("You can only access your own provider revenue");
            }
        }
        return buildRevenueSummary(providerId, paidFrom, paidTo);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByAppointmentInternally(String appointmentId) {
        return toResponse(findPaymentByAppointmentOrThrow(appointmentId));
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueSummaryResponse getProviderRevenueInternally(String providerId, LocalDate paidFrom, LocalDate paidTo) {
        validateDateRange(paidFrom, paidTo);
        return buildRevenueSummary(providerId, paidFrom, paidTo);
    }

    private Payment findPaymentOrThrow(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
    }

    private Payment findPaymentByAppointmentOrThrow(String appointmentId) {
        return paymentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
    }

    private void applyRefund(Payment payment, String reason) {
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new IllegalStateException("Payment is already refunded");
        }
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("Only paid payments can be refunded");
        }
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(Instant.now(clock));
        payment.setNotes(mergeNotes(payment.getNotes(), reason));
    }

    private void assertPaymentAllowedForAppointment(AppointmentSummary appointment) {
        if (appointment.status() == AppointmentStatus.CANCELLED || appointment.status() == AppointmentStatus.NO_SHOW) {
            throw new IllegalStateException("Payments are not allowed for cancelled or no-show appointments");
        }
    }

    private void assertCanView(Payment payment, AuthenticatedUser authenticatedUser) {
        if (isAdmin(authenticatedUser)) {
            return;
        }
        if (isPatient(authenticatedUser) && payment.getPatientId().equals(authenticatedUser.userId())) {
            return;
        }
        if (isProvider(authenticatedUser) && payment.getProviderId().equals(resolveCurrentProviderId(authenticatedUser))) {
            return;
        }
        throw new AccessDeniedException("You are not allowed to access this payment");
    }

    private String resolveCurrentProviderId(AuthenticatedUser authenticatedUser) {
        if (!isProvider(authenticatedUser)) {
            throw new AccessDeniedException("Only providers can access this resource");
        }
        ProviderSummary provider = providerServiceGateway.getProviderByUserId(authenticatedUser.userId());
        return provider.providerId();
    }

    private RevenueSummaryResponse buildRevenueSummary(String providerId, LocalDate paidFrom, LocalDate paidTo) {
        List<Payment> filteredPayments = paymentRepository.findByProviderIdOrderByCreatedAtDesc(providerId)
                .stream()
                .filter(payment -> isWithinDateRange(payment, paidFrom, paidTo))
                .toList();

        BigDecimal totalRevenue = sumAmounts(filteredPayments, PaymentStatus.PAID);
        BigDecimal pendingAmount = sumAmounts(filteredPayments, PaymentStatus.PENDING);
        BigDecimal refundedAmount = sumAmounts(filteredPayments, PaymentStatus.REFUNDED);
        long paidCount = countByStatus(filteredPayments, PaymentStatus.PAID);
        long pendingCount = countByStatus(filteredPayments, PaymentStatus.PENDING);
        List<MonthlyRevenueItemResponse> monthlyBreakdown = buildMonthlyBreakdown(filteredPayments);

        return new RevenueSummaryResponse(
                providerId,
                totalRevenue,
                pendingAmount,
                refundedAmount,
                paidCount,
                pendingCount,
                paidFrom,
                paidTo,
                monthlyBreakdown);
    }

    private PlatformRevenueSummaryResponse buildPlatformRevenueSummary(
            List<Payment> payments,
            LocalDate paidFrom,
            LocalDate paidTo) {
        List<Payment> filteredPayments = payments.stream()
                .filter(payment -> isWithinDateRange(payment, paidFrom, paidTo))
                .toList();

        return new PlatformRevenueSummaryResponse(
                sumAmounts(filteredPayments, PaymentStatus.PAID),
                sumAmounts(filteredPayments, PaymentStatus.PENDING),
                sumAmounts(filteredPayments, PaymentStatus.REFUNDED),
                countByStatus(filteredPayments, PaymentStatus.PAID),
                countByStatus(filteredPayments, PaymentStatus.PENDING),
                paidFrom,
                paidTo,
                buildMonthlyBreakdown(filteredPayments));
    }

    private List<MonthlyRevenueItemResponse> buildMonthlyBreakdown(List<Payment> payments) {
        Map<YearMonth, List<Payment>> grouped = payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PAID)
                .filter(payment -> resolveEffectiveTimestamp(payment) != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        payment -> YearMonth.from(LocalDateTime.ofInstant(resolveEffectiveTimestamp(payment), ZoneOffset.UTC))));

        return grouped.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .map(entry -> new MonthlyRevenueItemResponse(
                        entry.getKey().toString(),
                        entry.getValue().stream()
                                .map(Payment::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP),
                        entry.getValue().size()))
                .toList();
    }

    private BigDecimal sumAmounts(List<Payment> payments, PaymentStatus status) {
        return normalizeAmount(payments.stream()
                .filter(payment -> payment.getStatus() == status)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private long countByStatus(List<Payment> payments, PaymentStatus status) {
        return payments.stream()
                .filter(payment -> payment.getStatus() == status)
                .count();
    }

    private boolean isWithinDateRange(Payment payment, LocalDate paidFrom, LocalDate paidTo) {
        if (paidFrom == null && paidTo == null) {
            return true;
        }
        Instant effectiveTimestamp = resolveEffectiveTimestamp(payment);
        if (effectiveTimestamp == null) {
            return false;
        }
        LocalDate effectiveDate = LocalDateTime.ofInstant(effectiveTimestamp, ZoneOffset.UTC).toLocalDate();
        if (paidFrom != null && effectiveDate.isBefore(paidFrom)) {
            return false;
        }
        if (paidTo != null && effectiveDate.isAfter(paidTo)) {
            return false;
        }
        return true;
    }

    private Instant resolveEffectiveTimestamp(Payment payment) {
        return switch (payment.getStatus()) {
            case PAID -> payment.getPaidAt() != null ? payment.getPaidAt() : payment.getCreatedAt();
            case REFUNDED -> payment.getRefundedAt() != null ? payment.getRefundedAt() : payment.getUpdatedAt();
            case PENDING, FAILED -> payment.getCreatedAt();
        };
    }

    private MonthlyRevenueItemResponse toMonthlyRevenue(MonthlyRevenueView monthlyRevenueView) {
        return new MonthlyRevenueItemResponse(
                monthlyRevenueView.getRevenueMonth(),
                normalizeAmount(monthlyRevenueView.getTotalRevenue()),
                monthlyRevenueView.getPaidTransactionCount());
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getAppointmentId(),
                payment.getPatientId(),
                payment.getProviderId(),
                normalizeAmount(payment.getAmount()),
                payment.getStatus(),
                payment.getMode(),
                payment.getTransactionId(),
                payment.getCurrency(),
                payment.getPaidAt(),
                payment.getRefundedAt(),
                payment.getNotes(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }

    private String buildInvoiceNumber(Payment payment) {
        return "INV-" + payment.getPaymentId().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String generateTransactionId(PaymentMode mode) {
        return "TXN-" + mode.name() + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveCurrency(String currency) {
        String fallback = appProperties.getPayment().getDefaultCurrency();
        return StringUtils.hasText(currency) ? currency.trim().toUpperCase(Locale.ROOT) : fallback;
    }

    private boolean isAdmin(AuthenticatedUser authenticatedUser) {
        return authenticatedUser != null && authenticatedUser.role() == Role.ADMIN;
    }

    private boolean isPatient(AuthenticatedUser authenticatedUser) {
        return authenticatedUser != null && authenticatedUser.role() == Role.PATIENT;
    }

    private boolean isProvider(AuthenticatedUser authenticatedUser) {
        return authenticatedUser != null && authenticatedUser.role() == Role.PROVIDER;
    }

    private void assertPatient(AuthenticatedUser authenticatedUser) {
        if (!isPatient(authenticatedUser)) {
            throw new AccessDeniedException("Only patients can access this resource");
        }
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String mergeNotes(String existing, String additional) {
        String normalizedAdditional = blankToNull(additional);
        if (normalizedAdditional == null) {
            return existing;
        }
        String normalizedExisting = blankToNull(existing);
        if (normalizedExisting == null) {
            return normalizedAdditional;
        }
        return normalizedExisting + " | " + normalizedAdditional;
    }

    private void validateDateRange(LocalDate paidFrom, LocalDate paidTo) {
        if (paidFrom != null && paidTo != null && paidFrom.isAfter(paidTo)) {
            throw new IllegalArgumentException("paidFrom must be on or before paidTo");
        }
    }

    private Instant toStartInstant(LocalDate date) {
        return date == null ? null : date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private Instant toEndInstant(LocalDate date) {
        return date == null ? null : date.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
    }
}
