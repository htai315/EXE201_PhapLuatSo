package com.htai.exe201phapluatso.quiz.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs phục vụ tính năng thi thử và lịch sử thi.
 */
public class ExamDtos {

    public record ExamQuestionDto(
            Long id,
            String questionText,
            String explanation,
            List<ExamOptionDto> options
    ) {}

    public record ExamOptionDto(
            String optionKey,
            String optionText
    ) {}

    public record StartExamResponse(
            Long quizSetId,
            String quizSetTitle,
            int totalQuestions,
            List<ExamQuestionDto> questions
    ) {}

    public record SubmitExamRequest(
            List<AnswerDto> answers
    ) {
        public record AnswerDto(
                Long questionId,
                String selectedOptionKey
        ) {}
    }

    public record SubmitExamResponse(
            Long attemptId,
            int totalQuestions,
            int correctCount,
            int scorePercent,
            List<WrongQuestionDto> wrongQuestions
    ) {}

    public record WrongQuestionDto(
            Long questionId,
            String questionText,
            String correctOptionKey,
            String selectedOptionKey
    ) {}

    public record ExamHistoryItemDto(
            Long attemptId,
            LocalDateTime finishedAt,
            int totalQuestions,
            int correctCount,
            int scorePercent
    ) {}

    public record ExamHistoryResponse(
            Long quizSetId,
            String quizSetTitle,
            List<ExamHistoryItemDto> attempts
    ) {}
}


