package com.htai.exe201phapluatso.legal.dto;

public record UploadLegalDocumentRequest(
    String documentName,
    String documentCode,
    String documentType,
    String issuingBody,
    String effectiveDate  // Format: yyyy-MM-dd
) {
}
