package com.htai.exe201phapluatso.payment.dto;

public record CreatePaymentResponse(
    String paymentUrl,
    String orderCode,
    String qrCode,
    Long amount,
    String planName
) {
    // Constructor for backward compatibility
    public CreatePaymentResponse(String paymentUrl, String orderCode, String qrCode) {
        this(paymentUrl, orderCode, qrCode, null, null);
    }
}
