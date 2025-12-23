package com.htai.exe201phapluatso.legal.dto;

public record UploadLegalDocumentResponse(
    Long documentId,
    String documentName,
    int totalArticles,
    String message
) {
}
