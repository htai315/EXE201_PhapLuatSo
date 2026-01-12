package com.htai.exe201phapluatso.payment.repo;

import com.htai.exe201phapluatso.payment.entity.Payment;
import com.htai.exe201phapluatso.auth.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepo extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {
    Optional<Payment> findByVnpTxnRef(String vnpTxnRef);
    
    Optional<Payment> findByOrderCode(Long orderCode);
    
    // Fetch with plan to avoid LazyInitializationException
    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.plan WHERE p.orderCode = :orderCode")
    Optional<Payment> findByOrderCodeWithPlan(@Param("orderCode") Long orderCode);
    
    // Pessimistic lock for webhook processing (with plan eagerly loaded)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.plan WHERE p.orderCode = :orderCode")
    Optional<Payment> findByOrderCodeWithLock(@Param("orderCode") Long orderCode);
    
    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.plan WHERE p.user = :user ORDER BY p.createdAt DESC")
    List<Payment> findByUserOrderByCreatedAtDesc(@Param("user") User user);
    
    // Find pending payments for duplicate check (with plan eagerly loaded)
    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.plan WHERE p.user = :user AND p.status = :status ORDER BY p.createdAt DESC")
    List<Payment> findByUserAndStatusOrderByCreatedAtDesc(@Param("user") User user, @Param("status") String status);
    
    // Pessimistic lock on user for creating payment (prevent race condition)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.plan WHERE p.user.id = :userId AND p.status = 'PENDING' ORDER BY p.createdAt DESC")
    List<Payment> findPendingPaymentsByUserIdWithLock(@Param("userId") Long userId);
    
    // Find stale pending payments for cleanup
    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.createdAt < :date")
    List<Payment> findByStatusAndCreatedAtBefore(@Param("status") String status, @Param("date") LocalDateTime date);
    
    @Query("SELECT p FROM Payment p WHERE p.user = :user AND p.status = 'SUCCESS' ORDER BY p.createdAt DESC")
    List<Payment> findSuccessfulPaymentsByUser(@Param("user") User user);
    
    // Admin dashboard queries
    long countByStatus(String status);
    
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status")
    Long sumAmountByStatus(@Param("status") String status);
    
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status AND p.createdAt >= :date")
    Long sumAmountByStatusAndCreatedAtAfter(@Param("status") String status, @Param("date") LocalDateTime date);
    
    List<Payment> findByStatusAndCreatedAtBetween(String status, LocalDateTime start, LocalDateTime end);
    
    long countByUserIdAndStatus(Long userId, String status);
    
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.user.id = :userId AND p.status = :status")
    Long sumAmountByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);
    
    // Admin payments list with user and plan eagerly loaded
    @Query(value = "SELECT p FROM Payment p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.plan",
           countQuery = "SELECT COUNT(p) FROM Payment p")
    org.springframework.data.domain.Page<Payment> findAllWithUser(org.springframework.data.domain.Pageable pageable);
    
    // ==================== OPTIMIZED ADMIN QUERIES ====================
    
    /**
     * Get all payment stats in a single query
     * Returns: [totalPayments, successCount, failedCount, pendingCount, totalRevenue]
     */
    @Query(value = """
        SELECT 
            COUNT(*) as totalPayments,
            SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) as successCount,
            SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failedCount,
            SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pendingCount,
            COALESCE(SUM(CASE WHEN status = 'SUCCESS' THEN amount ELSE 0 END), 0) as totalRevenue
        FROM payments
        """, nativeQuery = true)
    Object[] getPaymentStatsAggregated();
    
    /**
     * Get revenue stats with time periods in a single query
     * Returns: [totalRevenue, revenueToday, revenueThisWeek, revenueThisMonth]
     */
    @Query(value = """
        SELECT 
            COALESCE(SUM(CASE WHEN status = 'SUCCESS' THEN amount ELSE 0 END), 0) as totalRevenue,
            COALESCE(SUM(CASE WHEN status = 'SUCCESS' AND created_at >= :today THEN amount ELSE 0 END), 0) as revenueToday,
            COALESCE(SUM(CASE WHEN status = 'SUCCESS' AND created_at >= :weekStart THEN amount ELSE 0 END), 0) as revenueThisWeek,
            COALESCE(SUM(CASE WHEN status = 'SUCCESS' AND created_at >= :monthStart THEN amount ELSE 0 END), 0) as revenueThisMonth
        FROM payments
        """, nativeQuery = true)
    Object[] getRevenueStatsAggregated(
        @Param("today") LocalDateTime today,
        @Param("weekStart") LocalDateTime weekStart,
        @Param("monthStart") LocalDateTime monthStart
    );
    
    /**
     * Get revenue grouped by date for chart (avoid loading all payments into memory)
     * Returns list of [date, totalAmount, count]
     */
    @Query(value = """
        SELECT created_at::DATE as date, 
               COALESCE(SUM(amount), 0) as totalAmount, 
               COUNT(*) as count
        FROM payments
        WHERE status = 'SUCCESS' AND created_at BETWEEN :startDate AND :endDate
        GROUP BY created_at::DATE
        ORDER BY date
        """, nativeQuery = true)
    List<Object[]> getRevenueByDateRange(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Batch count payments by user IDs (avoid N+1)
     * Returns list of [userId, count]
     */
    @Query(value = """
        SELECT user_id, COUNT(*) as count
        FROM payments
        WHERE user_id IN :userIds AND status = 'SUCCESS'
        GROUP BY user_id
        """, nativeQuery = true)
    List<Object[]> countByUserIdsAndStatus(@Param("userIds") List<Long> userIds);
}
