package com.htai.exe201phapluatso.payment.dto;

public record CreatePaymentResponse(
    String paymentUrl,
    String txnRef
) {
}
