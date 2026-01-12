package com.htai.exe201phapluatso.admin.service;

import com.htai.exe201phapluatso.admin.dto.CreditAnalyticsResponse;
import com.htai.exe201phapluatso.auth.entity.CreditTransaction;
import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.entity.UserCredit;
import com.htai.exe201phapluatso.auth.repo.CreditTransactionRepo;
import com.htai.exe201phapluatso.auth.repo.UserCreditRepo;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import com.htai.exe201phapluatso.common.exception.BadRequestException;
import com.htai.exe201phapluatso.common.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for admin credit management operations
 */
@Service
public class AdminCreditService {

    private static final Logger log = LoggerFactory.getLogger(AdminCreditService.class);

    private final UserCreditRepo userCreditRepo;
    private final CreditTransactionRepo transactionRepo;
    private final UserRepo userRepo;
    private final AdminActivityLogService activityLogService;

    public AdminCreditService(
            UserCreditRepo userCreditRepo,
            CreditTransactionRepo transactionRepo,
            UserRepo userRepo,
            AdminActivityLogService activityLogService
    ) {
        this.userCreditRepo = userCreditRepo;
        this.transactionRepo = transactionRepo;
        this.userRepo = userRepo;
        this.activityLogService = activityLogService;
    }

    /**
     * Add credits to a user (admin operation)
     */
    @Transactional
    public void addCredits(Long userId, int chatCredits, int quizGenCredits, 
                          String reason, User adminUser) {
        if (chatCredits <= 0 && quizGenCredits <= 0) {
            throw new BadRequestException("Phải thêm ít nhất 1 loại credit.");
        }

        User targetUser = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy user."));

        // Try to find existing credits, or create new if not exists (for legacy users)
        UserCredit credits = userCreditRepo.findByUserIdWithLock(userId)
                .orElseGet(() -> {
                    log.info("Creating new UserCredit record for user {} (legacy user without credits)", userId);
                    UserCredit newCredits = new UserCredit();
                    newCredits.setUser(targetUser);
                    newCredits.setChatCredits(0);
                    newCredits.setQuizGenCredits(0);
                    newCredits.setUpdatedAt(LocalDateTime.now());
                    return userCreditRepo.save(newCredits);
                });

        // Add credits
        if (chatCredits > 0) {
            int oldBalance = credits.getChatCredits();
            credits.setChatCredits(oldBalance + chatCredits);
            logTransaction(userId, "ADMIN_ADD", "CHAT", chatCredits, credits.getChatCredits(),
                    "Admin added " + chatCredits + " chat credits. Reason: " + reason);
        }

        if (quizGenCredits > 0) {
            int oldBalance = credits.getQuizGenCredits();
            credits.setQuizGenCredits(oldBalance + quizGenCredits);
            logTransaction(userId, "ADMIN_ADD", "QUIZ_GEN", quizGenCredits, credits.getQuizGenCredits(),
                    "Admin added " + quizGenCredits + " quiz gen credits. Reason: " + reason);
        }

        credits.setUpdatedAt(LocalDateTime.now());
        userCreditRepo.save(credits);

        // Log admin activity
        String description = String.format("Thêm credits cho user %s: chat=%d, quizGen=%d. Lý do: %s",
                targetUser.getEmail(), chatCredits, quizGenCredits, reason);
        activityLogService.logAction(adminUser, "ADD_CREDITS", "USER", userId, description);

        log.info("Admin {} added credits to user {}: chat={}, quizGen={}", 
                adminUser.getEmail(), targetUser.getEmail(), chatCredits, quizGenCredits);
    }

    /**
     * Remove credits from a user (admin operation)
     */
    @Transactional
    public void removeCredits(Long userId, int chatCredits, int quizGenCredits, 
                             String reason, User adminUser) {
        if (chatCredits <= 0 && quizGenCredits <= 0) {
            throw new BadRequestException("Phải trừ ít nhất 1 loại credit.");
        }

        User targetUser = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy user."));

        // Try to find existing credits, or create new if not exists (for legacy users)
        UserCredit credits = userCreditRepo.findByUserIdWithLock(userId)
                .orElseGet(() -> {
                    log.info("Creating new UserCredit record for user {} (legacy user without credits)", userId);
                    UserCredit newCredits = new UserCredit();
                    newCredits.setUser(targetUser);
                    newCredits.setChatCredits(0);
                    newCredits.setQuizGenCredits(0);
                    newCredits.setUpdatedAt(LocalDateTime.now());
                    return userCreditRepo.save(newCredits);
                });

        // Validate and remove credits
        if (chatCredits > 0) {
            if (credits.getChatCredits() < chatCredits) {
                throw new BadRequestException("Không thể trừ " + chatCredits + " chat credits. Số dư hiện tại: " + credits.getChatCredits());
            }
            int oldBalance = credits.getChatCredits();
            credits.setChatCredits(oldBalance - chatCredits);
            logTransaction(userId, "ADMIN_REMOVE", "CHAT", -chatCredits, credits.getChatCredits(),
                    "Admin removed " + chatCredits + " chat credits. Reason: " + reason);
        }

        if (quizGenCredits > 0) {
            if (credits.getQuizGenCredits() < quizGenCredits) {
                throw new BadRequestException("Không thể trừ " + quizGenCredits + " quiz gen credits. Số dư hiện tại: " + credits.getQuizGenCredits());
            }
            int oldBalance = credits.getQuizGenCredits();
            credits.setQuizGenCredits(oldBalance - quizGenCredits);
            logTransaction(userId, "ADMIN_REMOVE", "QUIZ_GEN", -quizGenCredits, credits.getQuizGenCredits(),
                    "Admin removed " + quizGenCredits + " quiz gen credits. Reason: " + reason);
        }

        credits.setUpdatedAt(LocalDateTime.now());
        userCreditRepo.save(credits);

        // Log admin activity
        String description = String.format("Trừ credits từ user %s: chat=%d, quizGen=%d. Lý do: %s",
                targetUser.getEmail(), chatCredits, quizGenCredits, reason);
        activityLogService.logAction(adminUser, "REMOVE_CREDITS", "USER", userId, description);

        log.info("Admin {} removed credits from user {}: chat={}, quizGen={}", 
                adminUser.getEmail(), targetUser.getEmail(), chatCredits, quizGenCredits);
    }

