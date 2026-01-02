package com.htai.exe201phapluatso.quiz.repo;

import com.htai.exe201phapluatso.quiz.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizQuestionRepo extends JpaRepository<QuizQuestion, Long> {
    List<QuizQuestion> findByQuizSetIdOrderBySortOrderAsc(Long quizSetId);
    
    /**
     * Fix N+1 query problem: Fetch questions with options in single query using JOIN FETCH
     * Performance: 1 query thay vì N+1 queries (N = số câu hỏi)
     */
    @Query("SELECT DISTINCT q FROM QuizQuestion q " +
           "LEFT JOIN FETCH q.options " +
           "WHERE q.quizSet.id = :quizSetId " +
           "ORDER BY q.sortOrder ASC")
    List<QuizQuestion> findByQuizSetIdWithOptions(@Param("quizSetId") Long quizSetId);
    
    long countByQuizSetId(Long quizSetId);
    
    /**
     * Batch count questions by quiz set IDs (avoid N+1 in list views)
     * Returns list of [quizSetId, count]
     */
    @Query(value = """
        SELECT quiz_set_id, COUNT(*) as count
        FROM quiz_questions
        WHERE quiz_set_id IN :quizSetIds
        GROUP BY quiz_set_id
        """, nativeQuery = true)
    List<Object[]> countByQuizSetIds(@Param("quizSetIds") List<Long> quizSetIds);
    
    @Modifying
    @Query("DELETE FROM QuizQuestion q WHERE q.quizSet.id = :quizSetId")
    void deleteByQuizSetId(@Param("quizSetId") Long quizSetId);
}