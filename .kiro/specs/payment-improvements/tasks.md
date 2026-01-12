# Implementation Plan: Payment Improvements

## Overview

Implement 2 features cho Payment Module:
1. Payment Email Notification - Gửi email xác nhận khi thanh toán thành công
2. Idempotency Key - Tránh tạo duplicate payment khi network retry

## Tasks

- [x] 1. Database Migration và Entity
  - [x] 1.1 Tạo migration V6 cho payment_idempotency_records table
    - Tạo file `src/main/resources/db/migration/V6__payment_idempotency.sql`
    - Table với columns: id, scoped_key (unique), user_id, plan_code, payment_id, status, created_at, expires_at
    - Indexes cho lookup và cleanup
    - _Requirements: 2.4, 2.6_
  - [x] 1.2 Tạo PaymentIdempotencyRecord entity
    - Tạo file `src/main/java/com/htai/exe201phapluatso/payment/entity/PaymentIdempotencyRecord.java`
    - JPA entity với proper annotations
    - _Requirements: 2.4, 2.6_
  - [x] 1.3 Tạo PaymentIdempotencyRecordRepo
    - Tạo file `src/main/java/com/htai/exe201phapluatso/payment/repo/PaymentIdempotencyRecordRepo.java`
    - Methods: findByScopedKey, deleteByExpiresAtBefore
    - _Requirements: 2.1, 2.7_

- [x] 2. Payment Email Service
  - [x] 2.1 Tạo PaymentEmailService
    - Tạo file `src/main/java/com/htai/exe201phapluatso/payment/service/PaymentEmailService.java`
    - @Async method sendPaymentSuccessEmail(Payment)
    - HTML email template với thông tin: orderCode, planName, amount, credits, paidAt
    - Fallback log to console khi email disabled
    - Error handling: log error, không throw exception
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_
  - [x] 2.2 Enable @Async trong Application
    - Thêm @EnableAsync vào main Application class
    - _Requirements: 1.6_

- [x] 3. Idempotency Service
  - [x] 3.1 Tạo IdempotencyService
    - Tạo file `src/main/java/com/htai/exe201phapluatso/payment/service/IdempotencyService.java`
    - Method checkAndSaveIdempotencyKey(userId, idempotencyKey, planCode) → Optional<Payment>
    - Method updateIdempotencyRecord(scopedKey, payment)
    - Scoped key format: "{userId}:{idempotencyKey}"
    - Logic: PENDING/SUCCESS → return existing, FAILED/EXPIRED/CANCELLED → allow new
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.6_
  - [x] 3.2 Thêm scheduled cleanup task
    - Method cleanupExpiredIdempotencyRecords() với @Scheduled
    - Xóa records có expires_at < now
    - _Requirements: 2.7_

- [x] 4. Tích hợp vào PayOSService
  - [x] 4.1 Inject PaymentEmailService vào PayOSService
    - Thêm dependency injection
    - _Requirements: 1.1_
  - [x] 4.2 Gọi sendPaymentSuccessEmail trong handleWebhook
    - Sau khi payment.setStatus("SUCCESS")
    - Gọi async email service
    - _Requirements: 1.1, 1.6_

- [x] 5. Tích hợp vào PaymentController
  - [x] 5.1 Inject IdempotencyService vào PaymentController
    - Thêm dependency injection
    - _Requirements: 2.1_
  - [x] 5.2 Thêm Idempotency-Key header vào createPayment endpoint
    - @RequestHeader(value = "Idempotency-Key", required = false)
    - Check idempotency trước khi gọi payOSService.createPayment
    - Return existing payment nếu key đã tồn tại với PENDING/SUCCESS
    - _Requirements: 2.1, 2.2, 2.3, 2.5_

- [x] 6. Checkpoint - Verify implementation
  - Ensure all code compiles without errors
  - Review logic flow
  - Ask user if questions arise

## Notes

- Code phải production-ready, clean và đúng logic
- Tất cả messages tiếng Việt
- Database migration phải là V6 (sau V5__auth_security.sql)
- Email service phải async để không block webhook
- Idempotency key có expiration 24h
