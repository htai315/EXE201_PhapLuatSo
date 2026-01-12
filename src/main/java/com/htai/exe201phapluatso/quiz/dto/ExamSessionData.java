package com.htai.exe201phapluatso.quiz.dto;

import com.htai.exe201phapluatso.quiz.dto.ExamDtos.ExamOptionDto;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Data class để lưu thông tin exam session
 * Hỗ trợ serialization cho Redis storage
 */
public record ExamSessionData(
        Map<Long, String> correctKeyMapping,
        Map<Long, List<ExamOptionDto>> shuffledOptionsMapping,
        LocalDateTime startedAt,
        long createdAt
) implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor với auto-generated timestamps
     */
    public ExamSessionData(
            Map<Long, String> correctKeyMapping,
            Map<Long, List<ExamOptionDto>> shuffledOptionsMapping
    ) {
        this(correctKeyMapping, shuffledOptionsMapping, LocalDateTime.now(), System.currentTimeMillis());
    }

    /**
     * Check if session is expired based on timeout duration
     */
    public boolean isExpired(Duration timeout) {
        return System.currentTimeMillis() - createdAt > timeout.toMillis();
    }
}
