package com.medibook.payment.service;

public record RazorpayOrder(
        String id,
        long amount,
        String currency,
        String status,
        String receipt) {
}
