package com.htai.exe201phapluatso.quiz.repo;

import com.htai.exe201phapluatso.quiz.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAttemptRepo extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findTop10ByUserIdAndQuizSetIdOrderByFinishedAtDesc(Long userId, Long quizSetId);
}


