package com.htai.exe201phapluatso.quiz.repo;

import com.htai.exe201phapluatso.quiz.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuizAttemptRepo extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findTop10ByUserIdAndQuizSetIdOrderByFinishedAtDesc(Long userId, Long quizSetId);
    
    @Modifying
    @Query("DELETE FROM QuizAttempt qa WHERE qa.quizSet.id = :quizSetId")
    void deleteByQuizSetId(Long quizSetId);
}


