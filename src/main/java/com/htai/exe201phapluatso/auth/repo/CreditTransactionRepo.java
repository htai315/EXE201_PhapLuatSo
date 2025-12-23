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
     * @param userId User ID
     * @param pageable Pagination
     * @return Page of transactions
     */
    @Query("SELECT ct FROM CreditTransaction ct WHERE ct.user.id = :userId ORDER BY ct.createdAt DESC")
    Page<CreditTransaction> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find recent transactions for a user (last N transactions)
     * @param userId User ID
     * @param pageable Pagination with limit
     * @return List of recent transactions
     */
    @Query("SELECT ct FROM CreditTransaction ct WHERE ct.user.id = :userId ORDER BY ct.createdAt DESC")
    List<CreditTransaction> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Count total usage transactions for a user
     * @param userId User ID
     * @return Count of USAGE transactions
     */
    @Query("SELECT COUNT(ct) FROM CreditTransaction ct WHERE ct.user.id = :userId AND ct.type = 'USAGE'")
    long countUsageByUserId(@Param("userId") Long userId);

    /**
     * Count usage transactions by credit type for a user
     * @param userId User ID
     * @param creditType CHAT or QUIZ_GEN
     * @return Count of usage transactions
     */
    @Query("SELECT COUNT(ct) FROM CreditTransaction ct WHERE ct.user.id = :userId AND ct.type = 'USAGE' AND ct.creditType = :creditType")
    long countUsageByUserIdAndCreditType(@Param("userId") Long userId, @Param("creditType") String creditType);

    /**
     * Find transactions within date range
     * @param userId User ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of transactions
     */
    @Query("SELECT ct FROM CreditTransaction ct WHERE ct.user.id = :userId AND ct.createdAt BETWEEN :startDate AND :endDate ORDER BY ct.createdAt DESC")
    List<CreditTransaction> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
