package com.htai.exe201phapluatso.credit.service;

import com.htai.exe201phapluatso.auth.entity.CreditTransaction;
import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.entity.UserCredit;
import com.htai.exe201phapluatso.auth.repo.CreditTransactionRepo;
import com.htai.exe201phapluatso.auth.repo.UserCreditRepo;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import com.htai.exe201phapluatso.common.exception.ForbiddenException;
import com.htai.exe201phapluatso.common.exception.InsufficientCreditsException;
import com.htai.exe201phapluatso.common.exception.NotFoundException;
import com.htai.exe201phapluatso.credit.dto.CreditBalanceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for managing user credits
 * Handles credit checking, deduction, and transaction logging
 * 
 * Performance optimizations:
 * - Pessimistic locking to prevent race conditions
 * - Single database transaction for check + deduct
 * - Efficient queries with proper indexing
 */
@Service
public class CreditService {

    private static final Logger log = LoggerFactory.getLogger(CreditService.class);

    private final UserCreditRepo userCreditRepo;
    private final CreditTransactionRepo transactionRepo;
    private final UserRepo userRepo;

    public CreditService(
            UserCreditRepo userCreditRepo,
            CreditTransactionRepo transactionRepo,
            UserRepo userRepo
    ) {
        this.userCreditRepo = userCreditRepo;
        this.transactionRepo = transactionRepo;
        this.userRepo = userRepo;
    }

    /**
     * Get credit balance for a user
     * 
     * @param userId User ID
     * @return Credit balance response
     */
    @Transactional
    public CreditBalanceResponse getCreditBalance(Long userId) {
        // Try to get existing credits
        UserCredit credits = userCreditRepo.findByUserId(userId).orElse(null);
        
        // If not exists, create FREE credits (fallback if trigger didn't work)
        if (credits == null) {
            log.info("Creating FREE credits for user {} (trigger fallback)", userId);
            credits = createFreeCredits(userId);
        }

        boolean isExpired = credits.getExpiresAt() != null 
                && LocalDateTime.now().isAfter(credits.getExpiresAt());

        // Determine plan name
        String planName = "FREE";
        if (credits.getChatCredits() > 10 || credits.getQuizGenCredits() > 0) {
            planName = credits.getQuizGenCredits() > 0 ? "STUDENT" : "REGULAR";
        }

        return new CreditBalanceResponse(
                credits.getChatCredits(),
                credits.getQuizGenCredits(),
                credits.getExpiresAt(),
                isExpired,
                planName
        );
    }
    
    /**
     * Create FREE credits for new user (fallback if trigger didn't work)
     */
    private UserCredit createFreeCredits(Long userId) {
        log.info("Attempting to create FREE credits for user ID: {}", userId);
        
        // Use getReferenceById instead of findById - more efficient and works within transaction
        User user = userRepo.getReferenceById(userId);
        
        log.info("Creating FREE credits for user {}", userId);
        
        UserCredit newCredit = new UserCredit();
        newCredit.setUser(user);
        newCredit.setChatCredits(10);
        newCredit.setQuizGenCredits(0);
        newCredit.setExpiresAt(null);
        newCredit.setUpdatedAt(LocalDateTime.now());
        
        UserCredit saved = userCreditRepo.save(newCredit);
        log.info("Successfully created credits for user {}", userId);
        
        // Log transaction
        logTransaction(userId, "BONUS", "CHAT", 10, 10, 
                "Welcome bonus - 10 free chat credits");
        
        return saved;
    }

    /**
     * Check and deduct 1 chat credit
     * Thread-safe with pessimistic locking
     * 
     * @param userId User ID
     * @throws ForbiddenException if insufficient credits or expired
     */
    @Transactional
    public void checkAndDeductChatCredit(Long userId) {
        log.debug("Checking and deducting chat credit for user {}", userId);

        // Get user credits with pessimistic lock to prevent race conditions
        UserCredit credits = userCreditRepo.findByUserIdWithLock(userId)
                .orElseThrow(() -> new NotFoundException("User credits not found"));

        // Check expiration
        if (credits.getExpiresAt() != null && LocalDateTime.now().isAfter(credits.getExpiresAt())) {
            log.warn("User {} credits expired at {}", userId, credits.getExpiresAt());
            throw new ForbiddenException("Credits đã hết hạn. Vui lòng mua thêm credits.");
        }

        // Check balance
        if (credits.getChatCredits() <= 0) {
            log.warn("User {} has insufficient chat credits: {}", userId, credits.getChatCredits());
            throw new ForbiddenException("Bạn đã hết lượt chat. Vui lòng mua thêm credits.");
        }

        // Deduct credit
        int oldBalance = credits.getChatCredits();
        credits.setChatCredits(oldBalance - 1);
        credits.setUpdatedAt(LocalDateTime.now());
        userCreditRepo.save(credits);

        // Log transaction
        logTransaction(userId, "USAGE", "CHAT", -1, credits.getChatCredits(), 
                "Used 1 chat credit");

        log.info("Deducted 1 chat credit from user {}. Balance: {} -> {}", 
                userId, oldBalance, credits.getChatCredits());
    }

