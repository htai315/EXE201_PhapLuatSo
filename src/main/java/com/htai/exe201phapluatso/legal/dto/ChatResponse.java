package com.htai.exe201phapluatso.legal.dto;

import java.util.List;

public record ChatResponse(
    String answer,
    List<CitationDTO> citations
) {
}
