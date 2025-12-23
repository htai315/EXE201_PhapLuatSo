# VNPay Payment Integration - Implementation Plan

## Overview
Tích hợp cổng thanh toán VNPay để người dùng có thể mua credits tự động cho 3 gói: FREE, REGULAR, STUDENT.

## Current System Status
✅ Credits system đã hoàn thiện:
- Database tables: `user_credits`, `credit_transactions`, `plans`
- Backend service: `CreditService` với methods `addCredits()`, `checkAndDeduct()`
- Frontend: Credits counter, Plans page
- 3 Plans: FREE (10 chat - 0đ), REGULAR (100 chat - 159K), STUDENT (100 chat + 20 quiz - 249K)

## VNPay Integration Architecture

### Flow Diagram
```
User clicks "Mua ngay" on plans.html
    ↓
POST /api/payment/create-payment
    ↓
Backend creates Payment record (PENDING)
    ↓
Generate VNPay payment URL with signature
    ↓
Redirect user to VNPay payment page
    ↓
User completes payment on VNPay
    ↓
VNPay redirects back to /api/payment/vnpay-return (frontend callback)
    ↓
VNPay sends IPN to /api/payment/vnpay-ipn (backend webhook)
    ↓
Verify signature, update Payment status
    ↓
If SUCCESS: Call creditService.addCredits()
    ↓
Show success page with updated credits
```

## Implementation Steps

### Phase 1: Database Schema (Migration)
**File**: `src/main/resources/db/migration/V2__add_payment_tables.sql`

```sql
-- Payment transactions table
CREATE TABLE payments (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_code VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    
    -- VNPay fields
    vnp_txn_ref VARCHAR(100) UNIQUE NOT NULL,  -- Order ID
    vnp_transaction_no VARCHAR(100),            -- VNPay transaction ID
    vnp_bank_code VARCHAR(20),
    vnp_card_type VARCHAR(20),
    
    -- Status tracking
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SUCCESS, FAILED, CANCELLED
    payment_method VARCHAR(20) DEFAULT 'VNPAY',
    
    -- Timestamps
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    paid_at DATETIME2,
    
    -- Metadata
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (plan_code) REFERENCES plans(code)
);

CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_vnp_txn_ref ON payments(vnp_txn_ref);
CREATE INDEX idx_payments_status ON payments(status);
```

### Phase 2: Backend Implementation

#### 2.1 Entity Classes
**Files to create**:
- `src/main/java/com/htai/exe201phapluatso/payment/entity/Payment.java`
- `src/main/java/com/htai/exe201phapluatso/payment/repo/PaymentRepo.java`

#### 2.2 VNPay Configuration
**File**: `src/main/java/com/htai/exe201phapluatso/payment/config/VNPayConfig.java`

```java
@Configuration
public class VNPayConfig {
    @Value("${vnpay.tmn-code}")
    private String tmnCode;
    
    @Value("${vnpay.hash-secret}")
    private String hashSecret;
    
    @Value("${vnpay.url}")
    private String vnpayUrl;
    
    @Value("${vnpay.return-url}")
    private String returnUrl;
    
    // Getters...
}
```

**Add to `application.properties`**:
```properties
# VNPay Configuration
vnpay.tmn-code=YOUR_TMN_CODE
vnpay.hash-secret=YOUR_HASH_SECRET
vnpay.url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnpay.return-url=http://localhost:8080/payment-result.html
vnpay.ipn-url=http://localhost:8080/api/payment/vnpay-ipn
```

#### 2.3 VNPay Service
**File**: `src/main/java/com/htai/exe201phapluatso/payment/service/VNPayService.java`

Key methods:
- `createPaymentUrl(userId, planCode, ipAddress)` - Generate VNPay URL
- `verifySignature(params)` - Verify VNPay callback signature
- `processPaymentReturn(params)` - Handle frontend callback
- `processPaymentIPN(params)` - Handle backend webhook

#### 2.4 Payment Service
**File**: `src/main/java/com/htai/exe201phapluatso/payment/service/PaymentService.java`

Key methods:
- `createPayment(userId, planCode)` - Create payment record
- `updatePaymentStatus(txnRef, status, vnpayData)` - Update after payment
- `handleSuccessfulPayment(payment)` - Add credits after success

#### 2.5 Payment Controller
**File**: `src/main/java/com/htai/exe201phapluatso/payment/controller/PaymentController.java`

Endpoints:
- `POST /api/payment/create` - Create payment and get VNPay URL
- `GET /api/payment/vnpay-return` - Frontend callback (redirect to result page)
- `GET /api/payment/vnpay-ipn` - Backend webhook (VNPay server-to-server)
- `GET /api/payment/history` - Get user's payment history

