# 🏗️ System Architecture

Kiến trúc hệ thống Pháp Luật Số platform.

## 📐 Overall Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         Frontend                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │   HTML   │  │   CSS    │  │    JS    │  │Bootstrap │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ HTTP/REST
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      Spring Boot Backend                     │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Controllers Layer                        │  │
│  │  Auth │ Quiz │ AI │ Chat │ Payment │ Credit          │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Services Layer                           │  │
│  │  Business Logic & AI Integration                     │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Repository Layer                         │  │
│  │  JPA Repositories                                     │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│  SQL Server  │   │   OpenAI     │   │    VNPay     │
│   Database   │   │     API      │   │   Payment    │
└──────────────┘   └──────────────┘   └──────────────┘
```

---

## 🎯 Layer Architecture

### 1. Presentation Layer (Frontend)
**Location:** `src/main/resources/static/`

**Components:**
- **HTML Pages**: User interfaces
- **CSS Stylesheets**: Styling và responsive design
- **JavaScript**: Client-side logic, API calls, UI interactions
- **Bootstrap 5**: UI framework

**Key Features:**
- Single Page Application (SPA) style
- JWT token management
- Auto token refresh
- Error handling
- Toast notifications

### 2. Controller Layer
**Location:** `src/main/java/.../controller/`

**Responsibilities:**
- Handle HTTP requests
- Validate input
- Call service layer
- Return responses

**Controllers:**
```
AuthController          → /api/auth/*
QuizController          → /api/quiz-sets/*
AIQuizController        → /api/ai/quiz/*
ChatController          → /api/chat/*
ChatHistoryController   → /api/chat/sessions/*
PaymentController       → /api/payment/*
CreditController        → /api/credits/*
```

### 3. Service Layer
**Location:** `src/main/java/.../service/`

**Responsibilities:**
- Business logic
- Transaction management
- External API integration
- Data transformation

**Services:**
```
UserService             → User management
QuizService             → Quiz CRUD operations
AIQuizService           → AI quiz generation
LegalChatService        → AI chat with RAG
ChatHistoryService      → Chat history management
PaymentService          → Payment processing
VNPayService            → VNPay integration
CreditService           → Credit management
OpenAIService           → OpenAI API calls
DocumentParserService   → PDF/DOCX parsing
LegalSearchService      → Legal document search
```

### 4. Repository Layer
**Location:** `src/main/java/.../repo/`

**Responsibilities:**
- Database access
- CRUD operations
- Custom queries

**Repositories:**
```
UserRepo
QuizSetRepo
QuizQuestionRepo
QuizAttemptRepo
ChatSessionRepo
ChatMessageRepo
PaymentRepo
CreditTransactionRepo
LegalDocumentRepo
LegalArticleRepo
```

### 5. Entity Layer
**Location:** `src/main/java/.../entity/`

**Entities:**
```
User                    → Users table
QuizSet                 → Quiz sets
QuizQuestion            → Questions
QuizAttempt             → Exam attempts
ChatSession             → Chat sessions
ChatMessage             → Chat messages
Payment                 → Payments
CreditTransaction       → Credit transactions
LegalDocument           → Legal documents
LegalArticle            → Legal articles
```

---

## 🔐 Security Architecture

### Authentication Flow
```
1. User Login
   ↓
2. Validate Credentials
   ↓
3. Generate JWT Tokens
   - Access Token (24h)
   - Refresh Token (7 days)
   ↓
4. Return Tokens to Client
   ↓
5. Client Stores in localStorage
   ↓
6. Client Sends Access Token in Header
   ↓
7. Server Validates Token
   ↓
8. If Expired → Refresh Token
   ↓
9. Return New Access Token
```

### Security Components
- **JWT Filter**: Validate tokens on each request
- **Password Encoder**: BCrypt hashing
- **CORS Configuration**: Allow frontend origin
- **OAuth2**: Google login integration

---

## 🤖 AI Architecture (RAG)

### RAG (Retrieval-Augmented Generation) Flow
```
1. User Question
   ↓
2. Search Legal Documents
   - Vector similarity search
   - Keyword matching
   ↓
3. Retrieve Relevant Articles
   - Top 5 most relevant
   ↓
4. Build Context
   - Combine articles
   - Format for GPT
   ↓
5. Send to OpenAI GPT-4
   - Question + Context
   ↓
6. Generate Answer
   - With citations
   ↓
7. Return to User
```

### AI Components
```
OpenAIService
  ├── chat()              → Chat completion
  ├── generateQuiz()      → Quiz generation
  └── embedText()         → Text embedding

LegalSearchService
  ├── search()            → Search documents
  ├── vectorSearch()      → Similarity search
  └── keywordSearch()     → Keyword matching

DocumentParserService
  ├── parsePDF()          → Extract text from PDF
  └── parseDOCX()         → Extract text from DOCX
```

---

## 💳 Payment Architecture

### VNPay Integration Flow
```
1. User Selects Plan
   ↓
2. Create Payment Request
   - Plan code
   - Amount
   ↓
3. Generate VNPay URL
   - Sign with hash secret
   ↓
4. Redirect to VNPay
   ↓
5. User Pays
   ↓
6. VNPay Callback
   - Verify signature
   ↓
7. Update Payment Status
   ↓
8. Add Credits to User
   ↓
9. Redirect to Success Page
```

### Payment Components
```
PaymentService
  ├── createPayment()     → Create payment
  ├── processReturn()     → Handle VNPay return
  └── verifySignature()   → Verify VNPay signature

VNPayService
  ├── buildPaymentUrl()   → Build VNPay URL
  ├── generateHash()      → Generate signature
  └── verifyHash()        → Verify signature

VNPayUtil
  ├── hmacSHA512()        → HMAC SHA512
  └── sortParams()        → Sort parameters
```

---

## 💰 Credit System Architecture

### Credit Types
```
CHAT        → AI Chat (1 credit/message)
QUIZ_GEN    → AI Quiz Generation (1 credit/quiz)
```

### Credit Flow
```
1. User Action (Chat/Quiz Gen)
   ↓
2. Check Credit Balance
   ↓
3. If Insufficient → Return 402 Error
   ↓
4. Deduct Credit
   ↓
5. Log Transaction
   ↓
6. Perform Action
   ↓
7. Return Result
```

### Plans
```
FREE      → 0đ    → 0 credits
STUDENT   → 99k   → 100 credits (50 chat + 50 quiz)
PREMIUM   → 199k  → 300 credits (150 chat + 150 quiz)
```

---

## 📊 Database Schema

### Core Tables
```sql
users
  ├── id (PK)
  ├── email (UNIQUE)
  ├── password_hash
  ├── full_name
  ├── role
  ├── plan_code
  └── created_at

quiz_sets
  ├── id (PK)
  ├── user_id (FK)
  ├── title
  ├── description
  └── created_at

quiz_questions
  ├── id (PK)
  ├── quiz_set_id (FK)
  ├── question
  ├── option_a/b/c/d
  └── correct_answer

quiz_attempts
  ├── id (PK)
  ├── quiz_set_id (FK)
  ├── user_id (FK)
  ├── score_percent
  └── finished_at

chat_sessions
  ├── id (PK)
  ├── user_id (FK)
  ├── title
  └── created_at

chat_messages
  ├── id (PK)
  ├── session_id (FK)
  ├── role (user/assistant)
  ├── content
  └── created_at

payments
  ├── id (PK)
  ├── user_id (FK)
  ├── order_id
  ├── amount
  ├── status
  └── created_at

credit_transactions
  ├── id (PK)
  ├── user_id (FK)
  ├── credit_type
  ├── amount (+/-)
  └── created_at

legal_documents
  ├── id (PK)
  ├── name
  ├── type
  └── uploaded_at

legal_articles
  ├── id (PK)
  ├── document_id (FK)
  ├── article_number
  ├── content
  └── embedding (vector)
```

---

## 🔄 Request Flow Examples

### Example 1: AI Chat
```
1. POST /api/chat/sessions/messages
   Body: { "question": "Điều 1 Luật Dân Sự?" }
   Header: Authorization: Bearer <token>
   ↓
2. ChatController.sendMessage()
   ↓
3. CreditService.checkAndDeductChatCredit()
   - Check balance
   - Deduct 1 credit
   ↓
4. LegalSearchService.search()
   - Search relevant articles
   ↓
5. OpenAIService.chat()
   - Send to GPT-4 with context
   ↓
6. ChatHistoryService.saveMessage()
   - Save to database
   ↓
7. Return response with citations
```

### Example 2: AI Quiz Generation
```
1. POST /api/ai/quiz/generate-from-document
   Body: FormData (file, quizSetName, ...)
   Header: Authorization: Bearer <token>
   ↓
2. AIQuizController.generateQuiz()
   ↓
3. CreditService.checkAndDeductQuizGenCredit()
   - Check balance
   - Deduct 1 credit
   ↓
4. DocumentParserService.parse()
   - Extract text from PDF/DOCX
   ↓
5. OpenAIService.generateQuiz()
   - Send to GPT-4
   - Parse JSON response
   ↓
6. QuizService.createQuizSet()
   - Save quiz set
   - Save questions
   ↓
7. Return quiz data
```

### Example 3: Manual Quiz Creation
```
1. POST /api/quiz-sets
   Body: { "title": "...", "description": "..." }
   Header: Authorization: Bearer <token>
   ↓
2. QuizController.createQuizSet()
   ↓
3. QuizService.createQuizSet()
   - NO credit check (FREE)
   - Save to database
   ↓
4. Return quiz set
```

---

## 🚀 Deployment Architecture

### Development
```
localhost:8080
  ├── Spring Boot (embedded Tomcat)
  ├── SQL Server (local)
  └── .env file
```

### Production (Recommended)
```
Load Balancer
  ↓
Application Servers (multiple instances)
  ├── Spring Boot JAR
  ├── Environment variables
  └── Logging
  ↓
Database Cluster
  ├── Primary SQL Server
  └── Replica (read-only)
  ↓
External Services
  ├── OpenAI API
  └── VNPay Gateway
```

---

## 📈 Scalability Considerations

### Horizontal Scaling
- Stateless application (JWT)
- Multiple app instances behind load balancer
- Database connection pooling

### Caching Strategy
- Cache legal documents in memory
- Cache user credits
- Redis for session management (future)

### Performance Optimization
- Database indexing
- Lazy loading
- Pagination
- Async processing for AI calls

---

## 🔍 Monitoring & Logging

### Logging Levels
```
Production:
  - Root: WARN
  - Application: INFO
  - SQL: WARN

Development:
  - Root: INFO
  - Application: DEBUG
  - SQL: DEBUG
```

### Key Metrics to Monitor
- API response time
- Database query time
- OpenAI API latency
- Error rate
- Credit usage
- Payment success rate

---

## 🛡️ Security Best Practices

1. **Never expose sensitive data**
   - Use .env for secrets
   - Don't commit .env to git

2. **Validate all inputs**
   - Use @Valid annotations
   - Sanitize user input

3. **Use HTTPS in production**
   - SSL/TLS certificates
   - Secure cookies

4. **Rate limiting**
   - Prevent abuse
   - Protect AI APIs

5. **Regular security updates**
   - Update dependencies
   - Patch vulnerabilities

---

## 📚 Technology Stack Summary

| Layer | Technology |
|-------|-----------|
| Frontend | HTML5, CSS3, JavaScript, Bootstrap 5 |
| Backend | Spring Boot 3.x, Java 17 |
| Database | SQL Server 2019+ |
| Migration | Flyway |
| Security | Spring Security, JWT |
| AI | OpenAI GPT-4 |
| Payment | VNPay |
| Build | Maven |
| Logging | SLF4J + Logback |

---

## 🔮 Future Enhancements

1. **Redis Caching**
   - Cache frequently accessed data
   - Session management

2. **Elasticsearch**
   - Better legal document search
   - Full-text search

3. **WebSocket**
   - Real-time chat
   - Live notifications

4. **Microservices**
   - Separate AI service
   - Separate payment service

5. **Docker**
   - Containerization
   - Easy deployment

---

## 📞 Architecture Questions?

Nếu có câu hỏi về kiến trúc, vui lòng tạo issue trên GitHub.
