package com.htai.exe201phapluatso.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity for storing security-related events for audit and monitoring.
 * Events include: login attempts, account lockouts, token rotations, rate limit violations.
 */
@Entity
@Table(name = "security_audit_log")
public class SecurityAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "endpoint", length = 255)
    private String endpoint;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public SecurityAuditLog() {}

    public SecurityAuditLog(String eventType) {
        this.eventType = eventType;
        this.createdAt = LocalDateTime.now();
    }

    // Builder pattern for cleaner construction
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final SecurityAuditLog log = new SecurityAuditLog();

        public Builder eventType(String eventType) {
            log.eventType = eventType;
            return this;
        }

        public Builder userId(Long userId) {
            log.userId = userId;
            return this;
        }

        public Builder email(String email) {
            log.email = email;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            log.ipAddress = ipAddress;
            return this;
        }

        public Builder userAgent(String userAgent) {
            // Truncate user agent if too long
            log.userAgent = userAgent != null && userAgent.length() > 500 
                ? userAgent.substring(0, 500) 
                : userAgent;
            return this;
        }

        public Builder endpoint(String endpoint) {
            log.endpoint = endpoint;
            return this;
        }

        public Builder details(String details) {
            log.details = details;
            return this;
        }

        public SecurityAuditLog build() {
            if (log.eventType == null) {
                throw new IllegalStateException("eventType is required");
            }
            log.createdAt = LocalDateTime.now();
            return log;
        }
    }

    // Getters and Setters
    public Long getId() { return id; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    // Event type constants
    public static final String EVENT_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String EVENT_LOGIN_FAILED = "LOGIN_FAILED";
    public static final String EVENT_ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String EVENT_ACCOUNT_UNLOCKED = "ACCOUNT_UNLOCKED";
    public static final String EVENT_TOKEN_ROTATION = "TOKEN_ROTATION";
    public static final String EVENT_TOKEN_REUSE = "TOKEN_REUSE";
    public static final String EVENT_RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    public static final String EVENT_PASSWORD_CHANGED = "PASSWORD_CHANGED";
    public static final String EVENT_PASSWORD_RESET = "PASSWORD_RESET";
}
