package com.htai.exe201phapluatso.common.exception;

import com.htai.exe201phapluatso.auth.dto.LockoutInfo;

/**
 * Exception thrown when a user account is locked due to too many failed login attempts.
 */
public class AccountLockedException extends RuntimeException {
    
    private final LockoutInfo lockoutInfo;

    public AccountLockedException(String message, LockoutInfo lockoutInfo) {
        super(message);
        this.lockoutInfo = lockoutInfo;
    }

    public LockoutInfo getLockoutInfo() {
        return lockoutInfo;
    }
}
