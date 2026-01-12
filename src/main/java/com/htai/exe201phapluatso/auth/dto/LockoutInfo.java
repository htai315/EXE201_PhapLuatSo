package com.htai.exe201phapluatso.auth.dto;

import java.time.LocalDateTime;

/**
 * Information about account lockout status.
 */
public record LockoutInfo(
        boolean isLocked,
        int failedAttempts,
        LocalDateTime lockedUntil,
        long remainingSeconds
) {
    /**
     * Get remaining time in human-readable format (Vietnamese).
     */
    public String getRemainingTimeFormatted() {
        if (!isLocked || remainingSeconds <= 0) {
            return null;
        }

        long minutes = remainingSeconds / 60;
        long seconds = remainingSeconds % 60;

        if (minutes > 0) {
            return String.format("%d phút %d giây", minutes, seconds);
        }
        return String.format("%d giây", seconds);
    }
}
