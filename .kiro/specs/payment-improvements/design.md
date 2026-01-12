# Technical Design Document

## Introduction

Tài liệu thiết kế kỹ thuật cho Payment Improvements Module, bao gồm:
1. Payment Email Notification - Gửi email xác nhận khi thanh toán thành công
2. Idempotency Key - Tránh tạo duplicate payment khi network retry

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Payment Flow with Improvements                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  Client Request                                                               │
│       │                                                                       │
│       ▼                                                                       │
│  ┌─────────────────┐    ┌──────────────────────┐                             │
│  │PaymentController│───▶│  IdempotencyService  │                             │
│  └────────┬────────┘    │  (Check duplicate)   │                             │
│           │             └──────────┬───────────┘                             │
│           │                        │                                          │
│           │  ┌─────────────────────┴─────────────────────┐                   │
│           │  │ Exists & PENDING/SUCCESS?                 │                   │
│           │  │ YES → Return cached response              │                   │
│           │  │ NO  → Continue to PayOSService            │                   │
│           │  └───────────────────────────────────────────┘                   │
│           ▼                                                                   │
│  ┌─────────────────┐                                                         │
│  │  PayOSService   │──────────────────────────────────────┐                  │
│  │ (createPayment) │                                      │                  │
│  └────────┬────────┘                                      │                  │
│           │                                               │                  │
│           ▼                                               ▼                  │
│  ┌─────────────────┐                           ┌──────────────────┐          │
│  │    PayOS API    │                           │IdempotencyRecord │          │
│  │ (External)      │                           │   (Database)     │          │
│  └────────┬────────┘                           └──────────────────┘          │
│           │                                                                   │
│           │ Webhook (SUCCESS)                                                │
│           ▼                                                                   │
│  ┌─────────────────┐    ┌──────────────────────┐                             │
│  │  handleWebhook  │───▶│PaymentEmailService   │                             │
│  │ (PayOSService)  │    │ (Async email send)   │                             │
│  └─────────────────┘    └──────────────────────┘                             │
│                                                                               │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Component Design

### 1. PaymentEmailService

Service gửi email thông báo thanh toán thành công.

```java
@Service
public class PaymentEmailService {
    
    private final JavaMailSender mailSender;
    private final boolean emailEnabled;
    private final String fromEmail;
    
    /**
     * Gửi email xác nhận thanh toán thành công (async)
     * @param payment Payment entity với đầy đủ thông tin
     */
    @Async
    public void sendPaymentSuccessEmail(Payment payment) {
        // 1. Validate payment có user và plan
        // 2. Build HTML email content
        // 3. Send email hoặc log nếu disabled
        // 4. Log kết quả (không throw exception)
    }
    
    /**
     * Build HTML email template
     */
    private String buildPaymentSuccessEmailHtml(Payment payment) {
        // Template với: orderCode, planName, amount, credits, paidAt
    }
}
```

**Key Design Decisions:**
- Sử dụng `@Async` để không block webhook processing
- Không throw exception khi gửi email thất bại (payment vẫn SUCCESS)
- Fallback log to console khi email disabled
- HTML template với styling professional

### 2. IdempotencyService

Service quản lý idempotency key để tránh duplicate payments.

```java
@Service
public class IdempotencyService {
    
    private final PaymentIdempotencyRecordRepo idempotencyRepo;
    
    /**
     * Check và lưu idempotency key
     * @return Optional<Payment> - existing payment nếu key đã tồn tại
     */
    public Optional<Payment> checkAndSaveIdempotencyKey(
            Long userId, 
            String idempotencyKey,
            String planCode
    ) {
        // 1. Build scoped key: "{userId}:{idempotencyKey}"
        // 2. Check existing record
        // 3. If exists với PENDING/SUCCESS → return existing payment
        // 4. If exists với FAILED/EXPIRED → allow new payment
        // 5. If not exists → save new record
    }
    
    /**
     * Update idempotency record với payment result
     */
    public void updateIdempotencyRecord(String scopedKey, Payment payment) {
        // Update record với paymentId và status
    }
}
```

### 3. PaymentIdempotencyRecord Entity

```java
@Entity
@Table(name = "payment_idempotency_records")
public class PaymentIdempotencyRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "scoped_key", unique = true, nullable = false, length = 255)
    private String scopedKey;  // Format: "{userId}:{idempotencyKey}"
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "plan_code", length = 50)
    private String planCode;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;
    
    @Column(name = "status", length = 20)
    private String status;  // PENDING, SUCCESS, FAILED, EXPIRED
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;  // createdAt + 24 hours
}
```

## Database Design

### Migration V6: Payment Idempotency

```sql
-- Table for idempotency records
CREATE TABLE payment_idempotency_records (
    id BIGSERIAL PRIMARY KEY,
    scoped_key VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    plan_code VARCHAR(50),
    payment_id BIGINT REFERENCES payments(id),
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    
    CONSTRAINT fk_idempotency_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Index for cleanup query
CREATE INDEX idx_idempotency_expires_at ON payment_idempotency_records(expires_at);

-- Index for lookup
CREATE INDEX idx_idempotency_scoped_key ON payment_idempotency_records(scoped_key);
```

## API Changes

### PaymentController Updates

