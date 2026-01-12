package com.htai.exe201phapluatso.ai.dto;

import java.util.Set;

public record GenerateQuestionsRequest(
    String quizSetName,
    String description,
    int questionCount,
    Integer durationMinutes // null = default 45 phút
) {
    private static final Set<Integer> ALLOWED_COUNTS = Set.of(15, 20, 30, 40);
    
    public GenerateQuestionsRequest {
        if (!ALLOWED_COUNTS.contains(questionCount)) {
            throw new IllegalArgumentException("Question count must be 15, 20, 30, or 40");
        }
        // Validate duration if provided
        if (durationMinutes != null && (durationMinutes < 5 || durationMinutes > 180)) {
            throw new IllegalArgumentException("Duration must be between 5 and 180 minutes");
        }
    }
}
