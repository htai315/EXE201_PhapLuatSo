package com.htai.exe201phapluatso.quiz.repo;

import com.htai.exe201phapluatso.quiz.entity.QuizSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizSetRepo extends JpaRepository<QuizSet, Long> {
    List<QuizSet> findByCreatedById(Long userId);
    
    Page<QuizSet> findByCreatedById(Long userId, Pageable pageable);
}
