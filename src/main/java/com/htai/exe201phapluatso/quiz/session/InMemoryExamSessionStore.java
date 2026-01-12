package com.htai.exe201phapluatso.quiz.session;

import com.htai.exe201phapluatso.quiz.dto.ExamSessionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-Memory implementation của ExamSessionStore
 * Sử dụng làm fallback khi Redis không khả dụng
 */
@Component
public class InMemoryExamSessionStore implements ExamSessionStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryExamSessionStore.class);

    private final ConcurrentHashMap<String, ExamSessionData> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(String sessionKey, ExamSessionData session, Duration ttl) {
        sessions.put(sessionKey, session);
        log.debug("Saved session to in-memory store: {}", sessionKey);
    }

    @Override
    public Optional<ExamSessionData> get(String sessionKey) {
        return Optional.ofNullable(sessions.get(sessionKey));
    }

    @Override
    public void delete(String sessionKey) {
        sessions.remove(sessionKey);
        log.debug("Deleted session from in-memory store: {}", sessionKey);
    }

    @Override
    public boolean isAvailable() {
        return true; // Always available
    }

    /**
     * Cleanup expired sessions
     * @param timeout Session timeout duration
     * @return Số session đã xóa
     */
    public int cleanupExpired(Duration timeout) {
        int beforeSize = sessions.size();
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired(timeout));
        int removed = beforeSize - sessions.size();
        if (removed > 0) {
            log.info("Cleaned up {} expired sessions from in-memory store. Remaining: {}", 
                    removed, sessions.size());
        }
        return removed;
    }

    /**
     * Get current session count (for monitoring)
     */
    public int getSessionCount() {
        return sessions.size();
    }
}
