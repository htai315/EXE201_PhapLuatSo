# Credit System Improvements - Design

## 1. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                      Credit System                               │
├─────────────────────────────────────────────────────────────────┤
│  CreditService (enhanced)                                        │
│  ├── reserveCredit() ──────► CreditReservation                  │
│  ├── confirmReservation() ─► Deduct + Log CONFIRM               │
│  ├── refundReservation() ──► Restore + Log REFUND               │
│  └── Optimistic Locking ───► @Version + Retry                   │
├─────────────────────────────────────────────────────────────────┤
│  AdminCreditService (new)                                        │
│  ├── addCredits() ─────────► Log ADMIN_ADD                      │
│  ├── removeCredits() ──────► Log ADMIN_REMOVE                   │
│  └── getCreditAnalytics() ─► Usage Statistics                   │
├─────────────────────────────────────────────────────────────────┤
│  CreditReservationCleanupScheduler (new)                        │
│  └── cleanupExpiredReservations() ─► Auto refund expired        │
└─────────────────────────────────────────────────────────────────┘
```

## 2. Database Changes

### 2.1 Migration V7__credit_improvements.sql

```sql
-- Add version column for optimistic locking
ALTER TABLE user_credits ADD COLUMN version INTEGER NOT NULL DEFAULT 0;

-- Create credit_reservations table
CREATE TABLE credit_reservations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    credit_type VARCHAR(20) NOT NULL,  -- CHAT, QUIZ_GEN
    amount INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, CONFIRMED, REFUNDED, EXPIRED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    confirmed_at TIMESTAMP,
    refunded_at TIMESTAMP,
    operation_type VARCHAR(50),  -- AI_CHAT, AI_QUIZ_GEN
    
    CONSTRAINT chk_reservation_status CHECK (status IN ('PENDING', 'CONFIRMED', 'REFUNDED', 'EXPIRED'))
);

CREATE INDEX idx_credit_reservations_user_status ON credit_reservations(user_id, status);
CREATE INDEX idx_credit_reservations_expires ON credit_reservations(expires_at) WHERE status = 'PENDING';
```

## 3. Entity Changes

### 3.1 UserCredit.java - Add Version

```java
@Entity
@Table(name = "user_credits")
public class UserCredit {
    // ... existing fields ...
    
    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;
    
    // getter/setter
}
```

### 3.2 CreditReservation.java (New Entity)

```java
@Entity
@Table(name = "credit_reservations")
public class CreditReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "credit_type", nullable = false, length = 20)
    private String creditType;  // CHAT, QUIZ_GEN
    
    @Column(nullable = false)
    private Integer amount = 1;
    
    @Column(nullable = false, length = 20)
    private String status = "PENDING";  // PENDING, CONFIRMED, REFUNDED, EXPIRED
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
    
    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;
    
    @Column(name = "operation_type", length = 50)
    private String operationType;  // AI_CHAT, AI_QUIZ_GEN
}
```

## 4. Service Design

### 4.1 CreditService Enhancement

```java
// Reserve credit (deduct immediately, track for potential refund)
@Transactional
public CreditReservation reserveCredit(Long userId, String creditType, String operationType) {
    // 1. Get credits with optimistic lock (retry on conflict)
    // 2. Check balance
    // 3. Deduct credit
    // 4. Create reservation record
    // 5. Log RESERVE transaction
    // 6. Return reservation
}

// Confirm reservation (mark as confirmed)
@Transactional
public void confirmReservation(Long reservationId) {
    // 1. Find reservation
    // 2. Update status to CONFIRMED
    // 3. Log CONFIRM transaction
}

// Refund reservation (restore credit)
@Transactional
public void refundReservation(Long reservationId) {
    // 1. Find reservation
    // 2. Restore credit to user
    // 3. Update status to REFUNDED
    // 4. Log REFUND transaction
}
```

### 4.2 AdminCreditService (New)

```java
@Service
public class AdminCreditService {
    
    // Add credits to user
    @Transactional
    public void addCredits(Long userId, int chatCredits, int quizGenCredits, 
                          String reason, User adminUser) {
        // 1. Get user credits
        // 2. Add credits
        // 3. Log ADMIN_ADD transaction
        // 4. Log admin activity
    }
    
    // Remove credits from user
    @Transactional
    public void removeCredits(Long userId, int chatCredits, int quizGenCredits,
                             String reason, User adminUser) {
        // 1. Get user credits
        // 2. Validate không âm
        // 3. Remove credits
        // 4. Log ADMIN_REMOVE transaction
        // 5. Log admin activity
    }
    
