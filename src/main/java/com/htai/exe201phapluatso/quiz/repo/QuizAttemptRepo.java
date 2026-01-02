package com.htai.exe201phapluatso.quiz.repo;

import com.htai.exe201phapluatso.quiz.entity.QuizAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuizAttemptRepo extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findTop10ByUserIdAndQuizSetIdOrderByFinishedAtDesc(Long userId, Long quizSetId);
    
    /**
     * Lấy tất cả attempts của user với phân trang - JOIN FETCH để tránh N+1
     * countQuery riêng để tránh lỗi pagination với JOIN FETCH
     */
    @Query(value = "SELECT qa FROM QuizAttempt qa JOIN FETCH qa.quizSet WHERE qa.user.id = :userId ORDER BY qa.finishedAt DESC",
           countQuery = "SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.user.id = :userId")
    Page<QuizAttempt> findByUserIdWithQuizSet(Long userId, Pageable pageable);
    
    /**
     * Đếm tổng số attempts của user
     */
    long countByUserId(Long userId);
    
    @Modifying
    @Query("DELETE FROM QuizAttempt qa WHERE qa.quizSet.id = :quizSetId")
    void deleteByQuizSetId(Long quizSetId);
}


