# 📝 Changelog

Lịch sử thay đổi của dự án Pháp Luật Số.

## [1.2.0] - 2026-01-11

### 🔧 Quiz Module Improvements

#### ✨ Features

**Distributed Exam Session với Redis**
- Chuyển exam session từ in-memory (ConcurrentHashMap) sang Redis
- Hỗ trợ horizontal scaling với multiple instances
- Tự động fallback sang in-memory khi Redis không khả dụng
- Session key format: `exam:session:{userId}_{quizSetId}`
- JSON serialization cho session data

**Quiz Duration Validation**
- Validate thời gian làm bài: 5-180 phút
- Default duration: 45 phút
- Error message tiếng Việt: "Thời gian làm bài phải từ 5 đến 180 phút"

**Session Timeout Configuration**
- Cấu hình timeout qua `app.quiz.session-timeout-hours`
- Default: 2 giờ
- Valid range: 0.5 - 4 giờ (tự động clamp)
- Scheduled cleanup mỗi 10 phút cho in-memory fallback

#### 🏗️ Architecture Changes

- Thêm `ExamSessionStore` interface với 2 implementations:
  - `RedisExamSessionStore` (primary)
  - `InMemoryExamSessionStore` (fallback)
- Thêm `ExamSessionStoreManager` để quản lý fallback logic
- Thêm `QuizDurationValidator` utility class
- Thêm `ExamSessionData` record cho serialization

#### 📦 Dependencies

- `spring-boot-starter-data-redis`
- `jackson-datatype-jsr310` (Java 8 Date/Time serialization)

#### ⚙️ Configuration

```properties
# Redis (optional)
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Quiz Session
app.quiz.session-timeout-hours=2
app.quiz.min-duration-minutes=5
app.quiz.max-duration-minutes=180
app.quiz.default-duration-minutes=45
```

---

## [1.1.0] - 2026-01-11

### 🔐 Auth Security Improvements

#### ✨ Features

**Rate Limiting**
- Login: 5 requests/60s per IP
- Register: 3 requests/60s per IP
- Password Reset: 3 requests/60s per IP
- Vietnamese error messages

**Refresh Token Rotation**
- Token rotation on refresh
- Reuse detection (invalidate all tokens on reuse)
- Token family tracking

**Account Lockout**
- Lock after 5 failed login attempts
- 15 minutes lockout duration
- Auto-unlock after duration

**Password Policy**
- Minimum 8 characters
- At least 1 uppercase, 1 lowercase, 1 digit, 1 special character
- Vietnamese validation messages

**Security Logging**
- Audit log for security events
- IP address tracking
- User agent logging

---

## [1.0.0] - 2024-12-31

### 🎉 Initial Release

#### ✨ Features

**Authentication & User Management**
- Email registration & login
- Google OAuth2 integration
- JWT authentication with auto-refresh
- User profile management
- Role-based access control (STUDENT, ADMIN)

**Quiz Management**
- Create quiz sets manually (FREE)
- Add/edit/delete questions
- Multiple choice questions (A/B/C/D)
- Take quiz and submit answers
- View quiz results with score
- Quiz history tracking
- Quiz pagination

**AI Features**
- AI Chat with legal documents (RAG)
- AI Quiz Generation from PDF/DOCX
- Citation tracking
- Chat history with search
- Context-aware responses

**Credit System**
- 3 plans: FREE, STUDENT, PREMIUM
- Credit types: CHAT, QUIZ_GEN
- Credit transaction logging
- Credit balance tracking
- Insufficient credit handling

**Payment Integration**
- VNPay Sandbox integration
- Payment creation
- Payment callback handling
- Signature verification
- Payment history

**UI/UX**
- Responsive design
- Bootstrap 5 framework
- Toast notifications
- Confirm modals
- Loading states
- Error handling
- Credits counter

#### 🔧 Technical

**Backend**
- Spring Boot 3.x
- Spring Security
- JWT authentication
- Flyway migrations
- PostgreSQL database
- OpenAI GPT-4 integration
- VNPay payment gateway

**Frontend**
- HTML5/CSS3/JavaScript
- Bootstrap 5
- Chart.js for statistics
- Fetch API with auto-refresh
- LocalStorage for tokens

**Security**
- Password hashing (BCrypt)
- JWT tokens (access + refresh)
- CORS configuration
- Input validation
- SQL injection prevention
- XSS protection
- Debug endpoints disabled in production

#### 📚 Documentation
- README.md - Project overview
- SETUP_GUIDE.md - Installation guide
- API_DOCUMENTATION.md - API reference
- ARCHITECTURE.md - System architecture
- DEVELOPMENT_GUIDE.md - Development guide
- DOCUMENTATION_INDEX.md - Documentation index

#### 🐛 Bug Fixes
- Fixed credit system logic (manual quiz creation is FREE)
- Fixed error message display (no JSON objects shown to users)
- Fixed response stream already read error
- Fixed token refresh mechanism
- Fixed VNPay signature verification

#### 🗑️ Cleanup
- Removed 50+ redundant documentation files
- Removed temporary SQL scripts
- Removed debug batch files
- Organized documentation structure

---

## [Unreleased]

### 🚀 Planned Features
- Redis caching
- Elasticsearch for better search
- WebSocket for real-time chat
- Email verification
- Password reset
- Admin dashboard
- Quiz sharing
- Quiz categories
- Leaderboard
- Achievements
- Mobile app

### 🔧 Technical Improvements
- Microservices architecture
- Docker containerization
- CI/CD pipeline
- Automated testing
- Performance monitoring
- Error tracking (Sentry)
- API rate limiting
- Database optimization

---

## Version Format

Format: `[MAJOR.MINOR.PATCH]`

- **MAJOR**: Breaking changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes (backward compatible)

---

## Commit Message Convention

```
feat: Add new feature
fix: Fix bug
docs: Update documentation
style: Code style changes
refactor: Code refactoring
test: Add tests
chore: Maintenance tasks
```

---

**Maintained By:** Development Team  
**Last Updated:** 2024-12-31
