package com.htai.exe201phapluatso.quiz.session;

import com.htai.exe201phapluatso.quiz.dto.ExamSessionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Manager để quản lý exam session storage
 * Tự động fallback từ Redis sang In-Memory khi cần
 */
@Component
public class ExamSessionStoreManager {

    private static final Logger log = LoggerFactory.getLogger(ExamSessionStoreManager.class);

    // Valid timeout range: 0.5 to 4 hours
    private static final double MIN_TIMEOUT_HOURS = 0.5;
    private static final double MAX_TIMEOUT_HOURS = 4.0;
    private static final double DEFAULT_TIMEOUT_HOURS = 2.0;

    private final RedisExamSessionStore redisStore;
    private final InMemoryExamSessionStore inMemoryStore;
    private final Duration sessionTimeout;

    @Autowired
    public ExamSessionStoreManager(
            @Autowired(required = false) RedisExamSessionStore redisStore,
            InMemoryExamSessionStore inMemoryStore,
            @Value("${app.quiz.session-timeout-hours:2}") double sessionTimeoutHours
    ) {
        this.redisStore = redisStore;
        this.inMemoryStore = inMemoryStore;
        this.sessionTimeout = calculateTimeout(sessionTimeoutHours);
        
        log.info("ExamSessionStoreManager initialized. Redis available: {}, Session timeout: {} minutes",
                redisStore != null && redisStore.isAvailable(),
                sessionTimeout.toMinutes());
    }

    /**
     * Calculate and clamp timeout to valid range
     */
    private Duration calculateTimeout(double hours) {
        double clampedHours = Math.max(MIN_TIMEOUT_HOURS, Math.min(MAX_TIMEOUT_HOURS, hours));
        if (hours != clampedHours) {
            log.warn("Session timeout {} hours clamped to valid range [{}, {}]: {} hours",
                    hours, MIN_TIMEOUT_HOURS, MAX_TIMEOUT_HOURS, clampedHours);
        }
        return Duration.ofMinutes((long) (clampedHours * 60));
    }

    /**
     * Save exam session
     * Tự động chọn Redis hoặc In-Memory
     */
    public void save(String sessionKey, ExamSessionData session) {
        if (isRedisAvailable()) {
            redisStore.save(sessionKey, session, sessionTimeout);
        } else {
            log.warn("Redis unavailable, using in-memory fallback for session: {}", sessionKey);
            inMemoryStore.save(sessionKey, session, sessionTimeout);
        }
    }

    /**
     * Get exam session
     * Tự động chọn Redis hoặc In-Memory
     */
    public Optional<ExamSessionData> get(String sessionKey) {
        if (isRedisAvailable()) {
            return redisStore.get(sessionKey);
        }
        
        // Fallback to in-memory và check expiration manually
        Optional<ExamSessionData> session = inMemoryStore.get(sessionKey);
        if (session.isPresent() && session.get().isExpired(sessionTimeout)) {
            inMemoryStore.delete(sessionKey);
            return Optional.empty();
        }
        return session;
    }

    /**
     * Delete exam session
     * Xóa từ cả Redis và In-Memory để đảm bảo consistency
     */
    public void delete(String sessionKey) {
        if (isRedisAvailable()) {
            redisStore.delete(sessionKey);
        }
        // Always delete from in-memory as well (in case of fallback)
        inMemoryStore.delete(sessionKey);
    }

    /**
     * Get session timeout duration
     */
    public Duration getSessionTimeout() {
        return sessionTimeout;
    }

    /**
     * Check if Redis is available
     */
    public boolean isRedisAvailable() {
        return redisStore != null && redisStore.isAvailable();
    }

    /**
     * Cleanup expired sessions from in-memory store
     * Gọi bởi scheduled task
     */
    public int cleanupExpiredInMemorySessions() {
        return inMemoryStore.cleanupExpired(sessionTimeout);
    }

    /**
     * Get in-memory session count (for monitoring)
     */
    public int getInMemorySessionCount() {
        return inMemoryStore.getSessionCount();
    }

    /**
     * Build session key from userId and quizSetId
     */
    public static String buildSessionKey(Long userId, Long quizSetId) {
        return userId + "_" + quizSetId;
    }
}
