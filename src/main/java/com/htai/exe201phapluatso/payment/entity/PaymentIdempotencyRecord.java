package com.htai.exe201phapluatso.payment.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity lưu idempotency keys để tránh duplicate payment khi network retry.
 * Mỗi record có thời hạn 24 giờ.
 */
@Entity
@Table(name = "payment_idempotency_records")
public class PaymentIdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Scoped key format: "{userId}:{idempotencyKey}"
     * Đảm bảo mỗi user có namespace riêng cho idempotency keys
     */
    @Column(name = "scoped_key", unique = true, nullable = false, length = 255)
    private String scopedKey;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "plan_code", length = 50)
    private String planCode;

    /**
     * Payment được tạo từ request này.
     * Nullable vì có thể chưa tạo xong payment.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    /**
     * Status của idempotency record:
     * - PENDING: đang xử lý
     * - SUCCESS: payment thành công
     * - FAILED: payment thất bại
     * - EXPIRED: payment hết hạn
     */
    @Column(name = "status", length = 20)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Thời điểm key hết hạn (mặc định 24 giờ sau khi tạo)
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // ==================== Constructors ====================

    public PaymentIdempotencyRecord() {
    }

    public PaymentIdempotencyRecord(String scopedKey, Long userId, String planCode, LocalDateTime expiresAt) {
        this.scopedKey = scopedKey;
        this.userId = userId;
        this.planCode = planCode;
        this.expiresAt = expiresAt;
    }

    // ==================== Getters and Setters ====================

    public Long getId() {
        return id;
    }

    public String getScopedKey() {
        return scopedKey;
    }

    public void setScopedKey(String scopedKey) {
        this.scopedKey = scopedKey;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPlanCode() {
        return planCode;
    }

    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    // ==================== Helper Methods ====================

    /**
     * Check if this record allows creating a new payment.
     * Only FAILED, EXPIRED, CANCELLED status allow retry.
     */
    public boolean allowsNewPayment() {
        return "FAILED".equals(status) || "EXPIRED".equals(status) || "CANCELLED".equals(status);
    }

    /**
     * Check if this record has an active payment (PENDING or CREDITED).
     * NOTE: SUCCESS status is deprecated, only CREDITED indicates full success
     */
    public boolean hasActivePayment() {
        return "PENDING".equals(status) || "CREDITED".equals(status);
    }

    /**
     * Check if this record has expired.
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    @Override
    public String toString() {
        return "PaymentIdempotencyRecord{" +
                "id=" + id +
                ", scopedKey='" + scopedKey + '\'' +
                ", userId=" + userId +
                ", planCode='" + planCode + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                '}';
    }
}
