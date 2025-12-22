package com.htai.exe201phapluatso.quiz.repo;

import com.htai.exe201phapluatso.quiz.entity.QuizAttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizAttemptAnswerRepo extends JpaRepository<QuizAttemptAnswer, Long> {

    List<QuizAttemptAnswer> findByAttemptIdOrderByQuestionIdAsc(Long attemptId);
    
    @Modifying
    @Query("DELETE FROM QuizAttemptAnswer a WHERE a.question.id = :questionId")
    void deleteByQuestionId(Long questionId);
    
    @Modifying
    @Query("DELETE FROM QuizAttemptAnswer a WHERE a.question.id IN :questionIds")
    void deleteByQuestionIds(@Param("questionIds") List<Long> questionIds);
}

