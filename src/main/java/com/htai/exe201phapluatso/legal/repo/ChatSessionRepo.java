package com.htai.exe201phapluatso.legal.repo;

import com.htai.exe201phapluatso.legal.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatSessionRepo extends JpaRepository<ChatSession, Long> {
    
    @Query("SELECT s FROM ChatSession s WHERE s.user.id = :userId ORDER BY s.updatedAt DESC")
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);
    
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.session.id = :sessionId")
    long countMessagesBySessionId(Long sessionId);
}
