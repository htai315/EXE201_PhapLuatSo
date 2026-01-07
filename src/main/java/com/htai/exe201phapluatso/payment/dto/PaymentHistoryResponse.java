package com.htai.exe201phapluatso.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentHistoryResponse(
        Long id,
        String planCode,
        String planName,
        BigDecimal amount,
        String status,
        String paymentMethod,
        String orderCode,
        String transactionId,
        String vnpTxnRef,
        String vnpTransactionNo,
        String vnpBankCode,
        String vnpCardType,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        int chatCredits,
        int quizGenCredits,
        int durationMonths
) {}
