package com.htai.exe201phapluatso.common.exception;

/**
 * Exception thrown when a session is already being charged (duplicate
 * first-question attempt).
 * Results in HTTP 409 Conflict response.
 */
public class SessionAlreadyChargingException extends RuntimeException {

    public SessionAlreadyChargingException(String message) {
        super(message);
    }
}
