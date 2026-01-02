package com.htai.exe201phapluatso.ai.dto;

import java.util.Set;

public record GenerateQuestionsRequest(
    String quizSetName,
    String description,
    int questionCount
) {
    private static final Set<Integer> ALLOWED_COUNTS = Set.of(15, 20, 30, 40);
    
    public GenerateQuestionsRequest {
        if (!ALLOWED_COUNTS.contains(questionCount)) {
            throw new IllegalArgumentException("Question count must be 15, 20, 30, or 40");
        }
    }
}
