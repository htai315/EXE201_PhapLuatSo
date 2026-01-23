package com.htai.exe201phapluatso.common.exception;

/**
 * Exception thrown when the AI chat service fails to generate a response.
 * Results in HTTP 502 Bad Gateway response.
 */
public class AiChatFailedException extends RuntimeException {

    public AiChatFailedException(String message) {
        super(message);
    }

    public AiChatFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
