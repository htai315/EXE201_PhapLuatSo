package com.htai.exe201phapluatso.quiz.dto;

import com.htai.exe201phapluatso.quiz.entity.QuizQuestion;
import com.htai.exe201phapluatso.quiz.entity.QuizQuestionOption;

import java.time.LocalDateTime;
import java.util.List;

public record QuestionResponse(
        Long id,
        String questionText,
        String explanation,
        Integer sortOrder,
        List<OptionResponse> options,
        LocalDateTime createdAt
) {
    public record OptionResponse(
            Long id,
            String optionKey,
            String optionText,
            boolean isCorrect
    ) {}

    public static QuestionResponse from(QuizQuestion question, List<QuizQuestionOption> options) {
        List<OptionResponse> optionResponses = options.stream()
                .map(opt -> new OptionResponse(
                        opt.getId(),
                        opt.getOptionKey(),
                        opt.getOptionText(),
                        opt.isCorrect()
                ))
                .toList();

        return new QuestionResponse(
                question.getId(),
                question.getQuestionText(),
                question.getExplanation(),
                question.getSortOrder(),
                optionResponses,
                question.getCreatedAt()
        );
    }
}





