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
        Pageable pageable
    );
    
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.session.id = :sessionId")
    long countMessagesBySessionId(Long sessionId);
}
