package com.htai.exe201phapluatso.auth.dto;

/**
 * Rate limit information for an IP/endpoint combination.
 */
public record RateLimitInfo(
    int remaining,
    int limit,
    long resetTimestamp,
    long retryAfterSeconds
) {
    public boolean isAllowed() {
        return remaining > 0;
    }
}
