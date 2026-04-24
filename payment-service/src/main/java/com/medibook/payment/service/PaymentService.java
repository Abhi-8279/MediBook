package com.medibook.payment.service;

import com.medibook.payment.dto.request.ProcessPaymentRequest;
import com.medibook.payment.dto.request.RefundPaymentRequest;
import com.medibook.payment.dto.request.UpdatePaymentStatusRequest;
import com.medibook.payment.dto.response.InvoiceResponse;
import com.medibook.payment.dto.response.MessageResponse;
import com.medibook.payment.dto.response.PaymentResponse;
import com.medibook.payment.dto.response.PaymentStatusResponse;
import com.medibook.payment.dto.response.PlatformRevenueSummaryResponse;
import com.medibook.payment.dto.response.RevenueSummaryResponse;
import com.medibook.payment.enums.PaymentMode;
import com.medibook.payment.enums.PaymentStatus;
import com.medibook.payment.security.AuthenticatedUser;
import java.time.LocalDate;
import java.util.List;

public interface PaymentService {

    PaymentResponse processPayment(AuthenticatedUser authenticatedUser, ProcessPaymentRequest request);

    PaymentResponse getPaymentByAppointment(String appointmentId, AuthenticatedUser authenticatedUser);

    List<PaymentResponse> getMyPayments(AuthenticatedUser authenticatedUser);

    List<PaymentResponse> getPaymentsByPatientId(String patientId, AuthenticatedUser authenticatedUser);

    List<PaymentResponse> getPaymentHistory(
            PaymentStatus status,
            PaymentMode mode,
            String patientId,
            String providerId,
            LocalDate paidFrom,
            LocalDate paidTo,
            AuthenticatedUser authenticatedUser);

    PaymentResponse refundPayment(String paymentId, RefundPaymentRequest request, AuthenticatedUser authenticatedUser);

    MessageResponse refundPaymentByAppointmentInternally(String appointmentId, RefundPaymentRequest request);

    PaymentStatusResponse getPaymentStatus(String paymentId, AuthenticatedUser authenticatedUser);

    PaymentResponse updatePaymentStatus(
            String paymentId,
            UpdatePaymentStatusRequest request,
            AuthenticatedUser authenticatedUser);

    InvoiceResponse generateInvoice(String paymentId, AuthenticatedUser authenticatedUser);

    PlatformRevenueSummaryResponse getPlatformRevenue(
            LocalDate paidFrom,
            LocalDate paidTo,
            AuthenticatedUser authenticatedUser);

    RevenueSummaryResponse getMyProviderRevenue(
            LocalDate paidFrom,
            LocalDate paidTo,
            AuthenticatedUser authenticatedUser);

    RevenueSummaryResponse getProviderRevenue(
            String providerId,
            LocalDate paidFrom,
            LocalDate paidTo,
            AuthenticatedUser authenticatedUser);

    PaymentResponse getPaymentByAppointmentInternally(String appointmentId);

    RevenueSummaryResponse getProviderRevenueInternally(String providerId, LocalDate paidFrom, LocalDate paidTo);
}
