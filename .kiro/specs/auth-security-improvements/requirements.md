# Requirements Document

## Introduction

Tài liệu này mô tả các yêu cầu cải thiện bảo mật cho Module Authentication của hệ thống Pháp Luật Số. Dựa trên kết quả code review, module hiện tại có JWT/OAuth2 implementation tốt nhưng thiếu một số tính năng bảo mật quan trọng cho production: Rate Limiting, Refresh Token Rotation, Account Lockout, và Password Policy.

## Glossary

- **Auth_Service**: Service xử lý logic authentication (đăng nhập, đăng ký, refresh token)
- **Rate_Limiter**: Thành phần giới hạn số lượng request trong khoảng thời gian
- **Refresh_Token**: Token dùng để lấy access token mới khi hết hạn
- **Token_Rotation**: Cơ chế tạo refresh token mới và vô hiệu hóa token cũ mỗi lần refresh
- **Account_Lockout**: Cơ chế khóa tài khoản tạm thời sau nhiều lần đăng nhập thất bại
- **Password_Policy**: Quy tắc về độ mạnh của mật khẩu
- **Login_Attempt**: Một lần thử đăng nhập (thành công hoặc thất bại)
- **Lockout_Duration**: Thời gian tài khoản bị khóa tạm thời

## Requirements

### Requirement 1: Rate Limiting cho Authentication Endpoints

**User Story:** As a security administrator, I want to limit authentication attempts, so that brute force attacks are prevented.

#### Acceptance Criteria

1. WHEN a client sends more than 5 login requests within 1 minute from the same IP THEN THE Rate_Limiter SHALL reject subsequent requests with HTTP 429 status
2. WHEN a client sends more than 3 password reset requests within 5 minutes from the same IP THEN THE Rate_Limiter SHALL reject subsequent requests with HTTP 429 status
3. WHEN a client sends more than 5 registration requests within 10 minutes from the same IP THEN THE Rate_Limiter SHALL reject subsequent requests with HTTP 429 status
4. WHEN a rate-limited request is rejected THEN THE Auth_Service SHALL return JSON response with error code "RATE_LIMIT_EXCEEDED" and Retry-After header
5. WHEN rate limit is exceeded THEN THE Auth_Service SHALL log the event with IP address, endpoint, and timestamp for security monitoring

### Requirement 2: Refresh Token Rotation

**User Story:** As a security administrator, I want refresh tokens to be rotated on each use, so that stolen tokens have limited usefulness.

#### Acceptance Criteria

1. WHEN a user calls the refresh endpoint with a valid refresh token THEN THE Auth_Service SHALL generate a new refresh token and invalidate the old one
2. WHEN a user attempts to use an already-rotated (invalidated) refresh token THEN THE Auth_Service SHALL reject the request with HTTP 401 status
3. WHEN a rotated token is detected (reuse attempt) THEN THE Auth_Service SHALL invalidate ALL refresh tokens for that user (security breach response)
4. THE Auth_Service SHALL store refresh tokens in database with user_id, token_hash, created_at, expires_at, and revoked_at fields
5. WHEN a refresh token is successfully rotated THEN THE Auth_Service SHALL log the rotation event for audit purposes

### Requirement 3: Account Lockout

**User Story:** As a security administrator, I want accounts to be temporarily locked after failed login attempts, so that brute force attacks on specific accounts are prevented.

#### Acceptance Criteria

1. WHEN a user fails to login 5 times consecutively THEN THE Auth_Service SHALL lock the account for 15 minutes
2. WHEN an account is locked THEN THE Auth_Service SHALL return HTTP 423 status with message indicating lockout duration remaining
3. WHEN lockout duration expires THEN THE Auth_Service SHALL automatically unlock the account and reset failed attempt counter
4. WHEN a user successfully logs in THEN THE Auth_Service SHALL reset the failed attempt counter to zero
5. WHEN an account is locked THEN THE Auth_Service SHALL send email notification to the user about the lockout
6. THE Auth_Service SHALL store failed_login_attempts and locked_until fields in the users table

### Requirement 4: Password Policy Validation

**User Story:** As a security administrator, I want password strength requirements enforced, so that users create secure passwords.

#### Acceptance Criteria

1. WHEN a user registers or changes password THEN THE Auth_Service SHALL require minimum 8 characters
2. WHEN a user registers or changes password THEN THE Auth_Service SHALL require at least 1 uppercase letter
3. WHEN a user registers or changes password THEN THE Auth_Service SHALL require at least 1 lowercase letter
4. WHEN a user registers or changes password THEN THE Auth_Service SHALL require at least 1 digit
5. WHEN a user registers or changes password THEN THE Auth_Service SHALL require at least 1 special character (!@#$%^&*)
6. IF password does not meet policy THEN THE Auth_Service SHALL return HTTP 400 with specific validation errors in Vietnamese
7. THE Auth_Service SHALL validate password policy on both registration and password change endpoints

### Requirement 5: Security Logging and Monitoring

**User Story:** As a security administrator, I want comprehensive security event logging, so that I can monitor and investigate security incidents.

#### Acceptance Criteria

1. WHEN a login attempt occurs (success or failure) THEN THE Auth_Service SHALL log user_id/email, IP address, user_agent, timestamp, and result
2. WHEN an account is locked THEN THE Auth_Service SHALL log the lockout event with reason and duration
3. WHEN a refresh token rotation occurs THEN THE Auth_Service SHALL log the event with user_id and timestamp
4. WHEN a rate limit is exceeded THEN THE Auth_Service SHALL log the event with IP address and endpoint
5. THE Auth_Service SHALL store security logs in a dedicated security_audit_log table for compliance and investigation
