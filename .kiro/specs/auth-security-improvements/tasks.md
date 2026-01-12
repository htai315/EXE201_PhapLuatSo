# Implementation Plan: Auth Security Improvements

## Overview

Kế hoạch triển khai các cải thiện bảo mật cho Module Authentication, bao gồm Rate Limiting, Refresh Token Rotation, Account Lockout, Password Policy, và Security Logging. Sử dụng Java 17, Spring Boot 4.0, Bucket4j cho rate limiting.

## Tasks

- [x] 1. Database Migration và Entity Updates
  - [x] 1.1 Tạo migration V5__auth_security.sql
    - Thêm columns `failed_login_attempts`, `locked_until` vào users table
    - Thêm columns `used_at`, `replaced_by_token_id` vào refresh_tokens table
    - Tạo table `security_audit_log` với indexes
    - _Requirements: 2.4, 3.6, 5.5_
  - [x] 1.2 Update User entity
    - Thêm fields `failedLoginAttempts`, `lockedUntil`
    - Thêm getters/setters
    - _Requirements: 3.6_
  - [x] 1.3 Update RefreshToken entity
    - Thêm fields `usedAt`, `replacedByTokenId`
    - Thêm getters/setters
    - _Requirements: 2.4_
  - [x] 1.4 Tạo SecurityAuditLog entity
    - Tạo entity với tất cả fields theo design
    - Tạo SecurityAuditLogRepo
    - _Requirements: 5.5_

- [x] 2. Security Audit Service
  - [x] 2.1 Tạo SecurityAuditService interface và implementation
    - Implement các methods: logLoginAttempt, logAccountLocked, logTokenRotation, logRateLimitExceeded, logTokenReuse
    - Sử dụng @Async để không block main thread
    - _Requirements: 5.1, 5.2, 5.3, 5.4_
  - [ ]* 2.2 Write unit tests cho SecurityAuditService
    - Test log creation với các event types khác nhau
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [x] 3. Password Policy Validator
  - [x] 3.1 Tạo @ValidPassword annotation và PasswordPolicyValidator
    - Implement ConstraintValidator với regex patterns
    - Return Vietnamese error messages
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_
  - [x] 3.2 Apply @ValidPassword vào RegisterRequest và ChangePasswordRequest
    - Update DTOs với annotation
    - _Requirements: 4.7_
  - [ ]* 3.3 Write property test cho Password Policy
    - **Property 8: Password Policy Validation**
    - Test với random password strings
    - **Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7**

- [x] 4. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [-] 5. Account Lockout Service
  - [x] 5.1 Tạo AccountLockoutService interface và implementation
    - Implement recordFailedAttempt, isAccountLocked, getLockoutInfo, resetFailedAttempts
    - Sử dụng pessimistic locking để tránh race condition
    - _Requirements: 3.1, 3.2, 3.3, 3.4_
  - [x] 5.2 Tạo LockoutInfo record
    - Fields: isLocked, failedAttempts, lockedUntil, remainingSeconds
    - _Requirements: 3.2_
  - [x] 5.3 Integrate AccountLockoutService vào AuthService.login()
    - Check lockout trước khi validate password
    - Record failed attempt khi login fail
    - Reset attempts khi login success
    - Send email notification khi account locked
    - _Requirements: 3.1, 3.4, 3.5_
  - [ ]* 5.4 Write property tests cho Account Lockout
    - **Property 5: Account Lockout After Failed Attempts**
    - **Property 6: Account Unlock After Duration**
    - **Property 7: Successful Login Resets Failed Attempts**
    - **Validates: Requirements 3.1, 3.2, 3.3, 3.4**

- [x] 6. Token Rotation Service
  - [x] 6.1 Update TokenService để support rotation
    - Thêm logic mark token as used khi rotate
    - Detect token reuse và revoke all tokens
    - _Requirements: 2.1, 2.2, 2.3_
  - [x] 6.2 Tạo TokenReusedException
    - Custom exception với userId
    - _Requirements: 2.3_
  - [x] 6.3 Update AuthService.refresh() để handle token reuse
    - Catch TokenReusedException và return 401
    - Log security event
    - _Requirements: 2.3_
  - [ ]* 6.4 Write property tests cho Token Rotation
    - **Property 3: Refresh Token Rotation Invalidates Old Token**
    - **Property 4: Token Reuse Detection Revokes All Tokens**
    - **Validates: Requirements 2.1, 2.2, 2.3**

- [ ] 7. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Rate Limiting
  - [x] 8.1 Thêm Bucket4j dependency vào pom.xml
    - Sử dụng in-memory implementation (không cần dependency bên ngoài)
    - _Requirements: 1.1_
  - [x] 8.2 Tạo RateLimitService interface và implementation
    - Implement isAllowed, getRateLimitInfo
    - Configure different limits per endpoint pattern
    - Use ConcurrentHashMap for IP-based buckets
    - _Requirements: 1.1, 1.2, 1.3_
  - [x] 8.3 Tạo RateLimitInfo record
    - Fields: remaining, limit, resetTimestamp, retryAfterSeconds
    - _Requirements: 1.4_
  - [x] 8.4 Tạo RateLimitFilter
    - Extend OncePerRequestFilter
    - Check rate limit trước khi process request
    - Return 429 với proper headers và JSON response
    - Log rate limit exceeded events
    - _Requirements: 1.4, 1.5_
  - [x] 8.5 Register RateLimitFilter trong SecurityConfig
    - Filter tự động register qua @Component + @Order(HIGHEST_PRECEDENCE)
    - _Requirements: 1.1_
  - [x] 8.6 Tạo rate limit configuration trong application.properties
    - Configure limits cho login, register, password-reset endpoints
    - _Requirements: 1.1, 1.2, 1.3_
  - [ ]* 8.7 Write property tests cho Rate Limiting
    - **Property 1: Rate Limit Enforcement**
    - **Property 2: Rate Limit Response Format**
    - **Validates: Requirements 1.1, 1.2, 1.3, 1.4**

- [x] 9. Error Response Handlers
  - [x] 9.1 Tạo RateLimitExceededException
    - Custom exception với RateLimitInfo
    - Message tiếng Việt: "Quá nhiều yêu cầu. Vui lòng thử lại sau."
    - _Requirements: 1.4_
  - [x] 9.2 Tạo AccountLockedException
    - Custom exception với LockoutInfo (đã tạo trước đó)
    - Message tiếng Việt: "Tài khoản đã bị khóa tạm thời do đăng nhập sai nhiều lần..."
    - _Requirements: 3.2_
  - [x] 9.3 Update GlobalExceptionHandler
    - Handle RateLimitExceededException → 429
    - Handle AccountLockedException → 423
    - Handle TokenReusedException → 401
    - Return Vietnamese error messages trong field `error`
    - English code trong field `code` (cho frontend xử lý logic)
    - _Requirements: 1.4, 3.2, 2.3_

- [x] 10. Final Checkpoint - Ensure all tests pass
  - All security features implemented
  - Verify all security features work together
  - Test full login flow với rate limiting, lockout, và token rotation

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases
- Sử dụng jqwik library cho property-based testing trong Java
