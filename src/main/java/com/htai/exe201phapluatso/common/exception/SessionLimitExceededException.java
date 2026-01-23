package com.htai.exe201phapluatso.common.exception;

/**
 * Exception thrown when a chat session reaches its 10-question limit.
 * Results in HTTP 429 Too Many Requests response.
 */
public class SessionLimitExceededException extends RuntimeException {

    public SessionLimitExceededException(String message) {
        super(message);
    }
}
