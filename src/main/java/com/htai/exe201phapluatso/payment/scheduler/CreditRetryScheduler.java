package com.htai.exe201phapluatso.payment.scheduler;

import com.htai.exe201phapluatso.auth.entity.Plan;
import com.htai.exe201phapluatso.credit.service.CreditService;
import com.htai.exe201phapluatso.payment.entity.Payment;
import com.htai.exe201phapluatso.payment.repo.PaymentRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job to retry adding credits for payments that received money but
 * failed to add credits.
 * 
 * This handles the case where:
 * 1. User successfully paid via PayOS (code="00")
 * 2. Payment was marked as PAID
 * 3. But addCredits() failed (DB error, timeout, etc.)
 * 4. Payment was marked as PAID_CREDIT_FAILED
 * 
 * This job retries adding credits for such payments.
 * If max retries exceeded, payment is marked as NEEDS_REVIEW for manual
 * handling.
 */
@Component
public class CreditRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(CreditRetryScheduler.class);

    private final PaymentRepo paymentRepo;
    private final CreditService creditService;

    @Value("${payment.credit-retry.max-attempts:5}")
    private int maxRetryAttempts;

    @Value("${payment.credit-retry.batch-size:10}")
    private int batchSize;

    public CreditRetryScheduler(PaymentRepo paymentRepo, CreditService creditService) {
        this.paymentRepo = paymentRepo;
        this.creditService = creditService;
    }

    /**
     * Retry adding credits for PAID_CREDIT_FAILED payments.
     * Runs every 2 minutes.
     */
    @Scheduled(fixedDelay = 120000) // Every 2 minutes
    @Transactional
    public void retryCreditAddition() {
        log.info("Running credit retry job...");

        List<Payment> failedPayments = paymentRepo.findPaidCreditFailedPayments(maxRetryAttempts);

        if (failedPayments.isEmpty()) {
            log.debug("No PAID_CREDIT_FAILED payments to process");
            return;
        }

        // Process in batches to avoid long transactions
        int toProcess = Math.min(failedPayments.size(), batchSize);
        log.info("Found {} PAID_CREDIT_FAILED payments, processing {}", failedPayments.size(), toProcess);

        int success = 0;
        int failed = 0;
        int needsReview = 0;

        for (int i = 0; i < toProcess; i++) {
            Payment payment = failedPayments.get(i);
            try {
                processRetry(payment);
                success++;
            } catch (Exception e) {
                log.error("Retry failed for orderCode={}: {}", payment.getOrderCode(), e.getMessage());
                payment.incrementCreditRetryCount();

                if (payment.getCreditRetryCount() >= maxRetryAttempts) {
                    // Max retries reached - mark for manual review
                    payment.setStatus(Payment.STATUS_NEEDS_REVIEW);
                    log.error("Max retries ({}) exceeded for orderCode={} - marked as NEEDS_REVIEW",
                            maxRetryAttempts, payment.getOrderCode());
                    needsReview++;
                } else {
                    failed++;
                }
                paymentRepo.save(payment);
            }
        }

        log.info("Credit retry job completed. Success: {}, Failed: {}, NeedsReview: {}",
                success, failed, needsReview);
    }

    private void processRetry(Payment payment) {
        log.info("Retrying credit addition for orderCode={}, attempt={}",
                payment.getOrderCode(), payment.getCreditRetryCount() + 1);

        Plan plan = payment.getPlan();
        if (plan == null) {
            throw new IllegalStateException("Payment has no plan: " + payment.getOrderCode());
        }

        LocalDateTime expiresAt = plan.getDurationMonths() > 0
                ? payment.getPaidAt().plusMonths(plan.getDurationMonths())
                : null;

        // Attempt to add credits
        creditService.addCredits(
                payment.getUser().getId(),
                plan.getChatCredits(),
                plan.getQuizGenCredits(),
                plan.getCode(),
                expiresAt);

        // Success! Mark as CREDITED
        payment.setStatus(Payment.STATUS_CREDITED);
        paymentRepo.save(payment);

        log.info("Credit retry SUCCESS for orderCode={}, credits added: chat={}, quiz={}",
                payment.getOrderCode(), plan.getChatCredits(), plan.getQuizGenCredits());
    }
}
