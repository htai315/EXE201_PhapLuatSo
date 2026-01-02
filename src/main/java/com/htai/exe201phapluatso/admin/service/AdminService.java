package com.htai.exe201phapluatso.admin.service;

import com.htai.exe201phapluatso.admin.dto.*;
import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.entity.UserCredit;
import com.htai.exe201phapluatso.auth.repo.*;
import com.htai.exe201phapluatso.common.exception.BadRequestException;
import com.htai.exe201phapluatso.common.exception.NotFoundException;
import com.htai.exe201phapluatso.legal.repo.ChatMessageRepo;
import com.htai.exe201phapluatso.legal.repo.ChatSessionRepo;
import com.htai.exe201phapluatso.legal.repo.LegalArticleRepo;
import com.htai.exe201phapluatso.legal.repo.LegalDocumentRepo;
import com.htai.exe201phapluatso.payment.entity.Payment;
import com.htai.exe201phapluatso.payment.repo.PaymentRepo;
import com.htai.exe201phapluatso.quiz.repo.QuizAttemptRepo;
import com.htai.exe201phapluatso.quiz.repo.QuizSetRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for admin dashboard operations - OPTIMIZED VERSION
 * Uses batch queries and aggregations to avoid N+1 problems
 */
@Service
public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    private final UserRepo userRepo;
    private final PaymentRepo paymentRepo;
    private final QuizSetRepo quizSetRepo;
    private final QuizAttemptRepo quizAttemptRepo;
    private final ChatSessionRepo chatSessionRepo;
    private final ChatMessageRepo chatMessageRepo;
    private final UserCreditRepo userCreditRepo;
    private final LegalDocumentRepo legalDocumentRepo;
    private final LegalArticleRepo legalArticleRepo;
    private final AdminActivityLogService adminActivityLogService;

    public AdminService(
            UserRepo userRepo,
            PaymentRepo paymentRepo,
            QuizSetRepo quizSetRepo,
            QuizAttemptRepo quizAttemptRepo,
            ChatSessionRepo chatSessionRepo,
            ChatMessageRepo chatMessageRepo,
            UserCreditRepo userCreditRepo,
            CreditTransactionRepo creditTransactionRepo,
            LegalDocumentRepo legalDocumentRepo,
            LegalArticleRepo legalArticleRepo,
            AdminActivityLogService adminActivityLogService
    ) {
        this.userRepo = userRepo;
        this.paymentRepo = paymentRepo;
        this.quizSetRepo = quizSetRepo;
        this.quizAttemptRepo = quizAttemptRepo;
        this.chatSessionRepo = chatSessionRepo;
        this.chatMessageRepo = chatMessageRepo;
        this.userCreditRepo = userCreditRepo;
        this.legalDocumentRepo = legalDocumentRepo;
        this.legalArticleRepo = legalArticleRepo;
        this.adminActivityLogService = adminActivityLogService;
    }


    /**
     * Get dashboard statistics - OPTIMIZED
     * Reduced from 10+ queries to ~5 queries using aggregations
     */
    public AdminStatsResponse getDashboardStats() {
        AdminStatsResponse stats = new AdminStatsResponse();
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        // Query 1: User statistics (single aggregated query)
        // Native query returns Object[] where element[0] is the row containing column values
        Object rawUserStats = userRepo.getUserStatsAggregated(thirtyDaysAgo);
        if (rawUserStats != null && rawUserStats instanceof Object[]) {
            Object[] wrapper = (Object[]) rawUserStats;
            if (wrapper.length > 0 && wrapper[0] instanceof Object[]) {
                Object[] userStats = (Object[]) wrapper[0];
                if (userStats.length >= 4) {
                    stats.setTotalUsers(toLong(userStats[0]));
                    stats.setActiveUsers(toLong(userStats[1]));
                    stats.setBannedUsers(toLong(userStats[2]));
                    stats.setNewUsersLast30Days(toLong(userStats[3]));
                }
            }
        }

        // Query 2: Payment statistics (single aggregated query)
        Object rawPaymentStats = paymentRepo.getPaymentStatsAggregated();
        if (rawPaymentStats != null && rawPaymentStats instanceof Object[]) {
            Object[] wrapper = (Object[]) rawPaymentStats;
            if (wrapper.length > 0 && wrapper[0] instanceof Object[]) {
                Object[] paymentStats = (Object[]) wrapper[0];
                if (paymentStats.length >= 5) {
                    stats.setTotalSuccessfulPayments(toLong(paymentStats[1]));
                    stats.setTotalRevenue(toLong(paymentStats[4]));
                }
            }
        }

        // Query 3: Revenue with time periods
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
        Object rawRevenueStats = paymentRepo.getRevenueStatsAggregated(today, weekStart, thirtyDaysAgo);
        if (rawRevenueStats != null && rawRevenueStats instanceof Object[]) {
            Object[] wrapper = (Object[]) rawRevenueStats;
            if (wrapper.length > 0 && wrapper[0] instanceof Object[]) {
                Object[] revenueStats = (Object[]) wrapper[0];
                if (revenueStats.length >= 4) {
                    stats.setRevenueLast30Days(toLong(revenueStats[3]));
                }
            }
        }

        // Query 4-7: Simple counts (acceptable)
        stats.setTotalQuizSets(quizSetRepo.count());
        stats.setTotalQuizAttempts(quizAttemptRepo.count());
        stats.setTotalChatSessions(chatSessionRepo.count());
        stats.setTotalChatMessages(chatMessageRepo.countByRole("USER"));
        stats.setTotalLegalDocuments(legalDocumentRepo.count());
        stats.setTotalLegalArticles(legalArticleRepo.count());

        return stats;
    }

    /**
     * Get revenue chart data - OPTIMIZED
     * Groups by date at database level, avoids loading all payments into memory
     */
    public List<RevenueByDate> getRevenueChart(LocalDate from, LocalDate to) {
        List<Object[]> results = paymentRepo.getRevenueByDateRange(
                from.atStartOfDay(),
                to.atTime(23, 59, 59)
        );

        return results.stream()
                .map(row -> new RevenueByDate(
                        toLocalDate(row[0]),
                        toLong(row[1]),
                        ((Number) row[2]).intValue()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Get user growth chart data - OPTIMIZED
     * Groups by date at database level
     */
    public List<UserGrowth> getUserGrowthChart(LocalDate from, LocalDate to) {
        List<Object[]> results = userRepo.getUserGrowthByDateRange(
                from.atStartOfDay(),
                to.atTime(23, 59, 59)
        );

        Map<LocalDate, Long> newUsersByDate = results.stream()
                .collect(Collectors.toMap(
                        row -> toLocalDate(row[0]),
                        row -> toLong(row[1])
                ));

        List<UserGrowth> result = new ArrayList<>();
        long cumulativeTotal = userRepo.countByCreatedAtBefore(from.atStartOfDay());

        LocalDate currentDate = from;
        while (!currentDate.isAfter(to)) {
            long newUsers = newUsersByDate.getOrDefault(currentDate, 0L);
            cumulativeTotal += newUsers;
            result.add(new UserGrowth(currentDate, newUsers, cumulativeTotal));
            currentDate = currentDate.plusDays(1);
        }

        return result;
    }


    /**
     * Get all users with pagination - OPTIMIZED
     * Reduced from N+1 (4 queries per user) to 4 batch queries total
     */
    public Page<AdminUserListResponse> getAllUsers(Pageable pageable, String search) {
        Specification<User> spec = (root, query, cb) -> {
            if (search == null || search.trim().isEmpty()) {
                return cb.conjunction();
            }
            String searchPattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("email")), searchPattern),
                    cb.like(cb.lower(root.get("fullName")), searchPattern)
            );
        };

        Page<User> users = userRepo.findAll(spec, pageable);

        if (users.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<Long> userIds = users.getContent().stream()
                .map(User::getId)
                .collect(Collectors.toList());

        // Batch queries to avoid N+1
        Map<Long, UserCredit> creditsMap = userCreditRepo.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(uc -> uc.getUser().getId(), Function.identity()));

        Map<Long, Long> paymentCountsMap = paymentRepo.countByUserIdsAndStatus(userIds).stream()
                .collect(Collectors.toMap(row -> toLong(row[0]), row -> toLong(row[1])));

        Map<Long, Long> quizCountsMap = quizSetRepo.countByUserIds(userIds).stream()
                .collect(Collectors.toMap(row -> toLong(row[0]), row -> toLong(row[1])));

        Map<Long, Long> chatCountsMap = chatSessionRepo.countByUserIds(userIds).stream()
                .collect(Collectors.toMap(row -> toLong(row[0]), row -> toLong(row[1])));

        List<AdminUserListResponse> responses = users.getContent().stream()
                .map(user -> mapToUserListResponse(user, creditsMap, paymentCountsMap, quizCountsMap, chatCountsMap))
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, users.getTotalElements());
    }

    public AdminUserDetailResponse getUserDetail(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
        return mapToUserDetailResponse(user);
    }

    @Transactional
    public void banUser(Long userId, String reason, User adminUser) {
        logger.info("Attempting to ban user {} by admin {}", userId, adminUser != null ? adminUser.getEmail() : "NULL");
        
        if (adminUser == null) {
            throw new BadRequestException("Admin user is null - authentication issue");
        }
        
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        if (!user.isActive()) {
            throw new BadRequestException("User is already banned");
        }
        if (user.getId().equals(adminUser.getId())) {
            throw new BadRequestException("Cannot ban yourself");
        }

        user.setActive(false);
        user.setBanReason(reason);
        user.setBannedAt(LocalDateTime.now());
        user.setBannedBy(adminUser);
        userRepo.save(user);

        logger.info("User {} banned by admin {} - Reason: {}", user.getEmail(), adminUser.getEmail(), reason);
        logAdminActivity(adminUser, "BAN_USER", "USER", userId, 
                "Banned user: " + user.getEmail() + " - Reason: " + reason);
    }

    @Transactional
    public void unbanUser(Long userId, User adminUser) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        if (user.isActive()) {
            throw new BadRequestException("User is not banned");
        }

        user.setActive(true);
        user.setBanReason(null);
        user.setBannedAt(null);
        user.setBannedBy(null);
        userRepo.save(user);

        logger.info("User {} unbanned by admin {}", user.getEmail(), adminUser.getEmail());
        logAdminActivity(adminUser, "UNBAN_USER", "USER", userId, "Unbanned user: " + user.getEmail());
    }

    @Transactional
    public void deleteUser(Long userId, User adminUser) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        if (user.getId().equals(adminUser.getId())) {
            throw new BadRequestException("Cannot delete yourself");
        }

        user.setEnabled(false);
        user.setActive(false);
        userRepo.save(user);

        logger.info("User {} deleted by admin {}", user.getEmail(), adminUser.getEmail());
        logAdminActivity(adminUser, "DELETE_USER", "USER", userId, "Deleted user: " + user.getEmail());
    }


    public Page<AdminPaymentListResponse> getAllPayments(Pageable pageable) {
        Page<Payment> payments = paymentRepo.findAllWithUser(pageable);

        List<AdminPaymentListResponse> responses = payments.getContent().stream()
                .map(this::mapToPaymentListResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, payments.getTotalElements());
    }

    /**
     * Get payment statistics - OPTIMIZED
     * Reduced from 8+ queries to 2 queries
     */
    public AdminPaymentStatsResponse getPaymentStats() {
        AdminPaymentStatsResponse stats = new AdminPaymentStatsResponse();

        Object rawPaymentStats = paymentRepo.getPaymentStatsAggregated();
        if (rawPaymentStats != null && rawPaymentStats instanceof Object[]) {
            Object[] wrapper = (Object[]) rawPaymentStats;
            if (wrapper.length > 0 && wrapper[0] instanceof Object[]) {
                Object[] paymentStats = (Object[]) wrapper[0];
                if (paymentStats.length >= 5) {
                    stats.setTotalPayments(toLong(paymentStats[0]));
                    stats.setSuccessfulPayments(toLong(paymentStats[1]));
                    stats.setFailedPayments(toLong(paymentStats[2]));
                    stats.setPendingPayments(toLong(paymentStats[3]));
                    stats.setTotalRevenue(toLong(paymentStats[4]));
                }
            }
        }

        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
        LocalDateTime monthStart = LocalDateTime.now().minusDays(30);

        Object rawRevenueStats = paymentRepo.getRevenueStatsAggregated(today, weekStart, monthStart);
        if (rawRevenueStats != null && rawRevenueStats instanceof Object[]) {
            Object[] wrapper = (Object[]) rawRevenueStats;
            if (wrapper.length > 0 && wrapper[0] instanceof Object[]) {
                Object[] revenueStats = (Object[]) wrapper[0];
                stats.setRevenueToday(toLong(revenueStats[1]));
                stats.setRevenueThisWeek(toLong(revenueStats[2]));
                stats.setRevenueThisMonth(toLong(revenueStats[3]));
            }
        }

        Long successCount = stats.getSuccessfulPayments();
        Long totalPayments = stats.getTotalPayments();
        Long totalRevenue = stats.getTotalRevenue();

        if (successCount != null && successCount > 0 && totalRevenue != null) {
            stats.setAveragePaymentAmount(totalRevenue.doubleValue() / successCount);
        } else {
            stats.setAveragePaymentAmount(0.0);
        }

        if (totalPayments != null && totalPayments > 0 && successCount != null) {
            stats.setSuccessRate((successCount.doubleValue() / totalPayments) * 100);
        } else {
            stats.setSuccessRate(0.0);
        }

        return stats;
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private AdminUserListResponse mapToUserListResponse(
            User user,
            Map<Long, UserCredit> creditsMap,
            Map<Long, Long> paymentCountsMap,
            Map<Long, Long> quizCountsMap,
            Map<Long, Long> chatCountsMap
    ) {
        AdminUserListResponse response = new AdminUserListResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setProvider(user.getProvider());
        response.setEmailVerified(user.isEmailVerified());
        response.setEnabled(user.isEnabled());
        response.setActive(user.isActive());
        response.setBanReason(user.getBanReason());
        response.setBannedAt(user.getBannedAt());
        response.setCreatedAt(user.getCreatedAt());

        UserCredit credit = creditsMap.get(user.getId());
        if (credit != null) {
            response.setChatCredits(credit.getChatCredits());
            response.setQuizGenCredits(credit.getQuizGenCredits());
        }

        response.setTotalPayments(paymentCountsMap.getOrDefault(user.getId(), 0L));
        response.setTotalQuizSets(quizCountsMap.getOrDefault(user.getId(), 0L));
        response.setTotalChatSessions(chatCountsMap.getOrDefault(user.getId(), 0L));

        return response;
    }


    private AdminUserDetailResponse mapToUserDetailResponse(User user) {
        AdminUserDetailResponse response = new AdminUserDetailResponse();

        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setProvider(user.getProvider());
        response.setProviderId(user.getProviderId());
        response.setEmailVerified(user.isEmailVerified());
        response.setEnabled(user.isEnabled());
        response.setActive(user.isActive());
        response.setBanReason(user.getBanReason());
        response.setBannedAt(user.getBannedAt());

        if (user.getBannedBy() != null) {
            response.setBannedByUserId(user.getBannedBy().getId());
            response.setBannedByUserName(user.getBannedBy().getFullName());
        }

        response.setCreatedAt(user.getCreatedAt());

        userCreditRepo.findByUserId(user.getId()).ifPresent(credit -> {
            response.setChatCredits(credit.getChatCredits());
            response.setQuizGenCredits(credit.getQuizGenCredits());
            response.setCreditsExpiresAt(credit.getExpiresAt());
        });

        response.setTotalPayments(paymentRepo.countByUserIdAndStatus(user.getId(), "SUCCESS"));
        response.setTotalRevenue(paymentRepo.sumAmountByUserIdAndStatus(user.getId(), "SUCCESS"));
        response.setTotalQuizSets(quizSetRepo.countByCreatedById(user.getId()));
        response.setTotalQuizAttempts(quizAttemptRepo.countByUserId(user.getId()));
        response.setTotalChatSessions(chatSessionRepo.countByUserId(user.getId()));
        response.setTotalChatMessages(chatMessageRepo.countBySessionUserIdAndRole(user.getId(), "USER"));

        response.setRecentPayments(new ArrayList<>());
        response.setRecentQuizzes(new ArrayList<>());
        response.setRecentChats(new ArrayList<>());

        return response;
    }

    private AdminPaymentListResponse mapToPaymentListResponse(Payment payment) {
        AdminPaymentListResponse response = new AdminPaymentListResponse();
        response.setId(payment.getId());
        response.setOrderId(payment.getOrderId());

        if (payment.getUser() != null) {
            response.setUserId(payment.getUser().getId());
            response.setUserEmail(payment.getUser().getEmail());
            response.setUserName(payment.getUser().getFullName());
        } else {
            response.setUserId(null);
            response.setUserEmail("Unknown");
            response.setUserName("Unknown");
        }

        if (payment.getPlan() != null) {
            response.setPlanCode(payment.getPlan().getCode());
        } else {
            response.setPlanCode("Unknown");
        }

        response.setAmount(payment.getAmount() != null ? payment.getAmount().longValue() : 0L);
        response.setStatus(payment.getStatus());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setTransactionNo(payment.getTransactionNo());
        response.setCreatedAt(payment.getCreatedAt());
        response.setPaidAt(payment.getPaidAt());
        return response;
    }

    private void logAdminActivity(User adminUser, String actionType, String targetType,
                                   Long targetId, String description) {
        adminActivityLogService.logAction(adminUser, actionType, targetType, targetId, description);
    }

    private Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long) return (Long) value;
        if (value instanceof BigDecimal) return ((BigDecimal) value).longValue();
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate) return (LocalDate) value;
        if (value instanceof Date) return ((Date) value).toLocalDate();
        if (value instanceof java.util.Date) {
            return ((java.util.Date) value).toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
        }
        return null;
    }
}
