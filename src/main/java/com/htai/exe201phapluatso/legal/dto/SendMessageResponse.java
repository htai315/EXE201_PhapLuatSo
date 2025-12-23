package com.htai.exe201phapluatso.legal.dto;

public record SendMessageResponse(
    Long sessionId,
    ChatMessageDTO userMessage,
    ChatMessageDTO assistantMessage
) {}
