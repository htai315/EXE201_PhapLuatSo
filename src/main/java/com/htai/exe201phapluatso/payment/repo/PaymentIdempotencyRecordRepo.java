package com.htai.exe201phapluatso.payment.repo;

import com.htai.exe201phapluatso.payment.entity.PaymentIdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository cho PaymentIdempotencyRecord.
 * Quản lý idempotency keys để tránh duplicate payment.
 */
@Repository
public interface PaymentIdempotencyRecordRepo extends JpaRepository<PaymentIdempotencyRecord, Long> {

    /**
     * Tìm record theo scoped key.
     * Scoped key format: "{userId}:{idempotencyKey}"
     */
    Optional<PaymentIdempotencyRecord> findByScopedKey(String scopedKey);

    /**
     * Tìm record theo scoped key với payment (eager fetch).
     */
    @Query("SELECT r FROM PaymentIdempotencyRecord r LEFT JOIN FETCH r.payment WHERE r.scopedKey = :scopedKey")
    Optional<PaymentIdempotencyRecord> findByScopedKeyWithPayment(@Param("scopedKey") String scopedKey);

    /**
     * Xóa các records đã hết hạn.
     * Dùng cho scheduled cleanup task.
     * 
     * @param expiresAt Xóa records có expires_at trước thời điểm này
     * @return Số records đã xóa
     */
    @Modifying
    @Query("DELETE FROM PaymentIdempotencyRecord r WHERE r.expiresAt < :expiresAt")
    int deleteByExpiresAtBefore(@Param("expiresAt") LocalDateTime expiresAt);

    /**
     * Đếm số records đã hết hạn (để log trước khi xóa).
     */
    @Query("SELECT COUNT(r) FROM PaymentIdempotencyRecord r WHERE r.expiresAt < :expiresAt")
    long countByExpiresAtBefore(@Param("expiresAt") LocalDateTime expiresAt);

    /**
     * Check xem scoped key đã tồn tại chưa.
     */
    boolean existsByScopedKey(String scopedKey);
}
