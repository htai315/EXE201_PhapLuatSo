package com.htai.exe201phapluatso.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateQuestionRequest(
        @NotBlank(message = "Question text is required")
        @Size(max = 2000, message = "Question text must not exceed 2000 characters")
        String questionText,
        
        @Size(max = 2000, message = "Explanation must not exceed 2000 characters")
        String explanation,
        
        @NotNull(message = "Options are required")
        @Valid
        List<OptionDto> options
) {
    public record OptionDto(
            @NotBlank(message = "Option key is required")
            String optionKey,
            
            @NotBlank(message = "Option text is required")
            @Size(max = 1000, message = "Option text must not exceed 1000 characters")
            String optionText,
            
            boolean isCorrect
    ) {}
}
