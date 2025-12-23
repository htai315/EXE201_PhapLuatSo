package com.htai.exe201phapluatso.ai.dto;

public record AIQuestionDTO(
    String question,
    String optionA,
    String optionB,
    String optionC,
    String optionD,
    String correctAnswer,
    String explanation
) {
    public void validate() {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question cannot be empty");
        }
        if (optionA == null || optionB == null || optionC == null || optionD == null) {
            throw new IllegalArgumentException("All options must be provided");
        }
        if (correctAnswer == null || !correctAnswer.matches("[ABCD]")) {
            throw new IllegalArgumentException("Correct answer must be A, B, C, or D");
        }
    }
}
