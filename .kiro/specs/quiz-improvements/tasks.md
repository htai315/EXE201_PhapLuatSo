# Implementation Plan: Quiz Improvements

## Overview

Kế hoạch triển khai cải thiện Module Quiz, bao gồm Redis session storage, duration validation, và configurable session timeout. Sử dụng Java 17, Spring Boot 4.0, Spring Data Redis.

## Tasks

- [x] 1. Thêm Redis Dependencies và Configuration
  - [x] 1.1 Thêm spring-boot-starter-data-redis vào pom.xml
    - Thêm dependency cho Spring Data Redis
    - _Requirements: 1.1_
  - [x] 1.2 Tạo Redis configuration trong application.properties
    - Thêm spring.data.redis.host, port, password, timeout
    - Thêm app.quiz.session-timeout-hours với default 2
    - _Requirements: 1.1, 3.1, 3.2_

- [x] 2. Tạo ExamSessionData và Serialization
  - [x] 2.1 Tạo ExamSessionData record
    - Fields: correctKeyMapping, shuffledOptionsMapping, startedAt, createdAt
    - Implement Serializable
    - Thêm method isExpired(Duration timeout)
    - _Requirements: 1.6_
  - [x] 2.2 Tạo ExamOptionDto record (nếu chưa có)
    - Fields: key, text
    - Implement Serializable
    - _Requirements: 1.6_
  - [ ]* 2.3 Write property test cho Session Round-Trip
    - **Property 1: Session Storage Round-Trip**
    - **Validates: Requirements 1.1, 1.2, 1.6**

- [x] 3. Tạo ExamSessionStore Interface và Implementations
  - [x] 3.1 Tạo ExamSessionStore interface
    - Methods: save, get, delete, isAvailable
    - _Requirements: 1.1, 1.2_
  - [x] 3.2 Tạo InMemoryExamSessionStore
    - Implement ExamSessionStore với ConcurrentHashMap
    - Thêm cleanupExpired method
    - _Requirements: 1.4_
  - [x] 3.3 Tạo RedisExamSessionStore
    - Implement ExamSessionStore với RedisTemplate
    - Sử dụng JSON serialization
    - Key prefix: "exam:session:"
    - _Requirements: 1.1, 1.2, 1.6, 1.7_
  - [ ]* 3.4 Write property test cho Session Key Format
    - **Property 2: Session Key Format**
    - **Validates: Requirements 1.7**

- [x] 4. Tạo ExamSessionStoreManager
  - [x] 4.1 Tạo ExamSessionStoreManager component
    - Inject RedisExamSessionStore và InMemoryExamSessionStore
    - Implement fallback logic khi Redis unavailable
    - Read session timeout từ config
    - _Requirements: 1.4, 3.1, 3.2, 3.3_
  - [x] 4.2 Thêm session timeout validation
    - Clamp timeout to range [0.5, 4] hours
    - _Requirements: 3.3_
  - [ ]* 4.3 Write property test cho Session Timeout Range
    - **Property 5: Session Timeout Configuration Range**
    - **Validates: Requirements 3.3**

- [x] 5. Checkpoint - Ensure session store components work
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Tạo QuizDurationValidator
  - [x] 6.1 Tạo QuizDurationValidator utility class
    - Constants: MIN_DURATION_MINUTES=5, MAX_DURATION_MINUTES=180, DEFAULT=45
    - Method: validateAndGetDuration(Integer duration)
    - Throw BadRequestException với message tiếng Việt
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_
  - [ ]* 6.2 Write property tests cho Duration Validation
    - **Property 3: Duration Validation - Invalid Range Rejection**
    - **Property 4: Duration Validation - Valid Range Acceptance**
    - **Validates: Requirements 2.1, 2.2, 2.3**

- [x] 7. Integrate vào QuizService
  - [x] 7.1 Update QuizService.createQuizSet()
    - Sử dụng QuizDurationValidator.validateAndGetDuration()
    - _Requirements: 2.1, 2.2, 2.4_
  - [x] 7.2 Thêm updateQuizSet method (nếu chưa có)
    - Validate duration khi update
    - _Requirements: 2.3_

- [x] 8. Integrate vào QuizExamService
  - [x] 8.1 Inject ExamSessionStoreManager vào QuizExamService
    - Thay thế ConcurrentHashMap bằng ExamSessionStoreManager
    - _Requirements: 1.1, 1.2_
  - [x] 8.2 Update startExam() method
    - Sử dụng ExamSessionStoreManager.save()
    - _Requirements: 1.1_
  - [x] 8.3 Update submitExam() method
    - Sử dụng ExamSessionStoreManager.get() và delete()
    - Update error message cho session expired: "Phiên thi đã hết hạn"
    - _Requirements: 1.2, 3.4_
  - [x] 8.4 Update cleanupExpiredExamSessions() scheduled task
    - Chỉ cleanup in-memory fallback store
    - Redis tự động cleanup qua TTL
    - _Requirements: 3.5_

- [x] 9. Checkpoint - Ensure integration works
  - Ensure all tests pass, ask the user if questions arise.

- [x] 10. Final Testing và Cleanup
  - [x] 10.1 Remove old ExamSession inner class
    - Đã được thay thế bởi ExamSessionData
    - _Requirements: 1.6_
  - [x] 10.2 Remove hardcoded SESSION_TIMEOUT_MS constant
    - Đã được thay thế bởi configurable timeout
    - _Requirements: 3.1_
  - [ ]* 10.3 Write integration tests
    - Test full exam flow với Redis
    - Test fallback khi Redis unavailable
    - _Requirements: 1.1, 1.2, 1.4_

- [x] 11. Final Checkpoint
  - Ensure all tests pass
  - Verify Redis integration works
  - Verify duration validation works
  - Verify session timeout configuration works

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases
- Sử dụng jqwik library cho property-based testing trong Java
- Redis là optional - hệ thống vẫn hoạt động với in-memory fallback

