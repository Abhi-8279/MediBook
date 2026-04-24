package com.medibook.payment.dto.response;

import java.math.BigDecimal;

public record MonthlyRevenueItemResponse(
        String revenueMonth,
        BigDecimal totalRevenue,
        long paidTransactionCount) {
}
