package com.htai.exe201phapluatso.common.exception;

/**
 * Exception thrown when rate limit is exceeded for an IP/endpoint.
 */
public class RateLimitExceededException extends RuntimeException {
    
    private final long retryAfterSeconds;
    private final int limit;
    private final int remaining;

    public RateLimitExceededException(long retryAfterSeconds, int limit) {
        super("Quá nhiều yêu cầu. Vui lòng thử lại sau.");
        this.retryAfterSeconds = retryAfterSeconds;
        this.limit = limit;
        this.remaining = 0;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public int getLimit() {
        return limit;
    }

    public int getRemaining() {
        return remaining;
    }
}
