package com.htai.exe201phapluatso.auth.repo;

import com.htai.exe201phapluatso.auth.entity.UserCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

/**
 * Repository for UserCredit entity
 * Uses pessimistic locking to prevent race conditions when deducting credits
 */
public interface UserCreditRepo extends JpaRepository<UserCredit, Long> {

    /**
     * Find user credits by user ID
     * @param userId User ID
     * @return Optional of UserCredit
     */
    @Query("SELECT uc FROM UserCredit uc WHERE uc.user.id = :userId")
    Optional<UserCredit> findByUserId(@Param("userId") Long userId);

    /**
     * Find user credits with pessimistic write lock
     * This prevents concurrent modifications when deducting credits
     * 
     * @param userId User ID
     * @return Optional of UserCredit with lock
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT uc FROM UserCredit uc WHERE uc.user.id = :userId")
    Optional<UserCredit> findByUserIdWithLock(@Param("userId") Long userId);

    /**
     * Check if user exists in user_credits table
     * @param userId User ID
     * @return true if exists
     */
    @Query("SELECT CASE WHEN COUNT(uc) > 0 THEN true ELSE false END FROM UserCredit uc WHERE uc.user.id = :userId")
    boolean existsByUserId(@Param("userId") Long userId);
    
    // ==================== BATCH QUERIES FOR ADMIN ====================
    
    /**
     * Find credits for multiple users in a single query (avoid N+1)
     */
    @Query("SELECT uc FROM UserCredit uc WHERE uc.user.id IN :userIds")
    List<UserCredit> findByUserIdIn(@Param("userIds") List<Long> userIds);
}
