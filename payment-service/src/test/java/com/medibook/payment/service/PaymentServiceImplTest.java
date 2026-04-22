package com.medibook.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.medibook.payment.config.AppProperties;
import com.medibook.payment.dto.request.ProcessPaymentRequest;
import com.medibook.payment.dto.request.RefundPaymentRequest;
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
import com.medibook.payment.repository.MonthlyRevenueView;
import com.medibook.payment.repository.PaymentRepository;
import com.medibook.payment.security.AuthenticatedUser;
import com.medibook.payment.service.impl.PaymentServiceImpl;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-04-22T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AppointmentServiceGateway appointmentServiceGateway;

    @Mock
    private ProviderServiceGateway providerServiceGateway;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        paymentService = new PaymentServiceImpl(
                paymentRepository,
                appointmentServiceGateway,
                providerServiceGateway,
                appProperties,
                FIXED_CLOCK);
    }

    @Test
    void shouldProcessOnlinePaymentForPatientAppointment() {
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
                        PaymentMode.UPI,
                        "INR",
                        "Consultation fee"));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).saveAndFlush(captor.capture());
        Payment saved = captor.getValue();

        assertThat(saved.getAppointmentId()).isEqualTo("appointment-1");
        assertThat(saved.getPatientId()).isEqualTo("patient-1");
        assertThat(saved.getProviderId()).isEqualTo("provider-1");
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(saved.getPaidAt()).isEqualTo(Instant.parse("2026-04-22T10:00:00Z"));
        assertThat(saved.getTransactionId()).startsWith("TXN-UPI-");
        assertThat(response.status()).isEqualTo(PaymentStatus.PAID);
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
        payment.setTransactionId("TXN-CARD-1234567890AB");
        payment.setCurrency("INR");
        payment.setPaidAt(Instant.parse("2026-04-22T09:00:00Z"));
        payment.setNotes("Card payment");
        payment.setCreatedAt(Instant.parse("2026-04-22T08:00:00Z"));
        payment.setUpdatedAt(Instant.parse("2026-04-22T09:00:00Z"));
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
}
