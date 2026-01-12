package com.htai.exe201phapluatso.auth.service;

import com.htai.exe201phapluatso.auth.dto.RateLimitInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory rate limiting service using sliding window algorithm.
 * Configurable limits per endpoint pattern.
 */
@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    // Rate limit buckets: key = "ip:endpoint", value = bucket
    private final Map<String, RateBucket> buckets = new ConcurrentHashMap<>();

    // Configuration (can be externalized to application.properties)
    @Value("${app.rate-limit.login.limit:5}")
    private int loginLimit;

    @Value("${app.rate-limit.login.window-seconds:60}")
    private int loginWindowSeconds;

    @Value("${app.rate-limit.register.limit:3}")
    private int registerLimit;

    @Value("${app.rate-limit.register.window-seconds:60}")
    private int registerWindowSeconds;

    @Value("${app.rate-limit.password-reset.limit:3}")
    private int passwordResetLimit;

    @Value("${app.rate-limit.password-reset.window-seconds:60}")
    private int passwordResetWindowSeconds;

    @Value("${app.rate-limit.default.limit:100}")
    private int defaultLimit;

    @Value("${app.rate-limit.default.window-seconds:60}")
    private int defaultWindowSeconds;

    /**
     * Check if request is allowed and consume one token if allowed.
     */
    public boolean isAllowed(String ipAddress, String endpoint) {
        String key = buildKey(ipAddress, endpoint);
        RateLimitConfig config = getConfigForEndpoint(endpoint);
        
        RateBucket bucket = buckets.computeIfAbsent(key, 
            k -> new RateBucket(config.limit(), config.windowSeconds()));
        
        return bucket.tryConsume();
    }

    /**
     * Get rate limit info for an IP/endpoint combination.
     */
    public RateLimitInfo getRateLimitInfo(String ipAddress, String endpoint) {
        String key = buildKey(ipAddress, endpoint);
        RateLimitConfig config = getConfigForEndpoint(endpoint);
        
        RateBucket bucket = buckets.get(key);
        if (bucket == null) {
            return new RateLimitInfo(config.limit(), config.limit(), 
                System.currentTimeMillis() + config.windowSeconds() * 1000L, 0);
        }
        
        return bucket.getInfo(config.limit());
    }

    /**
     * Check if endpoint should be rate limited.
     */
    public boolean isRateLimitedEndpoint(String endpoint) {
        return endpoint.contains("/api/auth/login") ||
               endpoint.contains("/api/auth/register") ||
               endpoint.contains("/api/auth/password-reset");
    }

    private String buildKey(String ipAddress, String endpoint) {
        // Normalize endpoint to pattern
        String pattern = normalizeEndpoint(endpoint);
        return ipAddress + ":" + pattern;
    }

    private String normalizeEndpoint(String endpoint) {
        if (endpoint.contains("/api/auth/login")) return "/api/auth/login";
        if (endpoint.contains("/api/auth/register")) return "/api/auth/register";
        if (endpoint.contains("/api/auth/password-reset")) return "/api/auth/password-reset";
        return endpoint;
    }

    private RateLimitConfig getConfigForEndpoint(String endpoint) {
        if (endpoint.contains("/api/auth/login")) {
            return new RateLimitConfig(loginLimit, loginWindowSeconds);
        }
        if (endpoint.contains("/api/auth/register")) {
            return new RateLimitConfig(registerLimit, registerWindowSeconds);
        }
        if (endpoint.contains("/api/auth/password-reset")) {
            return new RateLimitConfig(passwordResetLimit, passwordResetWindowSeconds);
        }
        return new RateLimitConfig(defaultLimit, defaultWindowSeconds);
    }

    private record RateLimitConfig(int limit, int windowSeconds) {}

    /**
     * Simple sliding window rate limit bucket.
     */
    private static class RateBucket {
        private final int limit;
        private final long windowMillis;
        private final AtomicInteger count = new AtomicInteger(0);
        private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());

        RateBucket(int limit, int windowSeconds) {
            this.limit = limit;
            this.windowMillis = windowSeconds * 1000L;
        }

        synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            long start = windowStart.get();
            
            // Reset window if expired
            if (now - start >= windowMillis) {
                windowStart.set(now);
                count.set(1);
                return true;
            }
            
            // Check if under limit
            if (count.get() < limit) {
                count.incrementAndGet();
                return true;
            }
            
            return false;
        }

        synchronized RateLimitInfo getInfo(int configLimit) {
            long now = System.currentTimeMillis();
            long start = windowStart.get();
            long elapsed = now - start;
            
            if (elapsed >= windowMillis) {
                // Window expired, would reset on next request
                return new RateLimitInfo(configLimit, configLimit, now + windowMillis, 0);
            }
            
            int remaining = Math.max(0, limit - count.get());
            long resetTimestamp = start + windowMillis;
            long retryAfter = remaining == 0 ? (resetTimestamp - now) / 1000 : 0;
            
            return new RateLimitInfo(remaining, configLimit, resetTimestamp, retryAfter);
        }
    }
}
