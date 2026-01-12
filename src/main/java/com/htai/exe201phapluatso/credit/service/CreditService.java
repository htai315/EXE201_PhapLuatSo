package com.htai.exe201phapluatso.credit.service;

import com.htai.exe201phapluatso.auth.entity.CreditTransaction;
import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.entity.UserCredit;
import com.htai.exe201phapluatso.auth.repo.CreditTransactionRepo;
import com.htai.exe201phapluatso.auth.repo.UserCreditRepo;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import com.htai.exe201phapluatso.common.exception.BadRequestException;
import com.htai.exe201phapluatso.common.exception.ForbiddenException;
import com.htai.exe201phapluatso.common.exception.NotFoundException;
import com.htai.exe201phapluatso.credit.dto.CreditBalanceResponse;
import com.htai.exe201phapluatso.credit.entity.CreditReservation;
import com.htai.exe201phapluatso.credit.repo.CreditReservationRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for managing user credits
 * Supports reserve/confirm/refund pattern for AI operations
 * Uses optimistic locking with retry for concurrent access
 */
@Service
public class CreditService {

    private static final Logger log = LoggerFactory.getLogger(CreditService.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final UserCreditRepo userCreditRepo;
    private final CreditTransactionRepo transactionRepo;
    private final CreditReservationRepo reservationRepo;
    private final UserRepo userRepo;

    @Value("${credit.reservation.timeout-minutes:5}")
    private int reservationTimeoutMinutes;

    public CreditService(
            UserCreditRepo userCreditRepo,
            CreditTransactionRepo transactionRepo,
            CreditReservationRepo reservationRepo,
            UserRepo userRepo
    ) {
        this.userCreditRepo = userCreditRepo;
        this.transactionRepo = transactionRepo;
        this.reservationRepo = reservationRepo;
        this.userRepo = userRepo;
    }

    /**
     * Get credit balance for a user
     */
    @Transactional
    public CreditBalanceResponse getCreditBalance(Long userId) {
        UserCredit credits = userCreditRepo.findByUserId(userId).orElse(null);
        
        if (credits == null) {
            log.info("Creating FREE credits for user {} (trigger fallback)", userId);
            credits = createFreeCredits(userId);
        }

        boolean isExpired = credits.getExpiresAt() != null 
                && LocalDateTime.now().isAfter(credits.getExpiresAt());

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
    
    private UserCredit createFreeCredits(Long userId) {
        User user = userRepo.getReferenceById(userId);
        
        UserCredit newCredit = new UserCredit();
        newCredit.setUser(user);
        newCredit.setChatCredits(10);
        newCredit.setQuizGenCredits(0);
        newCredit.setExpiresAt(null);
        newCredit.setUpdatedAt(LocalDateTime.now());
        
        UserCredit saved = userCreditRepo.save(newCredit);
        
        logTransaction(userId, "BONUS", "CHAT", 10, 10, "Welcome bonus - 10 free chat credits");
        
        return saved;
    }

    // ==================== RESERVE/CONFIRM/REFUND PATTERN ====================

    /**
     * Reserve credit for an AI operation
     * Deducts credit immediately and creates a reservation for potential refund
     * Uses optimistic locking with retry
     * 
     * @param userId User ID
     * @param creditType CHAT or QUIZ_GEN
     * @param operationType AI_CHAT or AI_QUIZ_GEN
     * @return CreditReservation for tracking
     */
    @Transactional
    public CreditReservation reserveCredit(Long userId, String creditType, String operationType) {
        int attempts = 0;
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                return doReserveCredit(userId, creditType, operationType);
            } catch (OptimisticLockingFailureException e) {
                attempts++;
                log.warn("Optimistic lock conflict for user {}, attempt {}/{}", userId, attempts, MAX_RETRY_ATTEMPTS);
                if (attempts >= MAX_RETRY_ATTEMPTS) {
                    throw new BadRequestException("Hệ thống đang bận. Vui lòng thử lại sau.");
                }
                try {
                    Thread.sleep(100 * attempts); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new BadRequestException("Yêu cầu bị gián đoạn.");
                }
            }
        }
        throw new BadRequestException("Không thể xử lý yêu cầu. Vui lòng thử lại.");
    }

