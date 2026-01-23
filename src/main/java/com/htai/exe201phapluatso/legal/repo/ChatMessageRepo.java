package com.htai.exe201phapluatso.legal.repo;

import com.htai.exe201phapluatso.legal.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepo extends JpaRepository<ChatMessage, Long> {

    /**
     * Fetch messages with citations AND their documents in one query.
     * DISTINCT prevents duplicate messages from ManyToMany join.
     * 2-level fetch: m.citations -> c.document avoids LazyInitializationException.
     *
     * WARNING: Do NOT add Pageable to this method!
     * JOIN FETCH with collection + pagination causes Hibernate to fetch ALL rows
     * then paginate in-memory (HHH90003004), destroying performance.
     * Current session limit (10 questions) keeps result set small enough.
     */
    @Query("""
            SELECT DISTINCT m
            FROM ChatMessage m
            LEFT JOIN FETCH m.citations c
            LEFT JOIN FETCH c.document
            WHERE m.session.id = :sessionId
            ORDER BY m.createdAt ASC, m.id ASC
            """)
    List<ChatMessage> findBySessionIdWithCitations(@Param("sessionId") Long sessionId);

    // Admin dashboard queries
    long countByRole(String role);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.session.user.id = :userId AND m.role = :role")
    long countBySessionUserIdAndRole(@Param("userId") Long userId, @Param("role") String role);

    // Delete citations for specific article IDs (used when deleting legal
    // documents)
    @Modifying
    @Query(value = "DELETE FROM chat_message_citations WHERE article_id IN :articleIds", nativeQuery = true)
    void deleteCitationsByArticleIds(@Param("articleIds") List<Long> articleIds);
}
