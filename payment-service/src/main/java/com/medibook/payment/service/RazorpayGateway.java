package com.medibook.payment.service;

import java.math.BigDecimal;
import java.util.Map;

public interface RazorpayGateway {

    RazorpayOrder createOrder(String receipt, BigDecimal amount, String currency, Map<String, String> notes);

    String getKeyId();
}
