package com.medibook.payment.service.impl;

import com.medibook.payment.config.AppProperties;
import com.medibook.payment.dto.request.CreateCheckoutOrderRequest;
import com.medibook.payment.dto.request.MarkCheckoutPaymentFailedRequest;
import com.medibook.payment.dto.request.ProcessPaymentRequest;
import com.medibook.payment.dto.request.RefundPaymentRequest;
import com.medibook.payment.dto.request.UpdatePaymentStatusRequest;
import com.medibook.payment.dto.request.VerifyCheckoutPaymentRequest;
import com.medibook.payment.dto.response.CheckoutOrderResponse;
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
import com.medibook.payment.messaging.NotificationEventPublisher;
import com.medibook.payment.repository.PaymentRepository;
import com.medibook.payment.security.AuthenticatedUser;
import com.medibook.payment.service.AppointmentServiceGateway;
import com.medibook.payment.service.AppointmentSummary;
import com.medibook.payment.service.PaymentService;
import com.medibook.payment.service.ProviderServiceGateway;
import com.medibook.payment.service.ProviderSummary;
import com.medibook.payment.service.RazorpayGateway;
import com.medibook.payment.service.RazorpayOrder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
    private final RazorpayGateway razorpayGateway;
    private final NotificationEventPublisher notificationEventPublisher;
    private final AppProperties appProperties;
    private final Clock clock;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            AppointmentServiceGateway appointmentServiceGateway,
            ProviderServiceGateway providerServiceGateway,
            RazorpayGateway razorpayGateway,
            NotificationEventPublisher notificationEventPublisher,
            AppProperties appProperties,
            Clock clock) {
        this.paymentRepository = paymentRepository;
        this.appointmentServiceGateway = appointmentServiceGateway;
        this.providerServiceGateway = providerServiceGateway;
        this.razorpayGateway = razorpayGateway;
        this.notificationEventPublisher = notificationEventPublisher;
        this.appProperties = appProperties;
        this.clock = clock;
    }

    @Override
    public PaymentResponse processPayment(AuthenticatedUser authenticatedUser, ProcessPaymentRequest request) {
        if (!isAdmin(authenticatedUser) && !isPatient(authenticatedUser)) {
            throw new AccessDeniedException("Only patients or admins can process payments");
        }
        if (request.mode() != PaymentMode.CASH) {
            throw new IllegalStateException("Online payments must be completed through Razorpay checkout");
        }

        AppointmentSummary appointment = loadPayableAppointment(request.appointmentId(), authenticatedUser);
        Payment payment = paymentRepository.findByAppointmentId(appointment.appointmentId()).orElseGet(Payment::new);
        assertPaymentCanBeUpdated(payment);

        if (payment.getPaymentId() == null) {
            payment.setPaymentId(UUID.randomUUID().toString());
        }

        payment.setAppointmentId(appointment.appointmentId());
        payment.setPatientId(appointment.patientId());
        payment.setProviderId(appointment.providerId());
        payment.setAmount(normalizeAmount(request.amount()));
        payment.setMode(PaymentMode.CASH);
        payment.setCurrency(resolveCurrency(request.currency()));
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionId(generateTransactionId(PaymentMode.CASH));
        payment.setGatewayOrderId(null);
        payment.setGatewayPaymentId(null);
        payment.setPaidAt(null);
        payment.setRefundedAt(null);
        payment.setNotes(blankToNull(request.notes()));

        return toResponse(paymentRepository.saveAndFlush(payment));
    }

    @Override
    public CheckoutOrderResponse createCheckoutOrder(
            AuthenticatedUser authenticatedUser,
            CreateCheckoutOrderRequest request) {
        if (!isAdmin(authenticatedUser) && !isPatient(authenticatedUser)) {
            throw new AccessDeniedException("Only patients or admins can create checkout orders");
        }
        assertOnlineMode(request.mode());

        AppointmentSummary appointment = loadPayableAppointment(request.appointmentId(), authenticatedUser);
        Payment payment = paymentRepository.findByAppointmentId(appointment.appointmentId()).orElseGet(Payment::new);
        assertPaymentCanBeUpdated(payment);

        if (payment.getPaymentId() == null) {
            payment.setPaymentId(UUID.randomUUID().toString());
        }

        BigDecimal normalizedAmount = normalizeAmount(request.amount());
        String currency = resolveCurrency(request.currency());
        RazorpayOrder razorpayOrder = razorpayGateway.createOrder(
                buildReceipt(payment.getPaymentId(), appointment.appointmentId()),
                normalizedAmount,
                currency,
                buildRazorpayNotes(appointment, request.mode()));

        payment.setAppointmentId(appointment.appointmentId());
        payment.setPatientId(appointment.patientId());
        payment.setProviderId(appointment.providerId());
        payment.setAmount(normalizedAmount);
        payment.setMode(request.mode());
        payment.setCurrency(currency);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionId(generateTransactionId(request.mode()));
        payment.setGatewayOrderId(razorpayOrder.id());
        payment.setGatewayPaymentId(null);
        payment.setPaidAt(null);
        payment.setRefundedAt(null);
        payment.setNotes(blankToNull(request.notes()));

        Payment saved = paymentRepository.saveAndFlush(payment);
        AppProperties.Razorpay razorpay = appProperties.getPayment().getRazorpay();
        return new CheckoutOrderResponse(
                saved.getPaymentId(),
                saved.getAppointmentId(),
                razorpayOrder.id(),
                razorpayGateway.getKeyId(),
                normalizedAmount,
                razorpayOrder.amount(),
                currency,
                razorpay.getCheckoutName(),
                razorpay.getCheckoutDescription(),
                blankToNull(razorpay.getCheckoutImageUrl()));
    }

    @Override
    public PaymentResponse verifyCheckoutPayment(
            AuthenticatedUser authenticatedUser,
            VerifyCheckoutPaymentRequest request) {
        if (!isAdmin(authenticatedUser) && !isPatient(authenticatedUser)) {
            throw new AccessDeniedException("Only patients or admins can verify checkout payments");
        }

        Payment payment = findPaymentOrThrow(request.paymentId().trim());
        assertCanView(payment, authenticatedUser);
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new PaymentConflictException("Refunded appointments cannot be paid again");
        }
        if (payment.getStatus() == PaymentStatus.PAID) {
            if (Objects.equals(payment.getGatewayPaymentId(), request.razorpayPaymentId().trim())) {
                return toResponse(payment);
            }
            throw new PaymentConflictException("Payment is already completed for this appointment");
        }
        if (!StringUtils.hasText(payment.getGatewayOrderId())) {
            throw new IllegalStateException("No Razorpay checkout order exists for this payment");
        }
        if (!payment.getGatewayOrderId().equals(request.razorpayOrderId().trim())) {
            throw new IllegalStateException("Razorpay order does not match the initialized checkout order");
        }
        if (!verifyRazorpaySignature(payment.getGatewayOrderId(), request.razorpayPaymentId().trim(), request.razorpaySignature().trim())) {
            markPaymentFailed(payment, request.razorpayPaymentId().trim(), "Razorpay signature verification failed");
            paymentRepository.saveAndFlush(payment);
            throw new IllegalStateException("Payment verification failed");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setTransactionId(request.razorpayPaymentId().trim());
        payment.setGatewayPaymentId(request.razorpayPaymentId().trim());
        payment.setPaidAt(Instant.now(clock));
        payment.setRefundedAt(null);

        Payment saved = paymentRepository.saveAndFlush(payment);
        publishProcessedEventQuietly(saved);
        return toResponse(saved);
    }

    @Override
    public PaymentResponse markCheckoutPaymentFailed(
            AuthenticatedUser authenticatedUser,
            MarkCheckoutPaymentFailedRequest request) {
        if (!isAdmin(authenticatedUser) && !isPatient(authenticatedUser)) {
            throw new AccessDeniedException("Only patients or admins can update checkout failures");
        }

        Payment payment = findPaymentOrThrow(request.paymentId().trim());
        assertCanView(payment, authenticatedUser);
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new PaymentConflictException("Completed payments cannot be marked as failed");
        }
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new PaymentConflictException("Refunded payments cannot be marked as failed");
        }
        String gatewayOrderId = blankToNull(request.razorpayOrderId());
        if (gatewayOrderId != null
                && StringUtils.hasText(payment.getGatewayOrderId())
                && !payment.getGatewayOrderId().equals(gatewayOrderId)) {
            throw new IllegalStateException("Razorpay order does not match the initialized checkout order");
        }

        markPaymentFailed(payment, blankToNull(request.razorpayPaymentId()), request.reason());
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
        Payment saved = paymentRepository.saveAndFlush(payment);
        publishRefundedEventQuietly(saved);
        return toResponse(saved);
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
        Payment saved = paymentRepository.saveAndFlush(payment);
        publishRefundedEventQuietly(saved);
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
            if (request.status() == PaymentStatus.FAILED || request.status() == PaymentStatus.PENDING) {
                payment.setPaidAt(null);
            }
        }

        Payment saved = paymentRepository.saveAndFlush(payment);
        if (saved.getStatus() == PaymentStatus.PAID && previousStatus != PaymentStatus.PAID) {
            publishProcessedEventQuietly(saved);
        }
        if (saved.getStatus() == PaymentStatus.REFUNDED && previousStatus != PaymentStatus.REFUNDED) {
            publishRefundedEventQuietly(saved);
        }
        return toResponse(saved);
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

    private AppointmentSummary loadPayableAppointment(String appointmentId, AuthenticatedUser authenticatedUser) {
        AppointmentSummary appointment = appointmentServiceGateway.getAppointmentById(appointmentId.trim());
        assertPaymentAllowedForAppointment(appointment);
        if (isPatient(authenticatedUser) && !appointment.patientId().equals(authenticatedUser.userId())) {
            throw new AccessDeniedException("You can only pay for your own appointments");
        }
        return appointment;
    }

    private Payment findPaymentOrThrow(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
    }

    private Payment findPaymentByAppointmentOrThrow(String appointmentId) {
        return paymentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
    }

    private void assertPaymentCanBeUpdated(Payment payment) {
        if (payment.getPaymentId() != null && payment.getStatus() == PaymentStatus.PAID) {
            throw new PaymentConflictException("Payment is already completed for this appointment");
        }
        if (payment.getPaymentId() != null && payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new PaymentConflictException("Refunded appointments cannot be paid again");
        }
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

    private void assertOnlineMode(PaymentMode mode) {
        if (mode == null || mode == PaymentMode.CASH) {
            throw new IllegalArgumentException("Razorpay checkout supports CARD, UPI, or WALLET payments only");
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

    private String buildReceipt(String paymentId, String appointmentId) {
        String compactPaymentId = paymentId.replace("-", "");
        String compactAppointmentId = appointmentId.replace("-", "");
        return "MB-"
                + compactPaymentId.substring(0, Math.min(compactPaymentId.length(), 20))
                + "-"
                + compactAppointmentId.substring(0, Math.min(compactAppointmentId.length(), 12));
    }

    private Map<String, String> buildRazorpayNotes(AppointmentSummary appointment, PaymentMode mode) {
        Map<String, String> notes = new LinkedHashMap<>();
        notes.put("appointmentId", appointment.appointmentId());
        notes.put("patientId", appointment.patientId());
        notes.put("providerId", appointment.providerId());
        notes.put("mode", mode.name());
        return notes;
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

    private void publishProcessedEventQuietly(Payment payment) {
        try {
            notificationEventPublisher.publishProcessed(payment);
        } catch (RuntimeException ignored) {
            // Payment processing should remain available even if event publishing is unavailable.
        }
    }

    private void publishRefundedEventQuietly(Payment payment) {
        try {
            notificationEventPublisher.publishRefunded(payment);
        } catch (RuntimeException ignored) {
            // Refund processing should remain available even if event publishing is unavailable.
        }
    }

    private void markPaymentFailed(Payment payment, String gatewayPaymentId, String reason) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setGatewayPaymentId(blankToNull(gatewayPaymentId));
        payment.setPaidAt(null);
        payment.setRefundedAt(null);
        payment.setNotes(mergeNotes(payment.getNotes(), StringUtils.hasText(reason) ? reason : "Razorpay checkout failed"));
    }

    private boolean verifyRazorpaySignature(String orderId, String razorpayPaymentId, String signature) {
        String secret = appProperties.getPayment().getRazorpay().getKeySecret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("Razorpay credentials are missing from the payment service configuration");
        }

        String payload = orderId + "|" + razorpayPaymentId;
        byte[] expected = hmacSha256(payload, secret.trim());
        byte[] actual = signature.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    private byte[] hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return toHex(digest).getBytes(StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to verify Razorpay payment signature", exception);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format(Locale.ROOT, "%02x", value));
        }
        return builder.toString();
    }

    private Instant toStartInstant(LocalDate date) {
        return date == null ? null : date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private Instant toEndInstant(LocalDate date) {
        return date == null ? null : date.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
    }
}