    /**
     * Get credit analytics for a date range
     * 
     * Note: Credit usage is tracked via two patterns:
     * 1. Legacy: type="USAGE" (direct deduction)
     * 2. New reserve/confirm pattern: type="CONFIRM" (successful AI operations)
     * 
     * We count CONFIRM transactions as actual usage since they represent
     * successfully completed AI operations (chat/quiz generation).
     */
    @Transactional(readOnly = true)
    public CreditAnalyticsResponse getCreditAnalytics(LocalDate from, LocalDate to) {
        LocalDateTime startDateTime = from.atStartOfDay();
        LocalDateTime endDateTime = to.atTime(LocalTime.MAX);

        CreditAnalyticsResponse response = new CreditAnalyticsResponse();

        // Get totals - count both USAGE (legacy) and CONFIRM (new pattern) as actual usage
        // CONFIRM transactions represent successfully completed AI operations
        long chatUsageLegacy = transactionRepo.sumAmountByTypeAndCreditType("USAGE", "CHAT", startDateTime, endDateTime);
        long chatUsageNew = transactionRepo.countByTypeAndCreditTypeAndDateRange("CONFIRM", "CHAT", startDateTime, endDateTime);
        response.setTotalChatUsed(chatUsageLegacy + chatUsageNew);
        
        long quizUsageLegacy = transactionRepo.sumAmountByTypeAndCreditType("USAGE", "QUIZ_GEN", startDateTime, endDateTime);
        long quizUsageNew = transactionRepo.countByTypeAndCreditTypeAndDateRange("CONFIRM", "QUIZ_GEN", startDateTime, endDateTime);
        response.setTotalQuizGenUsed(quizUsageLegacy + quizUsageNew);
        
        response.setTotalChatPurchased(transactionRepo.sumAmountByTypeAndCreditType("PURCHASE", "CHAT", startDateTime, endDateTime));
        response.setTotalQuizGenPurchased(transactionRepo.sumAmountByTypeAndCreditType("PURCHASE", "QUIZ_GEN", startDateTime, endDateTime));
        response.setTotalChatRefunded(transactionRepo.sumAmountByTypeAndCreditType("REFUND", "CHAT", startDateTime, endDateTime));
        response.setTotalQuizGenRefunded(transactionRepo.sumAmountByTypeAndCreditType("REFUND", "QUIZ_GEN", startDateTime, endDateTime));

        // Get daily usage - includes both USAGE and CONFIRM transactions
        List<Object[]> dailyData = transactionRepo.getDailyUsageIncludingConfirm(startDateTime, endDateTime);
        
        // Convert to map for easy lookup
        // Handle both java.sql.Date and java.time.LocalDate (depends on Hibernate/driver version)
        Map<LocalDate, long[]> dailyMap = dailyData.stream()
                .collect(Collectors.toMap(
                        row -> {
                            Object dateObj = row[0];
                            if (dateObj instanceof LocalDate) {
                                return (LocalDate) dateObj;
                            } else if (dateObj instanceof java.sql.Date) {
                                return ((java.sql.Date) dateObj).toLocalDate();
                            } else {
                                // Fallback for other date types
                                return LocalDate.parse(dateObj.toString());
                            }
                        },
                        row -> new long[]{
                                row[1] != null ? ((Number) row[1]).longValue() : 0L,
                                row[2] != null ? ((Number) row[2]).longValue() : 0L
                        }
                ));

        // Fill in all dates in range
        List<CreditAnalyticsResponse.DailyUsage> usageByDay = new ArrayList<>();
        LocalDate current = from;
        while (!current.isAfter(to)) {
            long[] usage = dailyMap.getOrDefault(current, new long[]{0L, 0L});
            usageByDay.add(new CreditAnalyticsResponse.DailyUsage(current, Math.abs(usage[0]), Math.abs(usage[1])));
            current = current.plusDays(1);
        }
        response.setUsageByDay(usageByDay);

        return response;
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
}
