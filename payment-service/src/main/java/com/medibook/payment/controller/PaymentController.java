package com.medibook.payment.controller;

import com.medibook.payment.dto.request.CreateCheckoutOrderRequest;
import com.medibook.payment.dto.request.MarkCheckoutPaymentFailedRequest;
import com.medibook.payment.dto.request.ProcessPaymentRequest;
import com.medibook.payment.dto.request.RefundPaymentRequest;
import com.medibook.payment.dto.request.UpdatePaymentStatusRequest;
import com.medibook.payment.dto.request.VerifyCheckoutPaymentRequest;
import com.medibook.payment.dto.response.CheckoutOrderResponse;
import com.medibook.payment.dto.response.InvoiceResponse;
import com.medibook.payment.dto.response.PaymentResponse;
import com.medibook.payment.dto.response.PaymentStatusResponse;
import com.medibook.payment.dto.response.PlatformRevenueSummaryResponse;
import com.medibook.payment.dto.response.RevenueSummaryResponse;
import com.medibook.payment.enums.PaymentMode;
import com.medibook.payment.enums.PaymentStatus;
import com.medibook.payment.security.AuthenticatedUser;
import com.medibook.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/process")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PATIENT','ADMIN')")
    public ResponseEntity<PaymentResponse> processPayment(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody ProcessPaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.processPayment(authenticatedUser, request));
    }

    @PostMapping("/checkout/order")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PATIENT','ADMIN')")
    public ResponseEntity<CheckoutOrderResponse> createCheckoutOrder(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateCheckoutOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createCheckoutOrder(authenticatedUser, request));
    }

    @PostMapping("/checkout/confirm")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PATIENT','ADMIN')")
    public ResponseEntity<PaymentResponse> verifyCheckoutPayment(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody VerifyCheckoutPaymentRequest request) {
        return ResponseEntity.ok(paymentService.verifyCheckoutPayment(authenticatedUser, request));
    }

    @PostMapping("/checkout/failure")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PATIENT','ADMIN')")
    public ResponseEntity<PaymentResponse> markCheckoutFailure(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody MarkCheckoutPaymentFailedRequest request) {
        return ResponseEntity.ok(paymentService.markCheckoutPaymentFailed(authenticatedUser, request));
    }

    @GetMapping("/appointments/{appointmentId}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PaymentResponse> getPaymentByAppointment(
            @PathVariable String appointmentId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(paymentService.getPaymentByAppointment(appointmentId, authenticatedUser));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(paymentService.getMyPayments(authenticatedUser));
    }

    @GetMapping("/patients/{patientId}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PATIENT','ADMIN')")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByPatientId(
            @PathVariable String patientId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(paymentService.getPaymentsByPatientId(patientId, authenticatedUser));
    }

    @GetMapping("/history")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponse>> getPaymentHistory(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) PaymentMode mode,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String providerId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate paidFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate paidTo,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(paymentService.getPaymentHistory(
                status,
                mode,
                patientId,
                providerId,
                paidFrom,
                paidTo,
                authenticatedUser));
    }

    @PostMapping("/{paymentId}/refund")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable String paymentId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestBody(required = false) RefundPaymentRequest request) {
        RefundPaymentRequest effectiveRequest = request == null ? new RefundPaymentRequest(null) : request;
        return ResponseEntity.ok(paymentService.refundPayment(paymentId, effectiveRequest, authenticatedUser));
    }

    @GetMapping("/{paymentId}/status")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(
            @PathVariable String paymentId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(paymentId, authenticatedUser));
    }

    @PutMapping("/{paymentId}/status")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> updatePaymentStatus(
            @PathVariable String paymentId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdatePaymentStatusRequest request) {
        return ResponseEntity.ok(paymentService.updatePaymentStatus(paymentId, request, authenticatedUser));
    }

    @GetMapping("/{paymentId}/invoice")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<InvoiceResponse> generateInvoice(
            @PathVariable String paymentId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(paymentService.generateInvoice(paymentId, authenticatedUser));
    }

    @GetMapping("/admin/revenue")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlatformRevenueSummaryResponse> getPlatformRevenue(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate paidFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate paidTo) {
        return ResponseEntity.ok(paymentService.getPlatformRevenue(paidFrom, paidTo, authenticatedUser));
    }

    @GetMapping("/providers/me/revenue")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<RevenueSummaryResponse> getMyProviderRevenue(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate paidFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate paidTo) {
        return ResponseEntity.ok(paymentService.getMyProviderRevenue(paidFrom, paidTo, authenticatedUser));
    }

    @GetMapping("/providers/{providerId}/revenue")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    public ResponseEntity<RevenueSummaryResponse> getProviderRevenue(
            @PathVariable String providerId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate paidFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate paidTo) {
        return ResponseEntity.ok(paymentService.getProviderRevenue(providerId, paidFrom, paidTo, authenticatedUser));
    }
}
