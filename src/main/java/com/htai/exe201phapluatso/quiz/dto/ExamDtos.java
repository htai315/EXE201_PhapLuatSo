package com.htai.exe201phapluatso.quiz.dto;

import java.io.Serializable;
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
            List<ExamOptionDto> options,
            String correctOptionKey  // Null khi gửi về frontend (ẩn đáp án), chỉ dùng internal
    ) {}

    public record ExamOptionDto(
            String optionKey,
            String optionText
    ) implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    public record StartExamResponse(
            Long quizSetId,
            String quizSetTitle,
            int totalQuestions,
            int durationMinutes, // Thời gian làm bài (phút)
            List<ExamQuestionDto> questions
    ) {}

    public record SubmitExamRequest(
            List<AnswerDto> answers
    ) {
        public record AnswerDto(
                Long questionId,
                String selectedOptionKey
                // NOTE: correctOptionKey đã bị xóa - server tự validate từ session
        ) {}
    }

    public record SubmitExamResponse(
            Long attemptId,
            int totalQuestions,
            int correctCount,
            int scorePercent,
            double scoreOutOf10,
            List<WrongQuestionDto> wrongQuestions
    ) {}

    public record WrongQuestionDto(
            Long questionId,
            String questionText,
            String correctOptionKey,
            String selectedOptionKey,
            String explanation,
            List<ExamOptionDto> options
    ) {}

    public record ExamHistoryItemDto(
            Long attemptId,
            LocalDateTime finishedAt,
            int totalQuestions,
            int correctCount,
            int scorePercent,
            double scoreOutOf10
    ) {}

    public record ExamHistoryResponse(
            Long quizSetId,
            String quizSetTitle,
            List<ExamHistoryItemDto> attempts
    ) {}
}


