# Design Document: Auth Security Improvements

## Overview

Tài liệu này mô tả thiết kế kỹ thuật cho việc cải thiện bảo mật Module Authentication của hệ thống Pháp Luật Số. Thiết kế bao gồm 5 tính năng chính: Rate Limiting, Refresh Token Rotation, Account Lockout, Password Policy, và Security Logging.

### Technology Stack
- **Framework**: Spring Boot 4.0, Java 17
- **Rate Limiting**: Bucket4j (in-memory, có thể mở rộng Redis)
- **Database**: PostgreSQL (đã có)
- **Security**: Spring Security (đã có)

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Request                            │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    RateLimitFilter                               │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Check IP-based rate limit using Bucket4j               │   │
│  │  If exceeded → Return 429 + log event                   │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    JwtAuthFilter (existing)                      │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    AuthController                                │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  /login → AccountLockoutService → AuthService           │   │
│  │  /register → PasswordPolicyValidator → AuthService      │   │
│  │  /refresh → TokenRotationService                        │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SecurityAuditService                          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Log all security events to security_audit_log table    │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. RateLimitService

```java
public interface RateLimitService {
    /**
     * Check if request is allowed based on IP and endpoint
     * @return true if allowed, false if rate limited
     */
    boolean isAllowed(String ipAddress, String endpoint);
    
    /**
     * Get remaining requests for IP/endpoint combination
     */
    RateLimitInfo getRateLimitInfo(String ipAddress, String endpoint);
}

public record RateLimitInfo(
    int remaining,
    int limit,
    long resetTimestamp,
    long retryAfterSeconds
) {}
```

