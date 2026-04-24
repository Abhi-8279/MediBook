package com.medibook.payment.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RevenueSummaryResponse(
        String providerId,
        BigDecimal totalRevenue,
        BigDecimal pendingAmount,
        BigDecimal refundedAmount,
        long paidTransactionCount,
        long pendingTransactionCount,
        LocalDate dateFrom,
        LocalDate dateTo,
        List<MonthlyRevenueItemResponse> monthlyBreakdown) {
}
