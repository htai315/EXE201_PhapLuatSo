package com.htai.exe201phapluatso.auth.service;

import com.htai.exe201phapluatso.auth.dto.LockoutInfo;
import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Service for managing account lockout after failed login attempts.
 */
@Service
public class AccountLockoutService {

    private static final Logger log = LoggerFactory.getLogger(AccountLockoutService.class);

    private final UserRepo userRepo;
    private final SecurityAuditService auditService;

    @Value("${app.security.lockout.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.security.lockout.duration-minutes:15}")
    private int lockoutDurationMinutes;

    public AccountLockoutService(UserRepo userRepo, SecurityAuditService auditService) {
        this.userRepo = userRepo;
        this.auditService = auditService;
    }

    /**
     * Record a failed login attempt for a user.
     * @return true if account is now locked
     */
    @Transactional
    public boolean recordFailedAttempt(User user, String ipAddress) {
        // Check if already locked
        if (user.isLocked()) {
            log.debug("User {} is already locked until {}", user.getId(), user.getLockedUntil());
            return true;
        }

        int newAttempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(newAttempts);

        if (newAttempts >= maxAttempts) {
            // Lock the account
            LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(lockoutDurationMinutes);
            user.setLockedUntil(lockedUntil);
            userRepo.save(user);

            // Log the lockout event
            auditService.logAccountLocked(
                    user.getId(),
                    user.getEmail(),
                    ipAddress,
                    "Đăng nhập sai quá nhiều lần (" + newAttempts + " lần)",
                    lockoutDurationMinutes
            );

            log.warn("Account locked for user {} ({}) until {} after {} failed attempts",
                    user.getId(), user.getEmail(), lockedUntil, newAttempts);
            return true;
        }

        userRepo.save(user);
        log.debug("Failed login attempt {} of {} for user {}", newAttempts, maxAttempts, user.getId());
        return false;
    }

    /**
     * Check if a user account is currently locked.
     */
    public boolean isAccountLocked(User user) {
        if (user.getLockedUntil() == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(user.getLockedUntil())) {
            // Lockout has expired, auto-unlock
            return false;
        }

        return true;
    }

    /**
     * Get lockout information for a user.
     */
    public LockoutInfo getLockoutInfo(User user) {
        boolean isLocked = isAccountLocked(user);
        long remainingSeconds = 0;

        if (isLocked && user.getLockedUntil() != null) {
            remainingSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), user.getLockedUntil());
            if (remainingSeconds < 0) {
                remainingSeconds = 0;
            }
        }

        return new LockoutInfo(
                isLocked,
                user.getFailedLoginAttempts(),
                user.getLockedUntil(),
                remainingSeconds
        );
    }

    /**
     * Reset failed attempts on successful login.
     */
    @Transactional
    public void resetFailedAttempts(User user) {
        if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepo.save(user);
            log.debug("Reset failed login attempts for user {}", user.getId());
        }
    }

    /**
     * Manually unlock an account (admin action).
     */
    @Transactional
    public void unlockAccount(User user) {
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepo.save(user);
        log.info("Account manually unlocked for user {} ({})", user.getId(), user.getEmail());
    }
}
