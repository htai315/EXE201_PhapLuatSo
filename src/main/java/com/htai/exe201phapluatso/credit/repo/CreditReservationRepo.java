package com.htai.exe201phapluatso.credit.repo;

import com.htai.exe201phapluatso.credit.entity.CreditReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for CreditReservation entity
 */
public interface CreditReservationRepo extends JpaRepository<CreditReservation, Long> {

    /**
     * Find all expired pending reservations
     */
    @Query("SELECT cr FROM CreditReservation cr WHERE cr.status = 'PENDING' AND cr.expiresAt < :now")
    List<CreditReservation> findExpiredPendingReservations(@Param("now") LocalDateTime now);

    /**
     * Count pending reservations for a user
     */
    @Query("SELECT COUNT(cr) FROM CreditReservation cr WHERE cr.user.id = :userId AND cr.status = 'PENDING'")
    long countPendingByUserId(@Param("userId") Long userId);

    /**
     * Find pending reservations for a user by credit type
     */
    @Query("SELECT cr FROM CreditReservation cr WHERE cr.user.id = :userId AND cr.creditType = :creditType AND cr.status = 'PENDING'")
    List<CreditReservation> findPendingByUserIdAndCreditType(
            @Param("userId") Long userId, 
            @Param("creditType") String creditType
    );

    /**
     * Bulk update expired reservations to EXPIRED status
     * Returns number of updated records
     */
    @Modifying
    @Query("UPDATE CreditReservation cr SET cr.status = 'EXPIRED', cr.refundedAt = :now WHERE cr.status = 'PENDING' AND cr.expiresAt < :now")
    int markExpiredReservations(@Param("now") LocalDateTime now);
}
