# Requirements Document

## Introduction

Tài liệu yêu cầu cho việc cải thiện Module Quiz của hệ thống Pháp Luật Số. Các cải thiện tập trung vào scalability (Redis session), validation (duration), và configuration (timeout).

## Glossary

- **Quiz_System**: Hệ thống quản lý bộ đề quiz và câu hỏi
- **Exam_Session_Manager**: Component quản lý phiên thi của user (QuizExamService)
- **Quiz_Validator**: Component validate dữ liệu quiz
- **Redis_Session_Store**: Redis-based session storage cho exam sessions
- **ExamSession**: Object chứa thông tin phiên thi (correct answers, shuffled options, start time)

## Requirements

### Requirement 1: Distributed Exam Session với Redis

**User Story:** As a system administrator, I want exam sessions to be stored in Redis, so that the system can scale horizontally across multiple instances.

#### Acceptance Criteria

1.1. WHEN an exam session is started, THE Exam_Session_Manager SHALL store the session data in Redis with a configurable TTL
1.2. WHEN an exam session is retrieved, THE Exam_Session_Manager SHALL fetch the session from Redis
1.3. WHEN an exam session expires, THE Redis_Session_Store SHALL automatically remove the session data via Redis TTL
1.4. WHEN Redis is unavailable, THE Exam_Session_Manager SHALL fallback to in-memory storage and log a warning
1.5. WHEN the application restarts, THE Exam_Session_Manager SHALL recover active sessions from Redis automatically
1.6. THE Exam_Session_Manager SHALL use JSON serialization for session data stored in Redis
1.7. THE Exam_Session_Manager SHALL use session key format: "exam:session:{userId}:{quizSetId}"

### Requirement 2: Quiz Duration Validation

**User Story:** As a quiz creator, I want the system to validate quiz duration, so that I cannot set unreasonable time limits.

#### Acceptance Criteria

2.1. WHEN a quiz is created with duration less than 5 minutes, THE Quiz_Validator SHALL reject the request with error message "Thời gian làm bài phải từ 5 đến 180 phút"
2.2. WHEN a quiz is created with duration greater than 180 minutes, THE Quiz_Validator SHALL reject the request with error message "Thời gian làm bài phải từ 5 đến 180 phút"
2.3. WHEN a quiz is updated with invalid duration, THE Quiz_Validator SHALL reject the request with appropriate error message
2.4. WHEN duration is not provided, THE Quiz_System SHALL use default value of 45 minutes
2.5. WHEN duration is null, THE Quiz_Validator SHALL accept and use default value

### Requirement 3: Session Timeout Configuration

**User Story:** As a system administrator, I want to configure exam session timeout, so that I can balance between user experience and resource usage.

#### Acceptance Criteria

3.1. THE Exam_Session_Manager SHALL read session timeout from application.properties via key "app.quiz.session-timeout-hours"
3.2. WHEN session timeout is not configured, THE Exam_Session_Manager SHALL use default of 2 hours
3.3. THE Exam_Session_Manager SHALL support timeout values between 0.5 hours (30 minutes) and 4 hours
3.4. WHEN a session times out during exam, THE Quiz_System SHALL return error "Phiên thi đã hết hạn" when user tries to submit
3.5. THE scheduled cleanup task SHALL run every 10 minutes to remove expired sessions from in-memory fallback storage

