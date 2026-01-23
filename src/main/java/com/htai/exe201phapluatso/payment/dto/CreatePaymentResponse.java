package com.htai.exe201phapluatso.payment.dto;

public record CreatePaymentResponse(
    String paymentUrl,
    String orderCode,
    String qrCode,
    Long amount,
    String planName,
    String statusToken
) {
    // Constructor for backward compatibility (without token)
    public CreatePaymentResponse(String paymentUrl, String orderCode, String qrCode) {
        this(paymentUrl, orderCode, qrCode, null, null, null);
    }

    // Constructor for backward compatibility (without token but with amount/planName)
    public CreatePaymentResponse(String paymentUrl, String orderCode, String qrCode, Long amount, String planName) {
        this(paymentUrl, orderCode, qrCode, amount, planName, null);
    }
}
