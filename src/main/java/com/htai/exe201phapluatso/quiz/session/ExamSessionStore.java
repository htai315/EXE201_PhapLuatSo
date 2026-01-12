package com.htai.exe201phapluatso.quiz.session;

import com.htai.exe201phapluatso.quiz.dto.ExamSessionData;

import java.time.Duration;
import java.util.Optional;

/**
 * Interface cho exam session storage
 * Hỗ trợ Redis (primary) và In-Memory (fallback)
 */
public interface ExamSessionStore {

    /**
     * Save exam session với TTL
     * @param sessionKey Key dạng "userId_quizSetId"
     * @param session Session data
     * @param ttl Time-to-live
     */
    void save(String sessionKey, ExamSessionData session, Duration ttl);

    /**
     * Get exam session by key
     * @param sessionKey Key dạng "userId_quizSetId"
     * @return Optional chứa session nếu tồn tại và chưa expired
     */
    Optional<ExamSessionData> get(String sessionKey);

    /**
     * Delete exam session
     * @param sessionKey Key dạng "userId_quizSetId"
     */
    void delete(String sessionKey);

    /**
     * Check if store is available
     * @return true nếu store có thể sử dụng
     */
    boolean isAvailable();
}
