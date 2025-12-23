package com.htai.exe201phapluatso.legal.dto;

public record CitationDTO(
    Long articleId,
    String documentName,
    Integer articleNumber,
    String articleTitle,
    String contentPreview
) {
}
