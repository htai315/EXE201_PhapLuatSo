package com.htai.exe201phapluatso.quiz.dto;

import java.util.List;

public record PagedQuizSetsResponse(
    List<QuizSetResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
    public static PagedQuizSetsResponse from(
            List<QuizSetResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        return new PagedQuizSetsResponse(
                content,
                page,
                size,
                totalElements,
                totalPages,
                page == 0,
                page >= totalPages - 1
        );
    }
}
