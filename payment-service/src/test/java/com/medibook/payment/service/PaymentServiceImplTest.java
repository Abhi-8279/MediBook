package com.medibook.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medibook.payment.config.AppProperties;
import com.medibook.payment.dto.request.CreateCheckoutOrderRequest;
import com.medibook.payment.dto.request.MarkCheckoutPaymentFailedRequest;
import com.medibook.payment.dto.request.ProcessPaymentRequest;
import com.medibook.payment.dto.request.RefundPaymentRequest;
import com.medibook.payment.dto.request.VerifyCheckoutPaymentRequest;
import com.medibook.payment.dto.response.CheckoutOrderResponse;
import com.medibook.payment.dto.response.InvoiceResponse;
import com.medibook.payment.dto.response.MessageResponse;
import com.medibook.payment.dto.response.PaymentResponse;
import com.medibook.payment.dto.response.PlatformRevenueSummaryResponse;
import com.medibook.payment.dto.response.RevenueSummaryResponse;
import com.medibook.payment.entity.Payment;
import com.medibook.payment.enums.AppointmentStatus;
import com.medibook.payment.enums.PaymentMode;
import com.medibook.payment.enums.PaymentStatus;
import com.medibook.payment.enums.Role;
import com.medibook.payment.messaging.NotificationEventPublisher;
import com.medibook.payment.repository.PaymentRepository;
import com.medibook.payment.security.AuthenticatedUser;
import com.medibook.payment.service.impl.PaymentServiceImpl;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-04-22T10:00:00Z"), ZoneOffset.UTC);
    private static final String RAZORPAY_KEY_ID = "rzp_test_123";
    private static final String RAZORPAY_KEY_SECRET = "secret_test_123";

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AppointmentServiceGateway appointmentServiceGateway;

    @Mock
    private ProviderServiceGateway providerServiceGateway;

    @Mock
    private RazorpayGateway razorpayGateway;

    private RecordingNotificationEventPublisher notificationEventPublisher;
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getPayment().getRazorpay().setEnabled(true);
        appProperties.getPayment().getRazorpay().setKeyId(RAZORPAY_KEY_ID);
        appProperties.getPayment().getRazorpay().setKeySecret(RAZORPAY_KEY_SECRET);
        notificationEventPublisher = new RecordingNotificationEventPublisher();
        paymentService = new PaymentServiceImpl(
                paymentRepository,
                appointmentServiceGateway,
                providerServiceGateway,
                razorpayGateway,
                notificationEventPublisher,
                appProperties,
                FIXED_CLOCK);
    }

    @Test
    void shouldRejectDirectOnlinePaymentProcessing() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser("patient-1", "patient@medibook.com", Role.PATIENT);

        assertThatThrownBy(() -> paymentService.processPayment(
                        authenticatedUser,
                        new ProcessPaymentRequest(
                                "appointment-1",
                                new BigDecimal("500.00"),
                                PaymentMode.UPI,
                                "INR",
                                "Consultation fee")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Razorpay checkout");
    }

    @Test
    void shouldCreateCheckoutOrderForPatientAppointment() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser("patient-1", "patient@medibook.com", Role.PATIENT);
        AppointmentSummary appointment = appointmentSummary(AppointmentStatus.SCHEDULED);

        when(appointmentServiceGateway.getAppointmentById("appointment-1")).thenReturn(appointment);
        when(paymentRepository.findByAppointmentId("appointment-1")).thenReturn(Optional.empty());
        when(razorpayGateway.createOrder(any(), any(), any(), any()))
                .thenReturn(new RazorpayOrder("order_test_123", 50000L, "INR", "created", "MB-receipt"));
        when(razorpayGateway.getKeyId()).thenReturn(RAZORPAY_KEY_ID);
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutOrderResponse response = paymentService.createCheckoutOrder(
                authenticatedUser,
                new CreateCheckoutOrderRequest(
                        "appointment-1",
                        new BigDecimal("500.00"),
                        PaymentMode.UPI,
                        "INR",
                        "Consultation fee"));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).saveAndFlush(captor.capture());
        Payment saved = captor.getValue();

        assertThat(saved.getAppointmentId()).isEqualTo("appointment-1");
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getGatewayOrderId()).isEqualTo("order_test_123");
        assertThat(saved.getGatewayPaymentId()).isNull();
        assertThat(saved.getMode()).isEqualTo(PaymentMode.UPI);
        assertThat(response.razorpayOrderId()).isEqualTo("order_test_123");
        assertThat(response.keyId()).isEqualTo(RAZORPAY_KEY_ID);
        assertThat(response.amountInSubunits()).isEqualTo(50000L);
    }

    @Test
    void shouldVerifyCheckoutPaymentAndMarkItPaid() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser("patient-1", "patient@medibook.com", Role.PATIENT);
        Payment payment = pendingOnlinePayment();
        String razorpayPaymentId = "pay_test_123";

        when(paymentRepository.findByPaymentId("payment-online")).thenReturn(Optional.of(payment));
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.verifyCheckoutPayment(
                authenticatedUser,
                new VerifyCheckoutPaymentRequest(
                        "payment-online",
                        "order_test_123",
                        razorpayPaymentId,
                        signature("order_test_123", razorpayPaymentId)));

        assertThat(response.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.transactionId()).isEqualTo(razorpayPaymentId);
        assertThat(response.paidAt()).isEqualTo(Instant.parse("2026-04-22T10:00:00Z"));
        assertThat(notificationEventPublisher.processedPayment).isSameAs(payment);
    }

    @Test
    void shouldMarkCheckoutFailure() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser("patient-1", "patient@medibook.com", Role.PATIENT);
        Payment payment = pendingOnlinePayment();

        when(paymentRepository.findByPaymentId("payment-online")).thenReturn(Optional.of(payment));
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.markCheckoutPaymentFailed(
                authenticatedUser,
                new MarkCheckoutPaymentFailedRequest(
                        "payment-online",
                        "order_test_123",
                        "pay_failed_123",
                        "Bank declined the transaction"));

        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(response.notes()).contains("Bank declined the transaction");
        assertThat(payment.getGatewayPaymentId()).isEqualTo("pay_failed_123");
    }

    @Test
    void shouldCreatePendingCashPayment() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser("patient-1", "patient@medibook.com", Role.PATIENT);
        AppointmentSummary appointment = appointmentSummary(AppointmentStatus.SCHEDULED);

        when(appointmentServiceGateway.getAppointmentById("appointment-1")).thenReturn(appointment);
        when(paymentRepository.findByAppointmentId("appointment-1")).thenReturn(Optional.empty());
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.processPayment(
                authenticatedUser,
                new ProcessPaymentRequest(
                        "appointment-1",
                        new BigDecimal("500.00"),
                        PaymentMode.CASH,
                        null,
                        "Pay at clinic"));

        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.paidAt()).isNull();
        assertThat(response.currency()).isEqualTo("INR");
    }

    @Test
    void shouldRefundPaidPaymentInternally() {
        Payment payment = paidPayment();

        when(paymentRepository.findByAppointmentId("appointment-1")).thenReturn(Optional.of(payment));
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse response = paymentService.refundPaymentByAppointmentInternally(
                "appointment-1",
                new RefundPaymentRequest("Cancelled by patient"));

        assertThat(response.message()).contains("Refund processed");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getRefundedAt()).isEqualTo(Instant.parse("2026-04-22T10:00:00Z"));
        assertThat(payment.getNotes()).contains("Cancelled by patient");
        assertThat(notificationEventPublisher.refundedPayment).isSameAs(payment);
    }

    @Test
    void shouldGenerateInvoiceOnlyForCompletedAppointment() {
        Payment payment = paidPayment();

        when(paymentRepository.findByPaymentId("payment-1")).thenReturn(Optional.of(payment));
        when(appointmentServiceGateway.getAppointmentById("appointment-1"))
                .thenReturn(appointmentSummary(AppointmentStatus.COMPLETED));

        InvoiceResponse response = paymentService.generateInvoice(
                "payment-1",
                new AuthenticatedUser("patient-1", "patient@medibook.com", Role.PATIENT));

        assertThat(response.invoiceNumber()).startsWith("INV-");
        assertThat(response.appointmentId()).isEqualTo("appointment-1");
        assertThat(response.serviceType()).isEqualTo("General Consultation");
        assertThat(response.status()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void shouldRejectInvoiceForIncompleteAppointment() {
        Payment payment = paidPayment();

        when(paymentRepository.findByPaymentId("payment-1")).thenReturn(Optional.of(payment));
        when(appointmentServiceGateway.getAppointmentById("appointment-1"))
                .thenReturn(appointmentSummary(AppointmentStatus.SCHEDULED));

        assertThatThrownBy(() -> paymentService.generateInvoice(
                        "payment-1",
                        new AuthenticatedUser("patient-1", "patient@medibook.com", Role.PATIENT)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("completed");
    }

    @Test
    void shouldSummarizeProviderRevenue() {
        when(providerServiceGateway.getProviderByUserId("provider-user-1"))
                .thenReturn(new ProviderSummary("provider-1", "provider-user-1", true, true));
        Payment paid = paidPayment();
        paid.setCreatedAt(Instant.parse("2026-04-22T09:00:00Z"));
        Payment pending = pendingPayment();
        pending.setCreatedAt(Instant.parse("2026-04-18T09:00:00Z"));
        Payment refunded = refundedPayment();
        refunded.setCreatedAt(Instant.parse("2026-04-10T09:00:00Z"));
        refunded.setRefundedAt(Instant.parse("2026-04-20T09:00:00Z"));

        when(paymentRepository.findByProviderIdOrderByCreatedAtDesc("provider-1"))
                .thenReturn(List.of(paid, pending, refunded));

        RevenueSummaryResponse response = paymentService.getMyProviderRevenue(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                new AuthenticatedUser("provider-user-1", "doctor@medibook.com", Role.PROVIDER));

        assertThat(response.providerId()).isEqualTo("provider-1");
        assertThat(response.totalRevenue()).isEqualTo(new BigDecimal("500.00"));
        assertThat(response.pendingAmount()).isEqualTo(new BigDecimal("200.00"));
        assertThat(response.refundedAmount()).isEqualTo(new BigDecimal("150.00"));
        assertThat(response.monthlyBreakdown()).hasSize(1);
        assertThat(response.monthlyBreakdown().getFirst().revenueMonth()).isEqualTo("2026-04");
    }

    @Test
    void shouldIncludePendingTransactionsInAdminHistoryDateFilter() {
        Payment pending = pendingPayment();
        pending.setCreatedAt(Instant.parse("2026-04-18T09:00:00Z"));
        Payment olderPending = pendingPayment();
        olderPending.setPaymentId("payment-2");
        olderPending.setAppointmentId("appointment-2");
        olderPending.setTransactionId("TXN-CASH-OLDER123456");
        olderPending.setCreatedAt(Instant.parse("2026-03-18T09:00:00Z"));

        when(paymentRepository.searchPayments(null, null, null, null, null, null))
                .thenReturn(List.of(pending, olderPending));

        List<PaymentResponse> response = paymentService.getPaymentHistory(
                null,
                null,
                null,
                null,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                new AuthenticatedUser("admin-1", "admin@medibook.com", Role.ADMIN));

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().paymentId()).isEqualTo("payment-pending");
    }

    @Test
    void shouldProvidePlatformRevenueSummaryForAdmin() {
        Payment paid = paidPayment();
        paid.setCreatedAt(Instant.parse("2026-04-15T09:00:00Z"));
        Payment pending = pendingPayment();
        pending.setCreatedAt(Instant.parse("2026-04-18T09:00:00Z"));
        Payment refunded = refundedPayment();
        refunded.setCreatedAt(Instant.parse("2026-04-10T09:00:00Z"));
        refunded.setRefundedAt(Instant.parse("2026-04-20T09:00:00Z"));

        when(paymentRepository.searchPayments(null, null, null, null, null, null))
                .thenReturn(List.of(paid, pending, refunded));

        PlatformRevenueSummaryResponse response = paymentService.getPlatformRevenue(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                new AuthenticatedUser("admin-1", "admin@medibook.com", Role.ADMIN));

        assertThat(response.totalRevenue()).isEqualTo(new BigDecimal("500.00"));
        assertThat(response.pendingAmount()).isEqualTo(new BigDecimal("200.00"));
        assertThat(response.refundedAmount()).isEqualTo(new BigDecimal("150.00"));
        assertThat(response.paidTransactionCount()).isEqualTo(1);
        assertThat(response.pendingTransactionCount()).isEqualTo(1);
        assertThat(response.monthlyBreakdown()).hasSize(1);
    }

    private AppointmentSummary appointmentSummary(AppointmentStatus status) {
        return new AppointmentSummary(
                "appointment-1",
                "patient-1",
                "provider-1",
                "slot-1",
                "General Consultation",
                LocalDate.of(2026, 4, 25),
                LocalTime.of(11, 0),
                LocalTime.of(11, 30),
                status);
    }

    private Payment paidPayment() {
        Payment payment = new Payment();
        payment.setPaymentId("payment-1");
        payment.setAppointmentId("appointment-1");
        payment.setPatientId("patient-1");
        payment.setProviderId("provider-1");
        payment.setAmount(new BigDecimal("500.00"));
        payment.setStatus(PaymentStatus.PAID);
        payment.setMode(PaymentMode.CARD);
        payment.setTransactionId("pay_success_123");
        payment.setGatewayOrderId("order_success_123");
        payment.setGatewayPaymentId("pay_success_123");
        payment.setCurrency("INR");
        payment.setPaidAt(Instant.parse("2026-04-22T09:00:00Z"));
        payment.setNotes("Card payment");
        payment.setCreatedAt(Instant.parse("2026-04-22T08:00:00Z"));
        payment.setUpdatedAt(Instant.parse("2026-04-22T09:00:00Z"));
        return payment;
    }

    private Payment pendingOnlinePayment() {
        Payment payment = new Payment();
        payment.setPaymentId("payment-online");
        payment.setAppointmentId("appointment-1");
        payment.setPatientId("patient-1");
        payment.setProviderId("provider-1");
        payment.setAmount(new BigDecimal("500.00"));
        payment.setStatus(PaymentStatus.PENDING);
        payment.setMode(PaymentMode.UPI);
        payment.setTransactionId("TXN-UPI-PENDING123");
        payment.setGatewayOrderId("order_test_123");
        payment.setCurrency("INR");
        payment.setNotes("Waiting for checkout");
        payment.setCreatedAt(Instant.parse("2026-04-22T08:00:00Z"));
        payment.setUpdatedAt(Instant.parse("2026-04-22T08:30:00Z"));
        return payment;
    }

    private Payment pendingPayment() {
        Payment payment = new Payment();
        payment.setPaymentId("payment-pending");
        payment.setAppointmentId("appointment-pending");
        payment.setPatientId("patient-1");
        payment.setProviderId("provider-1");
        payment.setAmount(new BigDecimal("200.00"));
        payment.setStatus(PaymentStatus.PENDING);
        payment.setMode(PaymentMode.CASH);
        payment.setTransactionId("TXN-CASH-1234567890AB");
        payment.setCurrency("INR");
        payment.setNotes("Pay at clinic");
        payment.setUpdatedAt(Instant.parse("2026-04-18T09:00:00Z"));
        return payment;
    }

    private Payment refundedPayment() {
        Payment payment = new Payment();
        payment.setPaymentId("payment-refunded");
        payment.setAppointmentId("appointment-refunded");
        payment.setPatientId("patient-1");
        payment.setProviderId("provider-1");
        payment.setAmount(new BigDecimal("150.00"));
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setMode(PaymentMode.WALLET);
        payment.setTransactionId("TXN-WALLET-1234567890");
        payment.setCurrency("INR");
        payment.setPaidAt(Instant.parse("2026-04-15T09:00:00Z"));
        payment.setUpdatedAt(Instant.parse("2026-04-20T09:00:00Z"));
        payment.setNotes("Refunded payment");
        return payment;
    }

    private String signature(String orderId, String paymentId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(RAZORPAY_KEY_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class RecordingNotificationEventPublisher extends NotificationEventPublisher {

        private Payment processedPayment;
        private Payment refundedPayment;

        private RecordingNotificationEventPublisher() {
            super((RabbitTemplate) null, new ObjectMapper());
        }

        @Override
        public void publishProcessed(Payment payment) {
            this.processedPayment = payment;
        }

        @Override
        public void publishRefunded(Payment payment) {
            this.refundedPayment = payment;
        }
    }
}
