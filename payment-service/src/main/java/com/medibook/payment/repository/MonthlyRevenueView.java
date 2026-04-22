package com.medibook.payment.repository;

import java.math.BigDecimal;

public interface MonthlyRevenueView {

    String getRevenueMonth();

    BigDecimal getTotalRevenue();

    long getPaidTransactionCount();
}
