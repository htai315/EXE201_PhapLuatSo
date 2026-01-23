package com.htai.exe201phapluatso.legal.repo;

import com.htai.exe201phapluatso.legal.entity.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatSessionRepo extends JpaRepository<ChatSession, Long> {

    @Query("SELECT s FROM ChatSession s WHERE s.user.id = :userId ORDER BY s.updatedAt DESC")
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);

    @Query("SELECT s FROM ChatSession s WHERE s.user.id = :userId ORDER BY s.updatedAt DESC")
    Page<ChatSession> findByUserIdOrderByUpdatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT s FROM ChatSession s WHERE s.user.id = :userId " +
            "AND LOWER(s.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "ORDER BY s.updatedAt DESC")
    Page<ChatSession> searchByUserIdAndTitle(
            @Param("userId") Long userId,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.session.id = :sessionId")
    long countMessagesBySessionId(Long sessionId);

    /**
     * Batch count messages by session IDs (avoid N+1)
     * Returns list of [sessionId, count]
     */
    @Query(value = """
            SELECT session_id, COUNT(*) as count
            FROM chat_messages
            WHERE session_id IN :sessionIds
            GROUP BY session_id
            """, nativeQuery = true)
    List<Object[]> countMessagesBySessionIds(@Param("sessionIds") List<Long> sessionIds);

    // Admin dashboard queries
    long countByUserId(Long userId);

    /**
     * Batch count chat sessions by user IDs (avoid N+1)
     * Returns list of [userId, count]
     */
    @Query(value = """
            SELECT user_id, COUNT(*) as count
            FROM chat_sessions
            WHERE user_id IN :userIds
            GROUP BY user_id
            """, nativeQuery = true)
    List<Object[]> countByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * Find session by ID ensuring it belongs to the specified user.
     * Used for ownership validation in session billing.
     */
    @Query("SELECT s FROM ChatSession s WHERE s.id = :sessionId AND s.user.id = :userId")
    java.util.Optional<ChatSession> findByIdAndUserId(
            @Param("sessionId") Long sessionId,
            @Param("userId") Long userId);
}
