# Design Document: Quiz Improvements

## Overview

Tài liệu thiết kế cho việc cải thiện Module Quiz, bao gồm:
1. Chuyển exam session từ in-memory (ConcurrentHashMap) sang Redis để hỗ trợ horizontal scaling
2. Thêm validation cho quiz duration (5-180 phút)
3. Cấu hình session timeout linh hoạt qua application.properties

## Architecture

### Current Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                    QuizExamService                          │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  ConcurrentHashMap<String, ExamSession>             │   │
│  │  - Key: "userId_quizSetId"                          │   │
│  │  - Value: ExamSession (correctKeyMapping, options)  │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  - SESSION_TIMEOUT_MS = 3 hours (hardcoded)                │
│  - Cleanup every 10 minutes                                 │
│  - Single instance only                                     │
└─────────────────────────────────────────────────────────────┘
```

### Target Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                    QuizExamService                          │
│  ┌─────────────────────────────────────────────────────┐   │
│  │           ExamSessionStore (Interface)              │   │
│  │  - save(sessionKey, session, ttl)                   │   │
│  │  - get(sessionKey): Optional<ExamSession>           │   │
│  │  - delete(sessionKey)                               │   │
│  └─────────────────────────────────────────────────────┘   │
│                          │                                  │
│            ┌─────────────┴─────────────┐                   │
│            ▼                           ▼                    │
│  ┌─────────────────┐         ┌─────────────────┐           │
│  │ RedisSessionStore│         │InMemorySessionStore│        │
│  │ (Primary)        │         │(Fallback)          │        │
│  └─────────────────┘         └─────────────────┘           │
│            │                                                │
│            ▼                                                │
│  ┌─────────────────┐                                       │
│  │     Redis       │                                       │
│  │  TTL-based      │                                       │
│  │  expiration     │                                       │
│  └─────────────────┘                                       │
└─────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. ExamSessionStore Interface

```java
public interface ExamSessionStore {
    /**
     * Save exam session with TTL
     */
    void save(String sessionKey, ExamSessionData session, Duration ttl);
    
    /**
     * Get exam session by key
     */
    Optional<ExamSessionData> get(String sessionKey);
    
    /**
     * Delete exam session
     */
    void delete(String sessionKey);
    
    /**
     * Check if store is available
     */
    boolean isAvailable();
}
```

### 2. ExamSessionData (Serializable DTO)

```java
public record ExamSessionData(
    Map<Long, String> correctKeyMapping,
    Map<Long, List<ExamOptionDto>> shuffledOptionsMapping,
    LocalDateTime startedAt,
    long createdAt
) implements Serializable {
    
    public boolean isExpired(Duration timeout) {
        return System.currentTimeMillis() - createdAt > timeout.toMillis();
    }
}
```

### 3. RedisExamSessionStore

```java
@Component
@Primary
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisExamSessionStore implements ExamSessionStore {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String KEY_PREFIX = "exam:session:";
    
    @Override
    public void save(String sessionKey, ExamSessionData session, Duration ttl) {
        String key = KEY_PREFIX + sessionKey;
        String json = objectMapper.writeValueAsString(session);
        redisTemplate.opsForValue().set(key, json, ttl);
    }
    
    @Override
    public Optional<ExamSessionData> get(String sessionKey) {
        String key = KEY_PREFIX + sessionKey;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) return Optional.empty();
        return Optional.of(objectMapper.readValue(json, ExamSessionData.class));
    }
    
    @Override
    public void delete(String sessionKey) {
        redisTemplate.delete(KEY_PREFIX + sessionKey);
    }
    
    @Override
    public boolean isAvailable() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

### 4. InMemoryExamSessionStore (Fallback)

```java
@Component
public class InMemoryExamSessionStore implements ExamSessionStore {
    
    private final ConcurrentHashMap<String, ExamSessionData> sessions = new ConcurrentHashMap<>();
    
    @Override
    public void save(String sessionKey, ExamSessionData session, Duration ttl) {
        sessions.put(sessionKey, session);
    }
    
    @Override
    public Optional<ExamSessionData> get(String sessionKey) {
        return Optional.ofNullable(sessions.get(sessionKey));
    }
    
    @Override
    public void delete(String sessionKey) {
        sessions.remove(sessionKey);
    }
    
    @Override
    public boolean isAvailable() {
        return true; // Always available
    }
    
    /**
     * Cleanup expired sessions (called by scheduled task)
     */
    public int cleanupExpired(Duration timeout) {
        int beforeSize = sessions.size();
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired(timeout));
        return beforeSize - sessions.size();
    }
}
```

### 5. ExamSessionStoreManager

```java
@Component
public class ExamSessionStoreManager {
    
    private final RedisExamSessionStore redisStore;
    private final InMemoryExamSessionStore inMemoryStore;
    
    @Value("${app.quiz.session-timeout-hours:2}")
    private double sessionTimeoutHours;
    
    public void save(String sessionKey, ExamSessionData session) {
        Duration ttl = Duration.ofMinutes((long)(sessionTimeoutHours * 60));
        
        if (redisStore != null && redisStore.isAvailable()) {
            redisStore.save(sessionKey, session, ttl);
        } else {
            log.warn("Redis unavailable, using in-memory fallback for session: {}", sessionKey);
            inMemoryStore.save(sessionKey, session, ttl);
        }
    }
    
    public Optional<ExamSessionData> get(String sessionKey) {
        if (redisStore != null && redisStore.isAvailable()) {
            return redisStore.get(sessionKey);
        }
        return inMemoryStore.get(sessionKey);
    }
    
    public void delete(String sessionKey) {
        if (redisStore != null && redisStore.isAvailable()) {
            redisStore.delete(sessionKey);
        }
        inMemoryStore.delete(sessionKey);
    }
    
    public Duration getSessionTimeout() {
        return Duration.ofMinutes((long)(sessionTimeoutHours * 60));
    }
}
```

