package com.htai.exe201phapluatso.ai.dto;

public record GenerateQuestionsRequest(
    String quizSetName,
    String description,
    int questionCount
) {
    public GenerateQuestionsRequest {
        if (questionCount < 5 || questionCount > 50) {
            throw new IllegalArgumentException("Question count must be between 5 and 50");
        }
    }
}
