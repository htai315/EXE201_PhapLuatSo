package com.htai.exe201phapluatso.credit.dto;

import java.time.LocalDateTime;

/**
 * DTO for credit balance response
 * 
 * @param chatCredits Remaining chat credits
 * @param quizGenCredits Remaining quiz generation credits
 * @param expiryDate Expiration date (null if permanent)
 * @param isExpired Whether credits have expired
 * @param planName Current plan name (FREE, REGULAR, STUDENT)
 */
public record CreditBalanceResponse(
        int chatCredits,
        int quizGenCredits,
        LocalDateTime expiryDate,
        boolean isExpired,
        String planName
) {
}
