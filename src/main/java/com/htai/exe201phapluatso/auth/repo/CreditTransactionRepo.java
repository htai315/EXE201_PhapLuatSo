package com.htai.exe201phapluatso.auth.repo;

import com.htai.exe201phapluatso.auth.entity.CreditTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for CreditTransaction entity
 * Provides efficient queries for transaction history and analytics
 */
public interface CreditTransactionRepo extends JpaRepository<CreditTransaction, Long> {

    /**
     * Find all transactions for a user, ordered by date descending
     */
    @Query("SELECT ct FROM CreditTransaction ct WHERE ct.user.id = :userId ORDER BY ct.createdAt DESC")
    Page<CreditTransaction> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find recent transactions for a user (last N transactions)
     */
    @Query("SELECT ct FROM CreditTransaction ct WHERE ct.user.id = :userId ORDER BY ct.createdAt DESC")
    List<CreditTransaction> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Count total usage transactions for a user
     */
    @Query("SELECT COUNT(ct) FROM CreditTransaction ct WHERE ct.user.id = :userId AND ct.type = 'USAGE'")
    long countUsageByUserId(@Param("userId") Long userId);

    /**
     * Count usage transactions by credit type for a user
     */
    @Query("SELECT COUNT(ct) FROM CreditTransaction ct WHERE ct.user.id = :userId AND ct.type = 'USAGE' AND ct.creditType = :creditType")
    long countUsageByUserIdAndCreditType(@Param("userId") Long userId, @Param("creditType") String creditType);

    /**
     * Find transactions within date range
     */
    @Query("SELECT ct FROM CreditTransaction ct WHERE ct.user.id = :userId AND ct.createdAt BETWEEN :startDate AND :endDate ORDER BY ct.createdAt DESC")
    List<CreditTransaction> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // ==================== ANALYTICS QUERIES ====================

    /**
     * Sum amount by type and credit type within date range
     * Returns absolute value (usage amounts are negative)
     */
    @Query("SELECT COALESCE(SUM(ABS(ct.amount)), 0) FROM CreditTransaction ct " +
           "WHERE ct.type = :type AND ct.creditType = :creditType " +
           "AND ct.createdAt BETWEEN :startDate AND :endDate")
    long sumAmountByTypeAndCreditType(
            @Param("type") String type,
            @Param("creditType") String creditType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Get daily usage statistics (USAGE transactions only)
     * Returns: date, sum of chat usage, sum of quiz_gen usage
     */
    @Query(value = "SELECT DATE(created_at) as date, " +
           "SUM(CASE WHEN credit_type = 'CHAT' THEN ABS(amount) ELSE 0 END) as chat_used, " +
           "SUM(CASE WHEN credit_type = 'QUIZ_GEN' THEN ABS(amount) ELSE 0 END) as quiz_gen_used " +
           "FROM credit_transactions " +
           "WHERE type = 'USAGE' AND created_at BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(created_at) " +
           "ORDER BY date", nativeQuery = true)
    List<Object[]> getDailyUsage(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Count total transactions by type within date range
     */
    @Query("SELECT COUNT(ct) FROM CreditTransaction ct " +
           "WHERE ct.type = :type AND ct.createdAt BETWEEN :startDate AND :endDate")
    long countByTypeAndDateRange(
            @Param("type") String type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Count transactions by type and credit type within date range
     * Used for counting CONFIRM transactions (successful AI operations)
     */
    @Query("SELECT COUNT(ct) FROM CreditTransaction ct " +
           "WHERE ct.type = :type AND ct.creditType = :creditType " +
           "AND ct.createdAt BETWEEN :startDate AND :endDate")
    long countByTypeAndCreditTypeAndDateRange(
            @Param("type") String type,
            @Param("creditType") String creditType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Get daily usage statistics including both USAGE (legacy) and CONFIRM (new pattern)
     * CONFIRM transactions represent successfully completed AI operations
     * Returns: date, sum of chat usage, sum of quiz_gen usage
     */
    @Query(value = "SELECT DATE(created_at) as date, " +
           "SUM(CASE WHEN credit_type = 'CHAT' AND type = 'USAGE' THEN ABS(amount) " +
           "         WHEN credit_type = 'CHAT' AND type = 'CONFIRM' THEN 1 ELSE 0 END) as chat_used, " +
           "SUM(CASE WHEN credit_type = 'QUIZ_GEN' AND type = 'USAGE' THEN ABS(amount) " +
           "         WHEN credit_type = 'QUIZ_GEN' AND type = 'CONFIRM' THEN 1 ELSE 0 END) as quiz_gen_used " +
           "FROM credit_transactions " +
           "WHERE (type = 'USAGE' OR type = 'CONFIRM') AND created_at BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(created_at) " +
           "ORDER BY date", nativeQuery = true)
    List<Object[]> getDailyUsageIncludingConfirm(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
