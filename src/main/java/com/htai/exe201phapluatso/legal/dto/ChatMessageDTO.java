package com.htai.exe201phapluatso.legal.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ChatMessageDTO(
    Long id,
    String role,
    String content,
    List<CitationDTO> citations,
    LocalDateTime createdAt
) {}