    /**
     * Check and deduct 1 quiz generation credit (AI quiz generation only)
     * Thread-safe with pessimistic locking
     * 
     * @param userId User ID
     * @throws ForbiddenException if insufficient credits or expired
     */
    @Transactional
    public void checkAndDeductQuizGenCredit(Long userId) {
        log.debug("Checking and deducting quiz gen credit for user {}", userId);

        // Get user credits with pessimistic lock
        UserCredit credits = userCreditRepo.findByUserIdWithLock(userId)
                .orElseThrow(() -> new NotFoundException("User credits not found"));

        // Check expiration
        if (credits.getExpiresAt() != null && LocalDateTime.now().isAfter(credits.getExpiresAt())) {
            log.warn("User {} credits expired at {}", userId, credits.getExpiresAt());
            throw new ForbiddenException("Credits đã hết hạn. Vui lòng mua thêm credits.");
        }

        // Check balance
        if (credits.getQuizGenCredits() <= 0) {
            log.warn("User {} has insufficient quiz gen credits: {}", userId, credits.getQuizGenCredits());
            throw new ForbiddenException("Bạn đã hết lượt AI tạo đề. Vui lòng nâng cấp lên gói STUDENT.");
        }

        // Deduct credit
        int oldBalance = credits.getQuizGenCredits();
        credits.setQuizGenCredits(oldBalance - 1);
        credits.setUpdatedAt(LocalDateTime.now());
        userCreditRepo.save(credits);

        // Log transaction
        logTransaction(userId, "USAGE", "QUIZ_GEN", -1, credits.getQuizGenCredits(), 
                "Used 1 AI quiz generation credit");

        log.info("Deducted 1 quiz gen credit from user {}. Balance: {} -> {}", 
                userId, oldBalance, credits.getQuizGenCredits());
    }

    /**
     * Add credits to user (for purchase or bonus)
     * 
     * @param userId User ID
     * @param chatCredits Chat credits to add
     * @param quizGenCredits Quiz gen credits to add
     * @param planCode Plan code (for transaction log)
     * @param expiresAt Expiration date (null for permanent)
     */
    @Transactional
    public void addCredits(Long userId, int chatCredits, int quizGenCredits, 
                          String planCode, LocalDateTime expiresAt) {
        log.info("Adding credits to user {}: chat={}, quizGen={}, plan={}", 
                userId, chatCredits, quizGenCredits, planCode);

        UserCredit credits = userCreditRepo.findByUserIdWithLock(userId)
                .orElseThrow(() -> new NotFoundException("User credits not found"));

        // Add credits
        credits.setChatCredits(credits.getChatCredits() + chatCredits);
        credits.setQuizGenCredits(credits.getQuizGenCredits() + quizGenCredits);
        credits.setExpiresAt(expiresAt);
        credits.setUpdatedAt(LocalDateTime.now());
        userCreditRepo.save(credits);

        // Log transactions
        if (chatCredits > 0) {
            logTransaction(userId, "PURCHASE", "CHAT", chatCredits, credits.getChatCredits(),
                    "Purchased " + chatCredits + " chat credits - Plan: " + planCode);
        }
        if (quizGenCredits > 0) {
            logTransaction(userId, "PURCHASE", "QUIZ_GEN", quizGenCredits, credits.getQuizGenCredits(),
                    "Purchased " + quizGenCredits + " quiz gen credits - Plan: " + planCode);
        }

        log.info("Credits added successfully. New balance: chat={}, quizGen={}", 
                credits.getChatCredits(), credits.getQuizGenCredits());
    }

    /**
     * Log credit transaction
     * Private helper method
     */
    private void logTransaction(Long userId, String type, String creditType, 
                               int amount, int balanceAfter, String description) {
        User user = userRepo.getReferenceById(userId);
        
        CreditTransaction transaction = new CreditTransaction();
        transaction.setUser(user);
        transaction.setType(type);
        transaction.setCreditType(creditType);
        transaction.setAmount(amount);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setDescription(description);
        transaction.setCreatedAt(LocalDateTime.now());
        
        transactionRepo.save(transaction);
    }

    /**
     * Check if user has sufficient credits (without deducting)
     * Useful for UI to show/hide features
     * 
     * @param userId User ID
     * @param creditType CHAT or QUIZ_GEN
     * @return true if has credits
     */
    @Transactional(readOnly = true)
    public boolean hasCredits(Long userId, String creditType) {
        UserCredit credits = userCreditRepo.findByUserId(userId)
                .orElse(null);

        if (credits == null) {
            return false;
        }

        // Check expiration
        if (credits.getExpiresAt() != null && LocalDateTime.now().isAfter(credits.getExpiresAt())) {
            return false;
        }

        // Check balance
        if ("CHAT".equals(creditType)) {
            return credits.getChatCredits() > 0;
        } else if ("QUIZ_GEN".equals(creditType)) {
            return credits.getQuizGenCredits() > 0;
        }

        return false;
    }
}