### 6. Quiz Duration Validator

```java
public class QuizDurationValidator {
    
    public static final int MIN_DURATION_MINUTES = 5;
    public static final int MAX_DURATION_MINUTES = 180;
    public static final int DEFAULT_DURATION_MINUTES = 45;
    
    public static int validateAndGetDuration(Integer duration) {
        if (duration == null) {
            return DEFAULT_DURATION_MINUTES;
        }
        
        if (duration < MIN_DURATION_MINUTES || duration > MAX_DURATION_MINUTES) {
            throw new BadRequestException(
                "Thời gian làm bài phải từ " + MIN_DURATION_MINUTES + 
                " đến " + MAX_DURATION_MINUTES + " phút"
            );
        }
        
        return duration;
    }
}
```

## Data Models

### ExamSessionData JSON Structure (Redis)

```json
{
  "correctKeyMapping": {
    "123": "B",
    "124": "A",
    "125": "D"
  },
  "shuffledOptionsMapping": {
    "123": [
      {"key": "A", "text": "Option 1"},
      {"key": "B", "text": "Option 2"},
      {"key": "C", "text": "Option 3"},
      {"key": "D", "text": "Option 4"}
    ]
  },
  "startedAt": "2026-01-11T21:00:00",
  "createdAt": 1736600400000
}
```

### Redis Key Format

```
exam:session:{userId}:{quizSetId}
```

Example: `exam:session:42:15`

## Configuration

### application.properties

```properties
# Redis Configuration (optional - fallback to in-memory if not configured)
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=
spring.data.redis.timeout=5000ms

# Quiz Session Configuration
app.quiz.session-timeout-hours=2
# Valid range: 0.5 to 4 hours

# Quiz Duration Validation
app.quiz.min-duration-minutes=5
app.quiz.max-duration-minutes=180
app.quiz.default-duration-minutes=45
```



## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Session Storage Round-Trip

*For any* valid ExamSessionData object with any combination of correctKeyMapping and shuffledOptionsMapping, saving to the session store and then retrieving by the same key SHALL return an equivalent object.

**Validates: Requirements 1.1, 1.2, 1.6**

### Property 2: Session Key Format

*For any* userId (positive Long) and quizSetId (positive Long), the generated session key SHALL match the pattern "exam:session:{userId}:{quizSetId}".

**Validates: Requirements 1.7**

### Property 3: Duration Validation - Invalid Range Rejection

*For any* duration value that is less than 5 or greater than 180, the Quiz_Validator SHALL throw BadRequestException with message containing "5" and "180".

**Validates: Requirements 2.1, 2.2, 2.3**

### Property 4: Duration Validation - Valid Range Acceptance

*For any* duration value between 5 and 180 (inclusive), the Quiz_Validator SHALL accept and return the same value.

**Validates: Requirements 2.1, 2.2, 2.3**

### Property 5: Session Timeout Configuration Range

*For any* configured timeout value, if it is less than 0.5 hours or greater than 4 hours, the system SHALL clamp it to the valid range [0.5, 4] hours.

**Validates: Requirements 3.3**

## Error Handling

### Redis Unavailable

When Redis is unavailable:
1. Log warning: "Redis unavailable, using in-memory fallback for session: {sessionKey}"
2. Store session in InMemoryExamSessionStore
3. Continue normal operation
4. Retry Redis on next request

### Session Expired

When session is expired:
1. Return error: "Phiên thi đã hết hạn. Vui lòng bắt đầu lại bài thi."
2. HTTP Status: 400 Bad Request
3. Remove expired session from store

### Invalid Duration

When duration is invalid:
1. Return error: "Thời gian làm bài phải từ 5 đến 180 phút"
2. HTTP Status: 400 Bad Request
3. Do not create/update quiz

## Testing Strategy

### Unit Tests

1. **QuizDurationValidator Tests**
   - Test with duration = 4 (below min) → expect exception
   - Test with duration = 5 (min boundary) → expect success
   - Test with duration = 180 (max boundary) → expect success
   - Test with duration = 181 (above max) → expect exception
   - Test with duration = null → expect default 45

2. **ExamSessionStoreManager Tests**
   - Test save/get with Redis available
   - Test save/get with Redis unavailable (fallback)
   - Test delete operation
   - Test session timeout calculation

3. **ExamSessionData Serialization Tests**
   - Test JSON serialization/deserialization
   - Test with various correctKeyMapping sizes
   - Test with special characters in option text

### Property-Based Tests

Using jqwik library for property-based testing:

1. **Property 1: Session Round-Trip**
   - Generate random ExamSessionData
   - Save to store, retrieve, compare

2. **Property 3 & 4: Duration Validation**
   - Generate random integers
   - Verify validation behavior matches specification

### Integration Tests

1. **Redis Integration**
   - Test with real Redis instance
   - Test TTL expiration
   - Test concurrent access

2. **Full Exam Flow**
   - Start exam → verify session created
   - Submit exam → verify session deleted
   - Session timeout → verify error returned

