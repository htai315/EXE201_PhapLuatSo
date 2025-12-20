package com.htai.exe201phapluatso.quiz.dto;

import com.htai.exe201phapluatso.quiz.entity.QuizSet;

import java.time.LocalDateTime;

public record QuizSetResponse(
        Long id,
        String title,
        String description,
        String status,
        String visibility,
        Long createdById,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long questionCount
) {
    public static QuizSetResponse from(QuizSet quizSet) {
        return new QuizSetResponse(
                quizSet.getId(),
                quizSet.getTitle(),
                quizSet.getDescription(),
                quizSet.getStatus(),
                quizSet.getVisibility(),
                quizSet.getCreatedBy().getId(),
                quizSet.getCreatedAt(),
                quizSet.getUpdatedAt(),
                null
        );
    }
}



