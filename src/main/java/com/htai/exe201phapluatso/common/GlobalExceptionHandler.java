package com.htai.exe201phapluatso.common;

import com.htai.exe201phapluatso.auth.dto.LockoutInfo;
import com.htai.exe201phapluatso.common.exception.AccountLockedException;
import com.htai.exe201phapluatso.common.exception.AiChatFailedException;
import com.htai.exe201phapluatso.common.exception.BadRequestException;
import com.htai.exe201phapluatso.common.exception.ForbiddenException;
import com.htai.exe201phapluatso.common.exception.InsufficientCreditsException;
import com.htai.exe201phapluatso.common.exception.NotFoundException;
import com.htai.exe201phapluatso.common.exception.RateLimitExceededException;
import com.htai.exe201phapluatso.common.exception.SessionAlreadyChargingException;
import com.htai.exe201phapluatso.common.exception.SessionLimitExceededException;
import com.htai.exe201phapluatso.common.exception.TokenReusedException;
import com.htai.exe201phapluatso.common.exception.UnauthorizedException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<?> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<?> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<?> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InsufficientCreditsException.class)
    public ResponseEntity<?> handleInsufficientCredits(InsufficientCreditsException ex) {
        log.warn("Insufficient credits: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(Map.of(
                        "error", ex.getMessage(),
                        "code", "INSUFFICIENT_CREDITS"));
    }

    /**
     * Handle session already charging exception (HTTP 409 Conflict)
     * Thrown when a duplicate first-question attempt is detected (double-click/two
     * tabs)
     */
    @ExceptionHandler(SessionAlreadyChargingException.class)
    public ResponseEntity<?> handleSessionAlreadyCharging(SessionAlreadyChargingException ex) {
        log.info("Session already charging: {}", ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getMessage());
        body.put("code", "SESSION_ALREADY_CHARGING");

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Handle optimistic locking failure (HTTP 409 Conflict)
     * Thrown when concurrent updates conflict on session version
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<?> handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.warn("Optimistic locking conflict: {}", ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Phiên đang được cập nhật. Vui lòng thử lại.");
        body.put("code", "CONCURRENT_UPDATE");

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Handle AI chat failure (HTTP 502 Bad Gateway)
     * Thrown when AI service fails to generate a response
     */
    @ExceptionHandler(AiChatFailedException.class)
    public ResponseEntity<?> handleAiChatFailed(AiChatFailedException ex) {
        log.error("AI chat failed: {}", ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getMessage());
        body.put("code", "AI_SERVICE_ERROR");

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    /**
     * Handle account locked exception (HTTP 423 Locked)
     */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<?> handleAccountLocked(AccountLockedException ex) {
        log.warn("Account locked: {}", ex.getMessage());
        LockoutInfo info = ex.getLockoutInfo();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getMessage()); // Tiếng Việt message
        body.put("code", "ACCOUNT_LOCKED");
        if (info != null) {
            body.put("lockedUntil", info.lockedUntil());
            body.put("remainingSeconds", info.remainingSeconds());
            body.put("failedAttempts", info.failedAttempts());
        }

        return ResponseEntity.status(HttpStatus.LOCKED).body(body);
    }

    /**
     * Handle rate limit exceeded exception (HTTP 429 Too Many Requests)
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<?> handleRateLimitExceeded(RateLimitExceededException ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getMessage()); // Tiếng Việt message
        body.put("code", "RATE_LIMIT_EXCEEDED");
        body.put("retryAfter", ex.getRetryAfterSeconds());
        body.put("limit", ex.getLimit());
        body.put("remaining", ex.getRemaining());

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(body);
    }

    /**
     * Handle session limit exceeded exception (HTTP 429 Too Many Requests)
     * Thrown when a chat session reaches its 10-question limit
     */
    @ExceptionHandler(SessionLimitExceededException.class)
    public ResponseEntity<?> handleSessionLimitExceeded(SessionLimitExceededException ex) {
        log.info("Session limit exceeded: {}", ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getMessage());
        body.put("code", "SESSION_LIMIT_EXCEEDED");

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
    }

    /**
     * Handle token reuse exception (HTTP 401 Unauthorized)
     * This is a security breach - all tokens have been revoked
     */
    @ExceptionHandler(TokenReusedException.class)
    public ResponseEntity<?> handleTokenReuse(TokenReusedException ex) {
        log.error("TOKEN REUSE DETECTED for user {}", ex.getUserId());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getMessage()); // Tiếng Việt message
        body.put("code", "TOKEN_REUSE_DETECTED");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() != null
                                ? error.getDefaultMessage()
                                : "Invalid value",
                        (first, second) -> first));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Validation failed", "fields", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraintViolation(ConstraintViolationException ex) {
        var errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage(),
                        (first, second) -> first));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Validation failed", "fields", errors));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex) {
        log.error("Runtime exception occurred: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau"));
    }
}
