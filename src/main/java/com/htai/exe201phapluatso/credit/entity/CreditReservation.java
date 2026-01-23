package com.htai.exe201phapluatso.credit.entity;

import com.htai.exe201phapluatso.auth.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity for tracking credit reservations
 * Supports reserve/confirm/refund pattern for AI operations
 */
@Entity
@Table(name = "credit_reservations")
public class CreditReservation {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_REFUNDED = "REFUNDED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "credit_type", nullable = false, length = 20)
    private String creditType;  // CHAT, QUIZ_GEN

    @Column(nullable = false)
    private Integer amount = 1;

    @Column(nullable = false, length = 20)
    private String status = STATUS_PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "operation_type", length = 50)
    private String operationType;  // AI_CHAT, AI_QUIZ_GEN, AI_CHAT_SESSION

    @Column(name = "session_id")
    private Long sessionId;

    // Getters and Setters
    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getCreditType() { return creditType; }
    public void setCreditType(String creditType) { this.creditType = creditType; }

    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }

    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    public boolean isExpired() {
        return STATUS_PENDING.equals(status) && LocalDateTime.now().isAfter(expiresAt);
    }
}
