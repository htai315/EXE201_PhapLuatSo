package com.htai.exe201phapluatso.payment.entity;

import com.htai.exe201phapluatso.auth.entity.Plan;
import com.htai.exe201phapluatso.auth.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "vnp_txn_ref", length = 100)
    private String vnpTxnRef;

    @Column(name = "order_code", unique = true)
    private Long orderCode;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "vnp_transaction_no", length = 100)
    private String vnpTransactionNo;

    @Column(name = "vnp_bank_code", length = 20)
    private String vnpBankCode;

    @Column(name = "vnp_card_type", length = 20)
    private String vnpCardType;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "payment_method", length = 20)
    private String paymentMethod = "VNPAY";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "webhook_processed")
    private Boolean webhookProcessed = false;

    @Column(name = "checkout_url", length = 500)
    private String checkoutUrl;

    @Column(name = "qr_code", columnDefinition = "TEXT")
    private String qrCode;

    /**
     * Number of times we've tried to add credits after payment was successful.
     * Used by retry job for PAID_CREDIT_FAILED payments.
     */
    @Column(name = "credit_retry_count")
    private Integer creditRetryCount = 0;

    // ==================== STATUS CONSTANTS ====================
    // State machine: PENDING → PAID → CREDITED (happy path)
    // PENDING → PAID → PAID_CREDIT_FAILED → CREDITED (retry success)
    // PENDING → PAID → PAID_CREDIT_FAILED → NEEDS_REVIEW (retry exhausted)
    // PENDING → FAILED/CANCELLED/EXPIRED (no money received)

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PAID = "PAID"; // Money received, credits not yet added
    public static final String STATUS_CREDITED = "CREDITED"; // Money received AND credits added (SUCCESS)
    public static final String STATUS_PAID_CREDIT_FAILED = "PAID_CREDIT_FAILED"; // Money received but addCredits failed
    public static final String STATUS_FAILED = "FAILED"; // Payment failed (no money received)
    public static final String STATUS_CANCELLED = "CANCELLED"; // User cancelled (no money received)
    public static final String STATUS_EXPIRED = "EXPIRED"; // Payment expired (no money received)
    public static final String STATUS_NEEDS_REVIEW = "NEEDS_REVIEW"; // Manual review required

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getVnpTxnRef() {
        return vnpTxnRef;
    }

    public void setVnpTxnRef(String vnpTxnRef) {
        this.vnpTxnRef = vnpTxnRef;
    }

    public String getVnpTransactionNo() {
        return vnpTransactionNo;
    }

    public void setVnpTransactionNo(String vnpTransactionNo) {
        this.vnpTransactionNo = vnpTransactionNo;
    }

    public String getVnpBankCode() {
        return vnpBankCode;
    }

    public void setVnpBankCode(String vnpBankCode) {
        this.vnpBankCode = vnpBankCode;
    }

    public String getVnpCardType() {
        return vnpCardType;
    }

    public void setVnpCardType(String vnpCardType) {
        this.vnpCardType = vnpCardType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Long getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(Long orderCode) {
        this.orderCode = orderCode;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Boolean getWebhookProcessed() {
        return webhookProcessed;
    }

    public void setWebhookProcessed(Boolean webhookProcessed) {
        this.webhookProcessed = webhookProcessed;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public Integer getCreditRetryCount() {
        return creditRetryCount != null ? creditRetryCount : 0;
    }

    public void setCreditRetryCount(Integer creditRetryCount) {
        this.creditRetryCount = creditRetryCount;
    }

    public void incrementCreditRetryCount() {
        this.creditRetryCount = getCreditRetryCount() + 1;
    }

    // ==================== Status Helper Methods ====================

    /**
     * Check if payment is in a final success state (money received AND credits
     * added)
     * NOTE: SUCCESS status is deprecated, only CREDITED indicates full success
     */
    public boolean isSuccessful() {
        return STATUS_CREDITED.equals(status);
    }

    /**
     * Check if money was received (PAID, CREDITED, or PAID_CREDIT_FAILED)
     */
    public boolean isMoneyReceived() {
        return STATUS_PAID.equals(status) || STATUS_CREDITED.equals(status)
                || STATUS_PAID_CREDIT_FAILED.equals(status);
    }

    // Helper methods for admin
    public String getOrderId() {
        return orderCode != null ? String.valueOf(orderCode) : vnpTxnRef;
    }

    public String getPlanCode() {
        return plan != null ? plan.getCode() : null;
    }

    public String getTransactionNo() {
        return transactionId != null ? transactionId : vnpTransactionNo;
    }
}
