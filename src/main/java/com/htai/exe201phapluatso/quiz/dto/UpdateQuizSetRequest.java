package com.htai.exe201phapluatso.quiz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Request DTO để update quiz set
 * Tất cả fields đều optional - chỉ update những field được truyền
 */
public record UpdateQuizSetRequest(
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,
        
        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,
        
        @Min(value = 5, message = "Thời gian làm bài phải từ 5 phút")
        @Max(value = 180, message = "Thời gian làm bài không được quá 180 phút")
        Integer durationMinutes
) {}
