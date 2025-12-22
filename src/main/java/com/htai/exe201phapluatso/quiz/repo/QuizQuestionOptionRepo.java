package com.htai.exe201phapluatso.quiz.repo;

import com.htai.exe201phapluatso.quiz.entity.QuizQuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizQuestionOptionRepo extends JpaRepository<QuizQuestionOption, Long> {
    List<QuizQuestionOption> findByQuestionIdOrderByOptionKeyAsc(Long questionId);

    @Modifying
    @Query("DELETE FROM QuizQuestionOption o WHERE o.question.id = :questionId")
    void deleteByQuestionId(Long questionId);
    
    @Modifying
    @Query("DELETE FROM QuizQuestionOption o WHERE o.question.id IN :questionIds")
    void deleteByQuestionIds(@Param("questionIds") List<Long> questionIds);
}