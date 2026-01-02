package com.htai.exe201phapluatso.auth.repo;

import com.htai.exe201phapluatso.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepo extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    
    // Admin dashboard queries
    long countByActive(boolean active);
    long countByCreatedAtAfter(LocalDateTime date);
    long countByCreatedAtBefore(LocalDateTime date);
    List<User> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    // ==================== OPTIMIZED ADMIN QUERIES ====================
    
    /**
     * Get all user stats in a single query (total, active, banned, new users)
     * Returns: [totalUsers, activeUsers, bannedUsers, newUsersLast30Days]
     */
    @Query(value = """
        SELECT 
            COUNT(*) as totalUsers,
            SUM(CASE WHEN is_active = 1 THEN 1 ELSE 0 END) as activeUsers,
            SUM(CASE WHEN is_active = 0 THEN 1 ELSE 0 END) as bannedUsers,
            SUM(CASE WHEN created_at >= :thirtyDaysAgo THEN 1 ELSE 0 END) as newUsersLast30Days
        FROM users
        """, nativeQuery = true)
    Object[] getUserStatsAggregated(@Param("thirtyDaysAgo") LocalDateTime thirtyDaysAgo);
    
    /**
     * Get user growth data grouped by date (for chart)
     * Returns list of [date, count]
     */
    @Query(value = """
        SELECT CAST(created_at AS DATE) as date, COUNT(*) as count
        FROM users
        WHERE created_at BETWEEN :startDate AND :endDate
        GROUP BY CAST(created_at AS DATE)
        ORDER BY date
        """, nativeQuery = true)
    List<Object[]> getUserGrowthByDateRange(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );
}
