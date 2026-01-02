package com.htai.exe201phapluatso.quiz.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO cho lịch sử làm bài có phân trang
 */
public record PagedExamHistoryResponse(
        List<AttemptItem> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public record AttemptItem(
            Long attemptId,
            Long quizSetId,
            String quizSetTitle,
            LocalDateTime finishedAt,
            int totalQuestions,
            int correctCount,
            int scorePercent,
            double scoreOutOf10
    ) {}
}
