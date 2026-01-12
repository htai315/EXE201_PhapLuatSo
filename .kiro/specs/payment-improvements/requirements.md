# Requirements Document

## Introduction

Tài liệu yêu cầu cho việc cải thiện Module Payment của hệ thống Pháp Luật Số. Các cải thiện tập trung vào:
1. Gửi email thông báo khi thanh toán thành công
2. Idempotency key để tránh tạo duplicate payment khi network retry

## Glossary

- **Payment_System**: Hệ thống xử lý thanh toán qua PayOS
- **Email_Notification_Service**: Service gửi email thông báo thanh toán
- **Idempotency_Manager**: Component quản lý idempotency key để tránh duplicate requests
- **Idempotency_Key**: Unique key do client gửi để đảm bảo request chỉ được xử lý một lần
- **Payment_Webhook**: Callback từ PayOS khi thanh toán hoàn tất

## Requirements

### Requirement 1: Payment Email Notification

**User Story:** As a user, I want to receive an email confirmation when my payment is successful, so that I have a record of my purchase.

#### Acceptance Criteria

1.1. WHEN a payment is marked as SUCCESS via webhook, THE Email_Notification_Service SHALL send a confirmation email to the user's registered email address
1.2. THE Email_Notification_Service SHALL include the following information in the email: order code, plan name, amount paid, credits received (chat + quiz), payment date
1.3. WHEN email sending fails, THE Payment_System SHALL log the error but NOT affect the payment status (payment remains SUCCESS)
1.4. WHEN email is disabled in configuration, THE Email_Notification_Service SHALL log the email content to console instead of sending
1.5. THE Email_Notification_Service SHALL use HTML email template with professional formatting
1.6. THE Email_Notification_Service SHALL send email asynchronously to avoid blocking webhook processing

### Requirement 2: Idempotency Key for Payment Creation

**User Story:** As a system administrator, I want to prevent duplicate payments when network issues cause request retries, so that users are not charged multiple times.

#### Acceptance Criteria

2.1. WHEN a client sends a payment creation request with an Idempotency-Key header, THE Payment_System SHALL check if a payment with that key already exists
2.2. WHEN a payment with the same idempotency key exists and is PENDING or SUCCESS, THE Payment_System SHALL return the existing payment response instead of creating a new one
2.3. WHEN a payment with the same idempotency key exists but is FAILED/EXPIRED/CANCELLED, THE Payment_System SHALL create a new payment
2.4. THE Idempotency_Manager SHALL store idempotency keys in database with expiration time of 24 hours
2.5. WHEN Idempotency-Key header is not provided, THE Payment_System SHALL behave as before (use existing reuse logic)
2.6. THE Idempotency_Manager SHALL use format: "{userId}:{idempotencyKey}" to scope keys per user
2.7. THE scheduled cleanup task SHALL remove expired idempotency records older than 24 hours