```java
@PostMapping("/create")
public ResponseEntity<CreatePaymentResponse> createPayment(
        @RequestBody CreatePaymentRequest request,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        Authentication authentication
) {
    // 1. If idempotencyKey provided → check IdempotencyService
    // 2. If existing payment found → return cached response
    // 3. Else → proceed with normal createPayment flow
}
```

## Correctness Properties

### Property 1: Email Delivery Independence
- **Invariant**: Payment status MUST NOT depend on email delivery success
- **Verification**: Payment marked SUCCESS before email send attempt
- **Recovery**: Email failures logged but not propagated

### Property 2: Idempotency Guarantee
- **Invariant**: Same idempotency key + userId MUST return same payment
- **Verification**: Database unique constraint on scoped_key
- **Edge Case**: FAILED/EXPIRED payments allow retry with same key

### Property 3: Async Email Non-Blocking
- **Invariant**: Webhook processing MUST complete within timeout
- **Verification**: @Async annotation ensures separate thread
- **Monitoring**: Log email send duration

### Property 4: Idempotency Key Expiration
- **Invariant**: Keys older than 24h MUST be cleaned up
- **Verification**: Scheduled task runs daily
- **Recovery**: Expired keys allow new payment creation

## Error Handling

| Scenario | Handling | User Impact |
|----------|----------|-------------|
| Email send fails | Log error, continue | None (payment still SUCCESS) |
| Email service unavailable | Log to console | None |
| Idempotency key exists (PENDING) | Return existing payment | Seamless retry |
| Idempotency key exists (SUCCESS) | Return existing payment | Seamless retry |
| Idempotency key exists (FAILED) | Allow new payment | Can retry |
| Database error on idempotency check | Proceed without idempotency | Potential duplicate (rare) |

## Configuration

```properties
# Email Configuration (existing)
spring.mail.enabled=true
spring.mail.username=your-email@gmail.com

# Idempotency Configuration (new)
payment.idempotency.expiration-hours=24
payment.idempotency.cleanup-cron=0 0 4 * * ?
```

## Testing Strategy

### Unit Tests
1. `PaymentEmailServiceTest`
   - Test email content generation
   - Test async behavior
   - Test disabled email fallback

2. `IdempotencyServiceTest`
   - Test duplicate key detection
   - Test key expiration logic
   - Test scoped key format

### Integration Tests
1. Test webhook triggers email send
2. Test idempotency with concurrent requests
3. Test cleanup scheduled task

## Sequence Diagrams

### Payment Creation with Idempotency

```
Client          Controller      IdempotencyService    PayOSService    Database
  │                 │                   │                  │              │
  │ POST /create    │                   │                  │              │
  │ + Idempotency-Key                   │                  │              │
  │────────────────▶│                   │                  │              │
  │                 │ checkIdempotency  │                  │              │
  │                 │──────────────────▶│                  │              │
  │                 │                   │ query            │              │
  │                 │                   │─────────────────────────────────▶
  │                 │                   │                  │              │
  │                 │   [Key exists?]   │                  │              │
  │                 │◀──────────────────│                  │              │
  │                 │                   │                  │              │
  │                 │ [NO] createPayment│                  │              │
  │                 │─────────────────────────────────────▶│              │
  │                 │                   │                  │ save         │
  │                 │                   │                  │─────────────▶│
  │                 │                   │                  │              │
  │                 │ saveIdempotencyRecord                │              │
  │                 │──────────────────▶│─────────────────────────────────▶
  │                 │                   │                  │              │
  │◀────────────────│                   │                  │              │
  │  Response       │                   │                  │              │
```

### Webhook with Email Notification

```
PayOS           Controller      PayOSService    PaymentEmailService    Database
  │                 │                │                   │                 │
  │ POST /webhook   │                │                   │                 │
  │────────────────▶│                │                   │                 │
  │                 │ handleWebhook  │                   │                 │
  │                 │───────────────▶│                   │                 │
  │                 │                │ update payment    │                 │
  │                 │                │────────────────────────────────────▶│
  │                 │                │                   │                 │
  │                 │                │ sendEmail (async) │                 │
  │                 │                │──────────────────▶│                 │
  │                 │                │                   │ [async thread]  │
  │◀────────────────│◀───────────────│                   │                 │
  │  {code: "00"}   │                │                   │                 │
  │                 │                │                   │ send email      │
  │                 │                │                   │────────────────▶│
```

## Files to Create/Modify

### New Files
1. `src/main/java/com/htai/exe201phapluatso/payment/service/PaymentEmailService.java`
2. `src/main/java/com/htai/exe201phapluatso/payment/service/IdempotencyService.java`
3. `src/main/java/com/htai/exe201phapluatso/payment/entity/PaymentIdempotencyRecord.java`
4. `src/main/java/com/htai/exe201phapluatso/payment/repo/PaymentIdempotencyRecordRepo.java`
5. `src/main/resources/db/migration/V6__payment_idempotency.sql`

### Modified Files
1. `src/main/java/com/htai/exe201phapluatso/payment/service/PayOSService.java`
   - Inject PaymentEmailService
   - Call sendPaymentSuccessEmail in handleWebhook after SUCCESS
2. `src/main/java/com/htai/exe201phapluatso/payment/controller/PaymentController.java`
   - Add Idempotency-Key header parameter
   - Integrate IdempotencyService in createPayment
