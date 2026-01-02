package com.htai.exe201phapluatso.quiz.repo;

import com.htai.exe201phapluatso.quiz.entity.QuizSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizSetRepo extends JpaRepository<QuizSet, Long> {
    List<QuizSet> findByCreatedById(Long userId);
    
    Page<QuizSet> findByCreatedById(Long userId, Pageable pageable);
    
    // Admin dashboard queries
    long countByCreatedById(Long userId);
    
    /**
     * Batch count quiz sets by user IDs (avoid N+1)
     * Returns list of [userId, count]
     */
    @Query(value = """
        SELECT created_by, COUNT(*) as count
        FROM quiz_sets
        WHERE created_by IN :userIds
        GROUP BY created_by
        """, nativeQuery = true)
    List<Object[]> countByUserIds(@Param("userIds") List<Long> userIds);
}