    // Get credit analytics
    public CreditAnalyticsResponse getCreditAnalytics(LocalDate from, LocalDate to) {
        // Query aggregated data from credit_transactions
    }
}
```

### 4.3 Optimistic Locking with Retry

```java
@Retryable(
    retryFor = OptimisticLockingFailureException.class,
    maxAttempts = 3,
    backoff = @Backoff(delay = 100)
)
@Transactional
public CreditReservation reserveCredit(Long userId, String creditType, String operationType) {
    // Implementation with optimistic locking
}

@Recover
public CreditReservation recoverReserveCredit(OptimisticLockingFailureException e, 
                                               Long userId, String creditType, String operationType) {
    throw new ServiceException("Không thể xử lý yêu cầu do hệ thống đang bận. Vui lòng thử lại.");
}
```

## 5. API Endpoints

### 5.1 Admin Credit Management

```
POST /api/admin/users/{id}/credits/add
Body: { "chatCredits": 10, "quizGenCredits": 5, "reason": "Bonus cho user" }

POST /api/admin/users/{id}/credits/remove
Body: { "chatCredits": 5, "quizGenCredits": 0, "reason": "Điều chỉnh" }
```

### 5.2 Credit Analytics

```
GET /api/admin/credits/analytics?from=2025-01-01&to=2025-01-31

Response:
{
    "totalChatUsed": 1500,
    "totalQuizGenUsed": 200,
    "totalChatPurchased": 5000,
    "totalQuizGenPurchased": 500,
    "totalRefunded": 50,
    "usageByDay": [
        { "date": "2025-01-01", "chatUsed": 50, "quizGenUsed": 10 },
        ...
    ]
}
```

## 6. DTOs

### 6.1 AdminCreditAdjustRequest

```java
public class AdminCreditAdjustRequest {
    @Min(0)
    private int chatCredits;
    
    @Min(0)
    private int quizGenCredits;
    
    @NotBlank
    @Size(max = 500)
    private String reason;
}
```

### 6.2 CreditAnalyticsResponse

```java
public class CreditAnalyticsResponse {
    private long totalChatUsed;
    private long totalQuizGenUsed;
    private long totalChatPurchased;
    private long totalQuizGenPurchased;
    private long totalRefunded;
    private List<DailyUsage> usageByDay;
    
    public static class DailyUsage {
        private LocalDate date;
        private long chatUsed;
        private long quizGenUsed;
    }
}
```

## 7. Integration Points

### 7.1 LegalChatService Integration

```java
// Before
creditService.checkAndDeductChatCredit(userId);
String response = openAIService.chat(prompt);

// After
CreditReservation reservation = creditService.reserveCredit(userId, "CHAT", "AI_CHAT");
try {
    String response = openAIService.chat(prompt);
    creditService.confirmReservation(reservation.getId());
    return response;
} catch (Exception e) {
    creditService.refundReservation(reservation.getId());
    throw e;
}
```

### 7.2 AIQuizService Integration

```java
// Similar pattern for quiz generation
CreditReservation reservation = creditService.reserveCredit(userId, "QUIZ_GEN", "AI_QUIZ_GEN");
try {
    QuizSet quiz = generateQuiz(request);
    creditService.confirmReservation(reservation.getId());
    return quiz;
} catch (Exception e) {
    creditService.refundReservation(reservation.getId());
    throw e;
}
```

## 8. Scheduler

### 8.1 CreditReservationCleanupScheduler

```java
@Component
public class CreditReservationCleanupScheduler {
    
    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void cleanupExpiredReservations() {
        // Find PENDING reservations where expires_at < now
        // For each: refund credit, update status to EXPIRED
    }
}
```

## 9. Configuration

```properties
# Credit reservation timeout (minutes)
credit.reservation.timeout-minutes=5

# Cleanup scheduler enabled
credit.reservation.cleanup.enabled=true
```

## 10. Error Handling

| Error | HTTP Code | Message (Vietnamese) |
|-------|-----------|---------------------|
| Insufficient credits | 403 | Bạn không đủ credits. Vui lòng mua thêm. |
| Reservation not found | 404 | Không tìm thấy reservation. |
| Reservation expired | 400 | Reservation đã hết hạn. |
| Concurrent conflict | 409 | Hệ thống đang bận. Vui lòng thử lại. |
| Cannot remove (negative) | 400 | Không thể trừ credits vì số dư không đủ. |