    private CreditReservation doReserveCredit(Long userId, String creditType, String operationType) {
        UserCredit credits = userCreditRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin credits."));

        // Check expiration
        if (credits.getExpiresAt() != null && LocalDateTime.now().isAfter(credits.getExpiresAt())) {
            throw new ForbiddenException("Credits đã hết hạn. Vui lòng mua thêm credits.");
        }

        // Check and deduct balance
        int oldBalance;
        if ("CHAT".equals(creditType)) {
            if (credits.getChatCredits() <= 0) {
                throw new ForbiddenException("Bạn đã hết lượt chat. Vui lòng mua thêm credits.");
            }
            oldBalance = credits.getChatCredits();
            credits.setChatCredits(oldBalance - 1);
        } else if ("QUIZ_GEN".equals(creditType)) {
            if (credits.getQuizGenCredits() <= 0) {
                throw new ForbiddenException("Bạn đã hết lượt AI tạo đề. Vui lòng nâng cấp lên gói STUDENT.");
            }
            oldBalance = credits.getQuizGenCredits();
            credits.setQuizGenCredits(oldBalance - 1);
        } else {
            throw new BadRequestException("Loại credit không hợp lệ: " + creditType);
        }

        credits.setUpdatedAt(LocalDateTime.now());
        userCreditRepo.save(credits); // Optimistic lock check happens here

        // Create reservation
        User user = userRepo.getReferenceById(userId);
        CreditReservation reservation = new CreditReservation();
        reservation.setUser(user);
        reservation.setCreditType(creditType);
        reservation.setAmount(1);
        reservation.setStatus(CreditReservation.STATUS_PENDING);
        reservation.setCreatedAt(LocalDateTime.now());
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(reservationTimeoutMinutes));
        reservation.setOperationType(operationType);
        reservationRepo.save(reservation);

        // Log transaction
        int newBalance = "CHAT".equals(creditType) ? credits.getChatCredits() : credits.getQuizGenCredits();
        logTransaction(userId, "RESERVE", creditType, -1, newBalance, 
                "Reserved 1 " + creditType.toLowerCase() + " credit for " + operationType);

        log.info("Reserved 1 {} credit for user {}. Balance: {} -> {}", 
                creditType, userId, oldBalance, newBalance);