#### 2.6 DTOs
**Files to create**:
- `CreatePaymentRequest.java` - { planCode }
- `CreatePaymentResponse.java` - { paymentUrl, txnRef }
- `PaymentHistoryResponse.java` - List of payments

### Phase 3: Frontend Implementation

#### 3.1 Update Plans Page
**File**: `src/main/resources/static/html/plans.html`

Add payment flow:
```javascript
async function purchasePlan(planCode) {
    try {
        const response = await fetch('/api/payment/create', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getAccessToken()}`
            },
            body: JSON.stringify({ planCode })
        });
        
        const data = await response.json();
        
        // Redirect to VNPay
        window.location.href = data.paymentUrl;
    } catch (error) {
        console.error('Payment error:', error);
        alert('Có lỗi xảy ra. Vui lòng thử lại.');
    }
}
```

#### 3.2 Payment Result Page
**File**: `src/main/resources/static/html/payment-result.html`

Display payment result:
- Success: Show credits added, redirect to home after 3s
- Failed: Show error message, button to retry
- Parse URL params from VNPay return

#### 3.3 Payment History Page
**File**: `src/main/resources/static/html/payment-history.html`

Show user's payment transactions:
- Date, Plan, Amount, Status
- Filter by status
- Pagination

### Phase 4: Security & Testing

#### 4.1 Security Measures
- ✅ Verify VNPay signature on all callbacks
- ✅ Use HTTPS in production
- ✅ Validate payment amount matches plan price
- ✅ Prevent duplicate payment processing (check txnRef)
- ✅ Log all payment attempts for audit
- ✅ Rate limiting on payment creation endpoint

#### 4.2 Testing Checklist
- [ ] Test with VNPay sandbox
- [ ] Test successful payment flow
- [ ] Test failed payment flow
- [ ] Test cancelled payment
- [ ] Test duplicate IPN calls
- [ ] Test signature verification
- [ ] Test concurrent payment attempts
- [ ] Test credits addition after payment

### Phase 5: Production Deployment

#### 5.1 VNPay Account Setup
1. Register merchant account at https://vnpay.vn
2. Get TMN Code and Hash Secret
3. Configure IPN URL (must be public HTTPS)
4. Test in sandbox first

#### 5.2 Configuration Updates
- Update `vnpay.url` to production URL
- Update `vnpay.return-url` to production domain
- Update `vnpay.ipn-url` to production domain
- Use environment variables for secrets

#### 5.3 Monitoring
- Log all payment transactions
- Monitor payment success rate
- Alert on failed payments
- Track revenue metrics

## File Structure
```
src/main/java/com/htai/exe201phapluatso/
├── payment/
│   ├── entity/
│   │   └── Payment.java
│   ├── repo/
│   │   └── PaymentRepo.java
│   ├── service/
│   │   ├── VNPayService.java
│   │   └── PaymentService.java
│   ├── controller/
│   │   └── PaymentController.java
│   ├── config/
│   │   └── VNPayConfig.java
│   └── dto/
│       ├── CreatePaymentRequest.java
│       ├── CreatePaymentResponse.java
│       └── PaymentHistoryResponse.java

src/main/resources/
├── db/migration/
│   └── V2__add_payment_tables.sql
└── static/
    └── html/
        ├── payment-result.html
        └── payment-history.html
```

## Estimated Timeline
- **Phase 1** (Database): 30 minutes
- **Phase 2** (Backend): 3-4 hours
- **Phase 3** (Frontend): 2-3 hours
- **Phase 4** (Testing): 2 hours
- **Phase 5** (Deployment): 1 hour

**Total**: ~8-10 hours

## Dependencies to Add (pom.xml)
```xml
<!-- For HMAC SHA256 signature -->
<dependency>
    <groupId>commons-codec</groupId>
    <artifactId>commons-codec</artifactId>
</dependency>
```

## Important Notes

### VNPay Sandbox Testing
- Use test cards provided by VNPay
- Sandbox URL: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
- Test TMN Code and Hash Secret from VNPay sandbox

### IPN (Instant Payment Notification)
- VNPay will call your IPN URL even if user closes browser
- IPN ensures payment is processed even if return URL fails
- Must return `RspCode=00` to VNPay to confirm receipt

### Signature Verification
- Critical for security
- Verify on both return URL and IPN
- Use HMAC SHA512 with Hash Secret

### Idempotency
- Check `vnp_txn_ref` before processing
- Prevent duplicate credit additions
- Use database transaction locks

## Next Steps

Bạn muốn tôi:
1. **Tạo spec file** chi tiết theo format EARS requirements?
2. **Bắt đầu implement** luôn từ Phase 1?
3. **Giải thích thêm** về bất kỳ phần nào?

Tôi recommend tạo spec file trước để có requirements rõ ràng, sau đó implement từng phase một cách có hệ thống.
