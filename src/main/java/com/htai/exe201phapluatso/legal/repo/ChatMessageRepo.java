package com.htai.exe201phapluatso.legal.repo;

import com.htai.exe201phapluatso.legal.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatMessageRepo extends JpaRepository<ChatMessage, Long> {
    
    @Query("SELECT m FROM ChatMessage m LEFT JOIN FETCH m.citations WHERE m.session.id = :sessionId ORDER BY m.createdAt ASC")
    List<ChatMessage> findBySessionIdWithCitations(Long sessionId);
}
