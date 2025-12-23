package com.htai.exe201phapluatso.legal.dto;

import java.time.LocalDateTime;

public record ChatSessionDTO(
    Long id,
    String title,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    long messageCount
) {}
