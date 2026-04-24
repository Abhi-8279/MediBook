package com.medibook.payment.controller;

import com.medibook.payment.dto.request.RefundPaymentRequest;
import com.medibook.payment.dto.response.MessageResponse;
import com.medibook.payment.dto.response.PaymentResponse;
import com.medibook.payment.dto.response.RevenueSummaryResponse;
import com.medibook.payment.service.PaymentService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/internal")
public class InternalPaymentController {

    private final PaymentService paymentService;

    public InternalPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/appointments/{appointmentId}/refund")
    public ResponseEntity<MessageResponse> refundAppointmentPayment(
            @PathVariable String appointmentId,
            @RequestBody(required = false) RefundPaymentRequest request) {
        RefundPaymentRequest effectiveRequest = request == null ? new RefundPaymentRequest(null) : request;
        return ResponseEntity.ok(paymentService.refundPaymentByAppointmentInternally(appointmentId, effectiveRequest));
    }

    @GetMapping("/appointments/{appointmentId}")
    public ResponseEntity<PaymentResponse> getPaymentByAppointment(@PathVariable String appointmentId) {
        return ResponseEntity.ok(paymentService.getPaymentByAppointmentInternally(appointmentId));
    }

    @GetMapping("/providers/{providerId}/revenue")
    public ResponseEntity<RevenueSummaryResponse> getProviderRevenue(
            @PathVariable String providerId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate paidFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate paidTo) {
        return ResponseEntity.ok(paymentService.getProviderRevenueInternally(providerId, paidFrom, paidTo));
    }
}
