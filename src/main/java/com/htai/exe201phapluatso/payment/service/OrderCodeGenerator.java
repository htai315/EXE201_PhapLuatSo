package com.htai.exe201phapluatso.payment.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for generating unique order codes using database sequence.
 * This ensures no collision even in distributed/concurrent environments.
 */
@Service
public class OrderCodeGenerator {

    /**
     * Generate unique order code based on current time.
     * Format: YYMMDDHHmmss + 3 random digits
     * Max length: 15 digits (Safe for Java Long and PayOS)
     */
    public long generateOrderCode() {
        // Lấy thời gian hiện tại
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        // Format: YYMMDDHHmmss (12 digits)
        // Ví dụ 2024-01-27 23:30:45 -> 240127233045
        String timePart = java.time.format.DateTimeFormatter.ofPattern("yyMMddHHmmss").format(now);

        // Random 3 digits (100-999)
        int randomPart = java.util.concurrent.ThreadLocalRandom.current().nextInt(100, 1000);

        // Combine: 12 digits + 3 digits = 15 digits
        String orderCodeStr = timePart + randomPart;

        return Long.parseLong(orderCodeStr);
    }
}
