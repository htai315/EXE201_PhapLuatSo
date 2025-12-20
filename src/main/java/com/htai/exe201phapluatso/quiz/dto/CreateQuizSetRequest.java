package com.htai.exe201phapluatso.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateQuizSetRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,
        
        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description
) {}
