package com.htai.exe201phapluatso.legal.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LegalDocumentDTO(
    Long id,
    String documentName,
    String documentCode,
    String documentType,
    String issuingBody,
    LocalDate effectiveDate,
    int totalArticles,
    String status,
    LocalDateTime createdAt
) {
}
