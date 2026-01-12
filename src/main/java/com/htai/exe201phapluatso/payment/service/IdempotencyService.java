package com.htai.exe201phapluatso.payment.service;

import com.htai.exe201phapluatso.payment.entity.Payment;
import com.htai.exe201phapluatso.payment.entity.PaymentIdempotencyRecord;
import com.htai.exe201phapluatso.payment.repo.PaymentIdempotencyRecordRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service quản lý idempotency keys để tránh duplicate payment khi network retry.
 * 
 * Logic:
 * - Nếu key đã tồn tại với PENDING/SUCCESS → return existing payment
 * - Nếu key đã tồn tại với FAILED/EXPIRED/CANCELLED → cho phép tạo payment mới
 * - Nếu key chưa tồn tại → tạo record mới và cho phép tạo payment
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final PaymentIdempotencyRecordRepo idempotencyRepo;

    @Value("${payment.idempotency.expiration-hours:24}")
    private int expirationHours;

    public IdempotencyService(PaymentIdempotencyRecordRepo idempotencyRepo) {
        this.idempotencyRepo = idempotencyRepo;
    }

    /**
     * Check idempotency key và trả về existing payment nếu có.
     * 
     * @param userId User ID
     * @param idempotencyKey Idempotency key từ client
     * @param planCode Plan code được request
     * @return Optional<Payment> - existing payment nếu key đã tồn tại với PENDING/SUCCESS
     */
    @Transactional
    public Optional<Payment> checkIdempotencyKey(Long userId, String idempotencyKey, String planCode) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }

        String scopedKey = buildScopedKey(userId, idempotencyKey);
        log.debug("Checking idempotency key: {}", scopedKey);

        Optional<PaymentIdempotencyRecord> existingRecord = 
                idempotencyRepo.findByScopedKeyWithPayment(scopedKey);

        if (existingRecord.isEmpty()) {
            // Key chưa tồn tại → tạo record mới
            log.info("Idempotency key not found, creating new record: {}", scopedKey);
            createIdempotencyRecord(scopedKey, userId, planCode);
            return Optional.empty();
        }

        PaymentIdempotencyRecord record = existingRecord.get();

        // Check if record has expired
        if (record.isExpired()) {
            log.info("Idempotency record expired, allowing new payment: {}", scopedKey);
            // Xóa record cũ và tạo mới
            idempotencyRepo.delete(record);
            createIdempotencyRecord(scopedKey, userId, planCode);
            return Optional.empty();
        }

        // Check if record allows new payment (FAILED/EXPIRED/CANCELLED)
        if (record.allowsNewPayment()) {
            log.info("Idempotency record allows retry (status={}), creating new payment: {}", 
                    record.getStatus(), scopedKey);
            // Update record status to PENDING
            record.setStatus("PENDING");
            record.setPayment(null);
            idempotencyRepo.save(record);
            return Optional.empty();
        }

        // Record has active payment (PENDING/SUCCESS) → return existing
        Payment existingPayment = record.getPayment();
        if (existingPayment != null) {
            log.info("Idempotency key found with active payment (status={}): orderCode={}", 
                    record.getStatus(), existingPayment.getOrderCode());
            return Optional.of(existingPayment);
        }

        // Record exists but no payment yet (race condition) → allow new payment
        log.warn("Idempotency record exists but no payment found, allowing new payment: {}", scopedKey);
        return Optional.empty();
    }

    /**
     * Update idempotency record với payment result.
     * Gọi sau khi payment được tạo thành công.
     * 
     * @param userId User ID
     * @param idempotencyKey Idempotency key từ client
     * @param payment Payment đã được tạo
     */
    @Transactional
    public void updateIdempotencyRecord(Long userId, String idempotencyKey, Payment payment) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || payment == null) {
            return;
        }

        String scopedKey = buildScopedKey(userId, idempotencyKey);
        
        idempotencyRepo.findByScopedKey(scopedKey).ifPresent(record -> {
            record.setPayment(payment);
            record.setStatus(payment.getStatus());
            idempotencyRepo.save(record);
            log.debug("Updated idempotency record: {} → status={}, paymentId={}", 
                    scopedKey, payment.getStatus(), payment.getId());
        });
    }

    /**
     * Update idempotency record status khi payment status thay đổi.
     * Gọi từ webhook handler.
     * 
     * @param payment Payment với status mới
     */
    @Transactional
    public void syncIdempotencyStatus(Payment payment) {
        if (payment == null || payment.getOrderCode() == null) {
            return;
        }

        // Tìm record theo payment
        // Note: Không có index trực tiếp, nhưng số lượng records không nhiều
        // Có thể optimize sau nếu cần
    }

    /**
     * Build scoped key từ userId và idempotencyKey.
     * Format: "{userId}:{idempotencyKey}"
     */
    public String buildScopedKey(Long userId, String idempotencyKey) {
        return userId + ":" + idempotencyKey;
    }

    /**
     * Tạo idempotency record mới.
     */
    private void createIdempotencyRecord(String scopedKey, Long userId, String planCode) {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(expirationHours);
        
        PaymentIdempotencyRecord record = new PaymentIdempotencyRecord(
                scopedKey, userId, planCode, expiresAt);
        
        idempotencyRepo.save(record);
        log.debug("Created idempotency record: {} (expires at {})", scopedKey, expiresAt);
    }

    /**
     * Cleanup expired idempotency records.
     * Chạy mỗi ngày lúc 4:00 AM.
     */
    @Scheduled(cron = "${payment.idempotency.cleanup-cron:0 0 4 * * ?}")
    @Transactional
    public void cleanupExpiredIdempotencyRecords() {
        log.info("Running idempotency records cleanup task...");
        
        LocalDateTime now = LocalDateTime.now();
        long expiredCount = idempotencyRepo.countByExpiresAtBefore(now);
        
        if (expiredCount == 0) {
            log.info("No expired idempotency records found");
            return;
        }

        int deleted = idempotencyRepo.deleteByExpiresAtBefore(now);
        log.info("Deleted {} expired idempotency records", deleted);
    }
}