### 2. RateLimitFilter

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {
    
    private final RateLimitService rateLimitService;
    private final SecurityAuditService auditService;
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String ip = getClientIP(request);
        String endpoint = request.getRequestURI();
        
        if (isRateLimitedEndpoint(endpoint)) {
            if (!rateLimitService.isAllowed(ip, endpoint)) {
                RateLimitInfo info = rateLimitService.getRateLimitInfo(ip, endpoint);
                auditService.logRateLimitExceeded(ip, endpoint);
                sendRateLimitResponse(response, info);
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

### 3. AccountLockoutService

```java
public interface AccountLockoutService {
    /**
     * Record a failed login attempt
     * @return true if account is now locked
     */
    boolean recordFailedAttempt(Long userId);
    
    /**
     * Check if account is currently locked
     */
    boolean isAccountLocked(Long userId);
    
    /**
     * Get lockout info for user
     */
    LockoutInfo getLockoutInfo(Long userId);
    
    /**
     * Reset failed attempts on successful login
     */
    void resetFailedAttempts(Long userId);
}

public record LockoutInfo(
    boolean isLocked,
    int failedAttempts,
    LocalDateTime lockedUntil,
    long remainingSeconds
) {}
```

### 4. TokenRotationService

```java
public interface TokenRotationService {
    /**
     * Issue new refresh token for user
     */
    RefreshTokenResult issueRefreshToken(User user);
    
    /**
     * Validate and rotate refresh token
     * @throws TokenReusedException if token was already used (security breach)
     */
    RefreshTokenResult rotateToken(String oldToken) throws TokenReusedException;
    
    /**
     * Revoke all tokens for user (security breach response)
     */
    void revokeAllUserTokens(Long userId);
}

public record RefreshTokenResult(
    String token,
    LocalDateTime expiresAt
) {}

public class TokenReusedException extends RuntimeException {
    private final Long userId;
    // constructor, getters
}
```

### 5. PasswordPolicyValidator

```java
@Component
public class PasswordPolicyValidator implements ConstraintValidator<ValidPassword, String> {
    
    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[!@#$%^&*]");
    
    public List<String> validate(String password) {
        List<String> errors = new ArrayList<>();
        
        if (password.length() < MIN_LENGTH) {
            errors.add("Mật khẩu phải có ít nhất 8 ký tự");
        }
        if (!UPPERCASE.matcher(password).find()) {
            errors.add("Mật khẩu phải có ít nhất 1 chữ hoa");
        }
        if (!LOWERCASE.matcher(password).find()) {
            errors.add("Mật khẩu phải có ít nhất 1 chữ thường");
        }
        if (!DIGIT.matcher(password).find()) {
            errors.add("Mật khẩu phải có ít nhất 1 chữ số");
        }
        if (!SPECIAL.matcher(password).find()) {
            errors.add("Mật khẩu phải có ít nhất 1 ký tự đặc biệt (!@#$%^&*)");
        }
        
        return errors;
    }
}
```

### 6. SecurityAuditService

```java
public interface SecurityAuditService {
    void logLoginAttempt(String email, String ip, String userAgent, boolean success);
    void logAccountLocked(Long userId, String reason, int duration);
    void logTokenRotation(Long userId);
    void logRateLimitExceeded(String ip, String endpoint);
    void logTokenReuse(Long userId, String tokenId);
}
```

## Data Models

### 1. User Entity Updates

```java
@Entity
@Table(name = "users")
public class User {
    // ... existing fields ...
    
    @Column(name = "failed_login_attempts")
    private int failedLoginAttempts = 0;
    
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;
    
    // getters, setters
}
```

### 2. RefreshToken Entity Updates

```java
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    
    @Column(name = "used_at")
    private LocalDateTime usedAt; // For detecting reuse
    
    @Column(name = "replaced_by_token_id")
    private Long replacedByTokenId; // Chain tracking
}
```

### 3. SecurityAuditLog Entity (New)

```java
@Entity
@Table(name = "security_audit_log")
public class SecurityAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "event_type", nullable = false)
    private String eventType; // LOGIN_SUCCESS, LOGIN_FAILED, ACCOUNT_LOCKED, etc.
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(name = "endpoint")
    private String endpoint;
    
    @Column(name = "details")
    private String details; // JSON for additional info
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
```

### 4. Database Migration (V3__auth_security.sql)

```sql
-- Add lockout fields to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_login_attempts INT DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP;

-- Add token rotation fields to refresh_tokens
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS used_at TIMESTAMP;
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS replaced_by_token_id BIGINT;

-- Create security audit log table
CREATE TABLE IF NOT EXISTS security_audit_log (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    user_id BIGINT,
    email VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    endpoint VARCHAR(255),
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes for security audit log
CREATE INDEX ix_security_audit_event_type ON security_audit_log(event_type);
CREATE INDEX ix_security_audit_user_id ON security_audit_log(user_id);
CREATE INDEX ix_security_audit_ip ON security_audit_log(ip_address);
CREATE INDEX ix_security_audit_created_at ON security_audit_log(created_at DESC);
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Rate Limit Enforcement

*For any* IP address and rate-limited endpoint, if the number of requests within the time window exceeds the configured limit, all subsequent requests SHALL be rejected with HTTP 429 until the window resets.

**Validates: Requirements 1.1, 1.2, 1.3**

### Property 2: Rate Limit Response Format

*For any* rate-limited request that is rejected, the response SHALL contain JSON with "error" field set to "RATE_LIMIT_EXCEEDED", "retryAfter" field with seconds, and HTTP header "Retry-After".

**Validates: Requirements 1.4**

### Property 3: Refresh Token Rotation Invalidates Old Token

*For any* valid refresh token, after calling the refresh endpoint, the old token SHALL be invalidated and using it again SHALL return HTTP 401.

**Validates: Requirements 2.1, 2.2**

### Property 4: Token Reuse Detection Revokes All Tokens

*For any* user with multiple refresh tokens, if a token reuse is detected (using an already-rotated token), ALL refresh tokens for that user SHALL be revoked.

**Validates: Requirements 2.3**

### Property 5: Account Lockout After Failed Attempts

*For any* user account, after 5 consecutive failed login attempts, the account SHALL be locked and subsequent login attempts SHALL return HTTP 423 with lockout duration.

**Validates: Requirements 3.1, 3.2**

### Property 6: Account Unlock After Duration

*For any* locked account, after the lockout duration expires, the account SHALL be automatically unlocked and login SHALL be allowed.

**Validates: Requirements 3.3**

### Property 7: Successful Login Resets Failed Attempts

*For any* user with failed login attempts, after a successful login, the failed attempt counter SHALL be reset to zero.

**Validates: Requirements 3.4**

### Property 8: Password Policy Validation

*For any* password submitted during registration or password change, if it does not meet ALL policy requirements (8+ chars, uppercase, lowercase, digit, special char), the request SHALL be rejected with HTTP 400 and specific Vietnamese error messages.

**Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7**

### Property 9: Security Event Logging

*For any* security event (login attempt, account lockout, token rotation, rate limit exceeded), a corresponding entry SHALL be created in security_audit_log with required fields.

**Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5**

## Error Handling

### Rate Limit Exceeded Response

```json
{
    "error": "RATE_LIMIT_EXCEEDED",
    "message": "Quá nhiều yêu cầu. Vui lòng thử lại sau.",
    "retryAfter": 45,
    "limit": 5,
    "remaining": 0
}
```
HTTP Status: 429 Too Many Requests
Headers: `Retry-After: 45`

### Account Locked Response

```json
{
    "error": "ACCOUNT_LOCKED",
    "message": "Tài khoản đã bị khóa tạm thời do đăng nhập sai nhiều lần.",
    "lockedUntil": "2026-01-11T15:30:00",
    "remainingSeconds": 845
}
```
HTTP Status: 423 Locked

### Password Policy Violation Response

```json
{
    "error": "PASSWORD_POLICY_VIOLATION",
    "message": "Mật khẩu không đáp ứng yêu cầu bảo mật",
    "violations": [
        "Mật khẩu phải có ít nhất 8 ký tự",
        "Mật khẩu phải có ít nhất 1 chữ hoa"
    ]
}
```
HTTP Status: 400 Bad Request

### Token Reuse Detected Response

```json
{
    "error": "TOKEN_REUSE_DETECTED",
    "message": "Phát hiện sử dụng lại token. Tất cả phiên đăng nhập đã bị hủy vì lý do bảo mật."
}
```
HTTP Status: 401 Unauthorized

## Testing Strategy

### Unit Tests
- Test PasswordPolicyValidator với các password hợp lệ và không hợp lệ
- Test AccountLockoutService logic (increment, reset, check locked)
- Test TokenRotationService (issue, rotate, revoke)
- Test RateLimitService bucket logic

### Property-Based Tests (using jqwik)
- Property 1: Rate limit enforcement với random IPs và request counts
- Property 3: Token rotation với random users và tokens
- Property 5: Account lockout với random failed attempt sequences
- Property 8: Password validation với random password strings

### Integration Tests
- Full login flow với rate limiting
- Token refresh với rotation
- Account lockout và unlock flow
- Security audit log verification

### Test Configuration
- Minimum 100 iterations per property test
- Use jqwik library for property-based testing in Java
