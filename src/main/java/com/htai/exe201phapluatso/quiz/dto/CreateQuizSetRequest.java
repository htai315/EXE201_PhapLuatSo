package com.htai.exe201phapluatso.quiz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateQuizSetRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,
        
        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,
        
        @Min(value = 5, message = "Duration must be at least 5 minutes")
        @Max(value = 180, message = "Duration must not exceed 180 minutes")
        Integer durationMinutes // null = default 45 phút
) {}