        return reservation;
    }

    /**
     * Confirm a reservation (AI operation succeeded)
     */
    @Transactional
    public void confirmReservation(Long reservationId) {
        CreditReservation reservation = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy reservation."));

        if (!CreditReservation.STATUS_PENDING.equals(reservation.getStatus())) {
            log.warn("Reservation {} is not pending, status: {}", reservationId, reservation.getStatus());
            return; // Already processed
        }

        reservation.setStatus(CreditReservation.STATUS_CONFIRMED);
        reservation.setConfirmedAt(LocalDateTime.now());
        reservationRepo.save(reservation);

        // Log transaction
        UserCredit credits = userCreditRepo.findByUserId(reservation.getUser().getId()).orElse(null);
        int balance = credits != null ? 
                ("CHAT".equals(reservation.getCreditType()) ? credits.getChatCredits() : credits.getQuizGenCredits()) : 0;
        
        logTransaction(reservation.getUser().getId(), "CONFIRM", reservation.getCreditType(), 
                0, balance, "Confirmed " + reservation.getOperationType() + " operation");

        log.info("Confirmed reservation {} for user {}", reservationId, reservation.getUser().getId());
    }

    /**
     * Refund a reservation (AI operation failed)
     */
    @Transactional
    public void refundReservation(Long reservationId) {
        CreditReservation reservation = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy reservation."));

        if (!CreditReservation.STATUS_PENDING.equals(reservation.getStatus())) {
            log.warn("Reservation {} is not pending, status: {}", reservationId, reservation.getStatus());
            return; // Already processed
        }

        // Restore credit
        Long userId = reservation.getUser().getId();
        UserCredit credits = userCreditRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin credits."));

        int oldBalance;
        if ("CHAT".equals(reservation.getCreditType())) {
            oldBalance = credits.getChatCredits();
            credits.setChatCredits(oldBalance + reservation.getAmount());
        } else {
            oldBalance = credits.getQuizGenCredits();
            credits.setQuizGenCredits(oldBalance + reservation.getAmount());
        }
        credits.setUpdatedAt(LocalDateTime.now());
        userCreditRepo.save(credits);

        // Update reservation
        reservation.setStatus(CreditReservation.STATUS_REFUNDED);
        reservation.setRefundedAt(LocalDateTime.now());
        reservationRepo.save(reservation);

        // Log transaction
        int newBalance = "CHAT".equals(reservation.getCreditType()) ? 
                credits.getChatCredits() : credits.getQuizGenCredits();
        logTransaction(userId, "REFUND", reservation.getCreditType(), 
                reservation.getAmount(), newBalance, 
                "Refunded " + reservation.getAmount() + " credit - " + reservation.getOperationType() + " failed");

        log.info("Refunded {} {} credit(s) to user {}. Balance: {} -> {}", 
                reservation.getAmount(), reservation.getCreditType(), userId, oldBalance, newBalance);
    }

    // ==================== LEGACY METHODS (for backward compatibility) ====================

    /**
     * Check and deduct 1 chat credit (legacy method)
     * @deprecated Use reserveCredit/confirmReservation/refundReservation instead
     */
    @Transactional
    public void checkAndDeductChatCredit(Long userId) {
        log.debug("Checking and deducting chat credit for user {}", userId);

        UserCredit credits = userCreditRepo.findByUserIdWithLock(userId)
                .orElseThrow(() -> new NotFoundException("User credits not found"));

        if (credits.getExpiresAt() != null && LocalDateTime.now().isAfter(credits.getExpiresAt())) {
            throw new ForbiddenException("Credits đã hết hạn. Vui lòng mua thêm credits.");
        }

        if (credits.getChatCredits() <= 0) {
            throw new ForbiddenException("Bạn đã hết lượt chat. Vui lòng mua thêm credits.");
        }

        int oldBalance = credits.getChatCredits();
        credits.setChatCredits(oldBalance - 1);
        credits.setUpdatedAt(LocalDateTime.now());
        userCreditRepo.save(credits);

        logTransaction(userId, "USAGE", "CHAT", -1, credits.getChatCredits(), "Used 1 chat credit");

        log.info("Deducted 1 chat credit from user {}. Balance: {} -> {}", 
                userId, oldBalance, credits.getChatCredits());
    }

    /**
     * Check and deduct 1 quiz generation credit (legacy method)
     * @deprecated Use reserveCredit/confirmReservation/refundReservation instead
     */
    @Transactional
    public void checkAndDeductQuizGenCredit(Long userId) {
        log.debug("Checking and deducting quiz gen credit for user {}", userId);

        UserCredit credits = userCreditRepo.findByUserIdWithLock(userId)
                .orElseThrow(() -> new NotFoundException("User credits not found"));

        if (credits.getExpiresAt() != null && LocalDateTime.now().isAfter(credits.getExpiresAt())) {
            throw new ForbiddenException("Credits đã hết hạn. Vui lòng mua thêm credits.");
        }

        if (credits.getQuizGenCredits() <= 0) {
            throw new ForbiddenException("Bạn đã hết lượt AI tạo đề. Vui lòng nâng cấp lên gói STUDENT.");
        }

        int oldBalance = credits.getQuizGenCredits();
        credits.setQuizGenCredits(oldBalance - 1);
        credits.setUpdatedAt(LocalDateTime.now());
        userCreditRepo.save(credits);

        logTransaction(userId, "USAGE", "QUIZ_GEN", -1, credits.getQuizGenCredits(), 
                "Used 1 AI quiz generation credit");

        log.info("Deducted 1 quiz gen credit from user {}. Balance: {} -> {}", 
                userId, oldBalance, credits.getQuizGenCredits());
    }

    /**
     * Add credits to user (for purchase or bonus)
     */
    @Transactional
    public void addCredits(Long userId, int chatCredits, int quizGenCredits, 
                          String planCode, LocalDateTime expiresAt) {
        log.info("Adding credits to user {}: chat={}, quizGen={}, plan={}", 
                userId, chatCredits, quizGenCredits, planCode);

        UserCredit credits = userCreditRepo.findByUserIdWithLock(userId)
                .orElseThrow(() -> new NotFoundException("User credits not found"));

        credits.setChatCredits(credits.getChatCredits() + chatCredits);
        credits.setQuizGenCredits(credits.getQuizGenCredits() + quizGenCredits);
        credits.setExpiresAt(expiresAt);
        credits.setUpdatedAt(LocalDateTime.now());
        userCreditRepo.save(credits);

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
     */
    @Transactional(readOnly = true)
    public boolean hasCredits(Long userId, String creditType) {
        UserCredit credits = userCreditRepo.findByUserId(userId).orElse(null);

        if (credits == null) {
            return false;
        }

        if (credits.getExpiresAt() != null && LocalDateTime.now().isAfter(credits.getExpiresAt())) {
            return false;
        }

        if ("CHAT".equals(creditType)) {
            return credits.getChatCredits() > 0;
        } else if ("QUIZ_GEN".equals(creditType)) {
            return credits.getQuizGenCredits() > 0;
        }

        return false;
    }
}
