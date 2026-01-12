package com.htai.exe201phapluatso.auth.service;

import com.htai.exe201phapluatso.auth.entity.SecurityAuditLog;
import com.htai.exe201phapluatso.auth.repo.SecurityAuditLogRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service for logging security-related events.
 * All logging methods are async to avoid blocking the main request thread.
 */
@Service
public class SecurityAuditService {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditService.class);

    private final SecurityAuditLogRepo auditLogRepo;

    public SecurityAuditService(SecurityAuditLogRepo auditLogRepo) {
        this.auditLogRepo = auditLogRepo;
    }

    /**
     * Log a login attempt (success or failure)
     */
    @Async
    public void logLoginAttempt(String email, String ipAddress, String userAgent, boolean success) {
        try {
            SecurityAuditLog auditLog = SecurityAuditLog.builder()
                    .eventType(success ? SecurityAuditLog.EVENT_LOGIN_SUCCESS : SecurityAuditLog.EVENT_LOGIN_FAILED)
                    .email(email)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .endpoint("/api/auth/login")
                    .details(success ? "Đăng nhập thành công" : "Đăng nhập thất bại - thông tin không hợp lệ")
                    .build();

            auditLogRepo.save(auditLog);
            log.debug("Logged {} for email: {} from IP: {}", auditLog.getEventType(), email, ipAddress);
        } catch (Exception e) {
            log.error("Failed to log login attempt for {}: {}", email, e.getMessage());
        }
    }

    /**
     * Log a login attempt with user ID (for successful logins)
     */
    @Async
    public void logLoginAttempt(Long userId, String email, String ipAddress, String userAgent, boolean success) {
        try {
            SecurityAuditLog auditLog = SecurityAuditLog.builder()
                    .eventType(success ? SecurityAuditLog.EVENT_LOGIN_SUCCESS : SecurityAuditLog.EVENT_LOGIN_FAILED)
                    .userId(userId)
                    .email(email)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .endpoint("/api/auth/login")
                    .details(success ? "Đăng nhập thành công" : "Đăng nhập thất bại - thông tin không hợp lệ")
                    .build();

            auditLogRepo.save(auditLog);
            log.debug("Logged {} for user: {} from IP: {}", auditLog.getEventType(), userId, ipAddress);
        } catch (Exception e) {
            log.error("Failed to log login attempt for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Log account lockout event
     */
    @Async
    public void logAccountLocked(Long userId, String email, String ipAddress, String reason, int durationMinutes) {
        try {
            SecurityAuditLog auditLog = SecurityAuditLog.builder()
                    .eventType(SecurityAuditLog.EVENT_ACCOUNT_LOCKED)
                    .userId(userId)
                    .email(email)
                    .ipAddress(ipAddress)
                    .details(String.format("Tài khoản bị khóa %d phút. Lý do: %s", durationMinutes, reason))
                    .build();

            auditLogRepo.save(auditLog);
            log.warn("Account locked for user: {} ({}). Reason: {}", userId, email, reason);
        } catch (Exception e) {
            log.error("Failed to log account lockout for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Log token rotation event
     */
    @Async
    public void logTokenRotation(Long userId, String ipAddress) {
        try {
            SecurityAuditLog auditLog = SecurityAuditLog.builder()
                    .eventType(SecurityAuditLog.EVENT_TOKEN_ROTATION)
                    .userId(userId)
                    .ipAddress(ipAddress)
                    .endpoint("/api/auth/refresh")
                    .details("Refresh token đã được xoay vòng thành công")
                    .build();

            auditLogRepo.save(auditLog);
            log.debug("Logged token rotation for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to log token rotation for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Log token reuse detection (security breach)
     */
    @Async
    public void logTokenReuse(Long userId, String ipAddress, String tokenId) {
        try {
            SecurityAuditLog auditLog = SecurityAuditLog.builder()
                    .eventType(SecurityAuditLog.EVENT_TOKEN_REUSE)
                    .userId(userId)
                    .ipAddress(ipAddress)
                    .endpoint("/api/auth/refresh")
                    .details(String.format("Phát hiện sử dụng lại token! Token ID: %s. Tất cả token đã bị thu hồi.", tokenId))
                    .build();

            auditLogRepo.save(auditLog);
            log.warn("SECURITY: Token reuse detected for user: {}. All tokens revoked.", userId);
        } catch (Exception e) {
            log.error("Failed to log token reuse for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Log rate limit exceeded event
     */
    @Async
    public void logRateLimitExceeded(String ipAddress, String endpoint) {
        try {
            SecurityAuditLog auditLog = SecurityAuditLog.builder()
                    .eventType(SecurityAuditLog.EVENT_RATE_LIMIT_EXCEEDED)
                    .ipAddress(ipAddress)
                    .endpoint(endpoint)
                    .details("Vượt quá giới hạn yêu cầu")
                    .build();

            auditLogRepo.save(auditLog);
            log.warn("Rate limit exceeded for IP: {} on endpoint: {}", ipAddress, endpoint);
        } catch (Exception e) {
            log.error("Failed to log rate limit exceeded for IP {}: {}", ipAddress, e.getMessage());
        }
    }

    /**
     * Log password change event
     */
    @Async
    public void logPasswordChanged(Long userId, String email, String ipAddress) {
        try {
            SecurityAuditLog auditLog = SecurityAuditLog.builder()
                    .eventType(SecurityAuditLog.EVENT_PASSWORD_CHANGED)
                    .userId(userId)
                    .email(email)
                    .ipAddress(ipAddress)
                    .details("Đổi mật khẩu thành công")
                    .build();

            auditLogRepo.save(auditLog);
            log.info("Password changed for user: {} ({})", userId, email);
        } catch (Exception e) {
            log.error("Failed to log password change for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Log password reset request
     */
    @Async
    public void logPasswordReset(String email, String ipAddress, boolean success) {
        try {
            SecurityAuditLog auditLog = SecurityAuditLog.builder()
                    .eventType(SecurityAuditLog.EVENT_PASSWORD_RESET)
                    .email(email)
                    .ipAddress(ipAddress)
                    .endpoint("/api/auth/password-reset")
                    .details(success ? "Đặt lại mật khẩu thành công" : "Yêu cầu đặt lại mật khẩu")
                    .build();

            auditLogRepo.save(auditLog);
            log.debug("Password reset {} for email: {}", success ? "completed" : "requested", email);
        } catch (Exception e) {
            log.error("Failed to log password reset for {}: {}", email, e.getMessage());
        }
    }
}
