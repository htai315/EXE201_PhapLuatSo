package com.htai.exe201phapluatso.credit.scheduler;

import com.htai.exe201phapluatso.auth.entity.UserCredit;
import com.htai.exe201phapluatso.auth.repo.UserCreditRepo;
import com.htai.exe201phapluatso.credit.entity.CreditReservation;
import com.htai.exe201phapluatso.credit.repo.CreditReservationRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler to cleanup expired credit reservations
 * Automatically refunds credits for expired pending reservations
 */
@Component
@ConditionalOnProperty(name = "credit.reservation.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class CreditReservationCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(CreditReservationCleanupScheduler.class);

    private final CreditReservationRepo reservationRepo;
    private final UserCreditRepo userCreditRepo;

    public CreditReservationCleanupScheduler(
            CreditReservationRepo reservationRepo,
            UserCreditRepo userCreditRepo
    ) {
        this.reservationRepo = reservationRepo;
        this.userCreditRepo = userCreditRepo;
    }

    /**
     * Cleanup expired reservations every minute
     * Refunds credits and marks reservations as EXPIRED
     */
    @Scheduled(fixedRateString = "${credit.reservation.cleanup.interval-ms:60000}")
    @Transactional
    public void cleanupExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<CreditReservation> expiredReservations = reservationRepo.findExpiredPendingReservations(now);

        if (expiredReservations.isEmpty()) {
            return;
        }

        log.info("Found {} expired reservations to cleanup", expiredReservations.size());

        int refundedCount = 0;
        for (CreditReservation reservation : expiredReservations) {
            try {
                refundExpiredReservation(reservation);
                refundedCount++;
            } catch (Exception e) {
                log.error("Failed to refund expired reservation {}: {}", reservation.getId(), e.getMessage());
            }
        }

        log.info("Cleaned up {} expired reservations, refunded {} credits", 
                expiredReservations.size(), refundedCount);
    }

    private void refundExpiredReservation(CreditReservation reservation) {
        Long userId = reservation.getUser().getId();
        
        // Restore credit
        UserCredit credits = userCreditRepo.findByUserId(userId).orElse(null);
        if (credits != null) {
            if ("CHAT".equals(reservation.getCreditType())) {
                credits.setChatCredits(credits.getChatCredits() + reservation.getAmount());
            } else if ("QUIZ_GEN".equals(reservation.getCreditType())) {
                credits.setQuizGenCredits(credits.getQuizGenCredits() + reservation.getAmount());
            }
            credits.setUpdatedAt(LocalDateTime.now());
            userCreditRepo.save(credits);
        }

        // Mark as expired
        reservation.setStatus(CreditReservation.STATUS_EXPIRED);
        reservation.setRefundedAt(LocalDateTime.now());
        reservationRepo.save(reservation);

        log.debug("Refunded expired reservation {} for user {}: {} {} credit(s)", 
                reservation.getId(), userId, reservation.getAmount(), reservation.getCreditType());
    }
}
