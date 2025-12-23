package com.htai.exe201phapluatso.ai.dto;

import java.util.List;

public record GenerateQuestionsResponse(
    Long quizSetId,
    String quizSetName,
    int totalQuestions,
    List<AIQuestionDTO> questions
) {}
