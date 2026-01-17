# 📋 Mô Tả Chi Tiết Dự Án: Pháp Luật Số - Legal AI Platform

> **Phiên bản:** 1.0  
> **Ngày tạo:** 16/01/2026  
> **Mục đích:** Tài liệu SRS (Software Requirements Specification) để thuê UI/UX Designer, QA/Tester, và Database Designer

---

## 📑 Mục Lục

1. [Tổng Quan Dự Án](#1-tổng-quan-dự-án)
2. [Đối Tượng Sử Dụng](#2-đối-tượng-sử-dụng)
3. [Tính Năng Chi Tiết](#3-tính-năng-chi-tiết)
4. [Yêu Cầu UI/UX Design](#4-yêu-cầu-uiux-design)
5. [Yêu Cầu Database Design](#5-yêu-cầu-database-design)
6. [Yêu Cầu Test Case](#6-yêu-cầu-test-case)
7. [Tech Stack](#7-tech-stack)
8. [Non-Functional Requirements](#8-non-functional-requirements)

---

## 1. Tổng Quan Dự Án

### 1.1 Giới Thiệu

**Pháp Luật Số** là một nền tảng web ứng dụng AI để hỗ trợ học tập, tra cứu và tư vấn pháp luật Việt Nam. Dự án kết hợp công nghệ AI tiên tiến (RAG - Retrieval-Augmented Generation) với cơ sở dữ liệu pháp luật để cung cấp thông tin chính xác, có trích dẫn nguồn.

### 1.2 Mục Tiêu Business

| # | Mục tiêu | Mô tả |
|---|----------|-------|
| 1 | **Democratize Legal Knowledge** | Giúp người dân tiếp cận kiến thức pháp luật dễ dàng hơn |
| 2 | **AI-Powered Learning** | Tự động tạo đề thi từ tài liệu pháp luật để hỗ trợ sinh viên luật |
| 3 | **Accurate Legal Consultation** | Tư vấn pháp luật có trích dẫn điều luật cụ thể |
| 4 | **Revenue Model** | Mô hình freemium với 3 gói dịch vụ |

### 1.3 Phạm Vi Dự Án

```
┌────────────────────────────────────────────────────────────────┐
│                      PHÁP LUẬT SỐ PLATFORM                      │
├────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │
│  │  AI Chat    │  │  AI Quiz    │  │    Quiz Management      │ │
│  │  Pháp Luật  │  │  Generator  │  │    (Manual + Exam)      │ │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘ │
├────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │
│  │   Credit    │  │   Payment   │  │    User & Auth          │ │
│  │   System    │  │   (PayOS)   │  │    (JWT + OAuth2)       │ │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘ │
├────────────────────────────────────────────────────────────────┤
│  ┌────────────────────────────────────────────────────────────┐│
│  │              ADMIN DASHBOARD (Analytics + Management)      ││
│  └────────────────────────────────────────────────────────────┘│
└────────────────────────────────────────────────────────────────┘
```

---

## 2. Đối Tượng Sử Dụng

### 2.1 User Personas

#### Persona 1: Sinh viên Luật (Primary)
| Thuộc tính | Giá trị |
|------------|---------|
| **Tên** | Minh - Sinh viên năm 3 Đại học Luật |
| **Tuổi** | 21 |
| **Mục tiêu** | Ôn thi các môn luật hiệu quả |
| **Pain Points** | Tài liệu nhiều, khó tổng hợp câu hỏi ôn tập |
| **Tính năng cần** | AI tạo đề thi từ giáo trình, làm bài và xem kết quả |

#### Persona 2: Người dân cần tư vấn (Secondary)
| Thuộc tính | Giá trị |
|------------|---------|
| **Tên** | Chị Lan - Nhân viên văn phòng |
| **Tuổi** | 35 |
| **Mục tiêu** | Tìm hiểu quyền lợi lao động |
| **Pain Points** | Không hiểu ngôn ngữ pháp lý, không biết hỏi ai |
| **Tính năng cần** | Chat với AI để được giải thích dễ hiểu + trích dẫn luật |

#### Persona 3: Doanh nghiệp (Future)
| Thuộc tính | Giá trị |
|------------|---------|
| **Tên** | Anh Hùng - Chủ doanh nghiệp nhỏ |
| **Tuổi** | 40 |
| **Mục tiêu** | Tra cứu nhanh các quy định liên quan đến kinh doanh |
| **Pain Points** | Không có bộ phận pháp lý, thuê luật sư đắt |
| **Tính năng cần** | Tra cứu nhanh, nhận alert khi có luật mới |

### 2.2 Vai Trò Trong Hệ Thống

| Vai trò | Mô tả | Quyền hạn |
|---------|-------|-----------|
| **GUEST** | Khách chưa đăng ký | Xem landing page, đăng ký, đăng nhập |
| **USER** | Người dùng đã đăng ký | Tất cả tính năng (theo gói credit) |
| **ADMIN** | Quản trị viên | Dashboard, quản lý user, xem thống kê, ban/unban |

---

## 3. Tính Năng Chi Tiết

### 3.1 Module: Authentication & User Management

#### 3.1.1 Đăng Ký (Register)

**User Story:**
> Là một khách, tôi muốn đăng ký tài khoản để sử dụng các tính năng của hệ thống.

**Flow:**
```
1. User điền form đăng ký
   └── Input: Email, Password, Confirm Password, Full Name
2. Validation client-side
   └── Check: Email format, password match, độ mạnh password
3. Submit → Backend validation
   └── Check: Email unique, password strength
4. Gửi email xác thực
   └── Link xác thực có token, hết hạn sau 24h
5. User click link → Kích hoạt tài khoản
6. Redirect đến trang đăng nhập
```

**Business Rules:**
- Email phải unique trong hệ thống
- Password tối thiểu 8 ký tự, có chữ hoa + chữ thường + số
- Email xác thực hết hạn sau 24 giờ
- User mới được tặng FREE credits (bonus)

---

#### 3.1.2 Đăng Nhập (Login)

**User Story:**
> Là một user đã đăng ký, tôi muốn đăng nhập để sử dụng hệ thống.

**Flow:**
```
1. User nhập email + password
2. Validation
   └── Check: Email exists, password match, email verified
3. Account lockout check
   └── Nếu bị khóa → Hiển thị thời gian còn lại
4. Tạo tokens
   └── Access Token (24h) + Refresh Token (7 ngày)
5. Redirect đến dashboard
6. Log security audit
```

**Business Rules:**
- Sai password 5 lần → Khóa tài khoản 15 phút
- Email chưa xác thực → Không cho đăng nhập
- Tài khoản bị ban → Từ chối và hiển thị lý do
- Refresh token rotation để bảo mật
- Phát hiện token reuse → Revoke tất cả tokens

---

#### 3.1.3 Google OAuth2 Login

**User Story:**
> Là một user, tôi muốn đăng nhập bằng Google để không phải nhớ password.

**Flow:**
```
1. Click "Đăng nhập với Google"
2. Redirect đến Google consent screen
3. User authorize
4. Google callback với authorization code
5. Backend exchange code → Get user info
6. Nếu email mới → Tạo tài khoản + tặng FREE credits
7. Nếu email đã có → Đăng nhập
8. Tạo tokens, redirect dashboard
```

**Business Rules:**
- Account Google không cần xác thực email (đã verify bởi Google)
- Có thể link nhiều provider vào 1 account (future)

---

#### 3.1.4 Quên Mật Khẩu (Forgot Password)

**User Story:**
> Là một user quên password, tôi muốn reset password qua email.

**Flow:**
```
1. Nhập email
2. Check email exists
3. Gửi email với reset link (token hết hạn 1h)
4. User click link → Hiện form nhập password mới
5. Validate + Update password
6. Invalidate tất cả tokens cũ
7. Redirect login
```

---

#### 3.1.5 Profile Management

**User Story:**
> Là một user, tôi muốn xem và cập nhật thông tin cá nhân.

**Chức năng:**
- Xem thông tin: Email, Full Name, Avatar, Plan, Credits
- Cập nhật: Full Name, Avatar
- Đổi password (yêu cầu nhập password cũ)
- Xem lịch sử giao dịch credit

---

### 3.2 Module: AI Chat Pháp Luật

#### 3.2.1 Chat với AI

**User Story:**
> Là một user, tôi muốn hỏi AI về vấn đề pháp luật và nhận câu trả lời có trích dẫn điều luật.

**Flow:**
```
1. User nhập câu hỏi
2. Check credit balance (tối thiểu 1)
3. Reserve 1 credit (chưa trừ thực sự)
4. Tìm kiếm điều luật liên quan
   ├── Vector similarity search (pgvector)
   ├── Keyword matching
   └── AI Re-ranking (lọc kết quả thực sự liên quan)
5. Build context từ điều luật
6. Gửi đến OpenAI GPT-4o-mini
7. Nhận response + parse citations
8. Confirm credit (trừ thực sự)
9. Lưu vào chat history
10. Trả về cho user với citations
```

**Error Handling:**
- Nếu AI fail → Refund credit (user không mất tiền)
- Nếu không tìm thấy luật liên quan → Thông báo "Không tìm thấy thông tin"

**UI Requirements:**
- Chat interface giống ChatGPT
- Citations hiển thị dạng card clickable
- Typing indicator khi AI đang xử lý
- Copy button cho mỗi message
- Markdown rendering cho response

---

#### 3.2.2 Quản Lý Session Chat

**User Story:**
> Là một user, tôi muốn quản lý các cuộc hội thoại của mình.

**Chức năng:**
- Xem danh sách sessions (sidebar)
- Tạo session mới
- Đổi tên session
- Xóa session
- Tìm kiếm trong lịch sử chat (theo keyword)
- Auto-generate title từ câu hỏi đầu tiên

---

### 3.3 Module: AI Tạo Đề Thi

#### 3.3.1 Upload và Tạo Đề AI

**User Story:**
> Là một sinh viên luật, tôi muốn upload file PDF/DOCX giáo trình và AI tự động tạo đề thi trắc nghiệm.

**Flow:**
```
1. Upload file (PDF/DOCX, max 10MB)
2. Chọn số câu hỏi: 15, 20, 30, hoặc 40
3. Nhập tên đề thi
4. Check credit (tối thiểu 1)
5. Reserve credit
6. Parse document → Extract text
7. Nếu text dài → Chia thành batches (20 câu/batch)
8. Gửi đến AI với context để tránh trùng lặp
9. Parse response JSON → Validate format
10. Nếu thiếu câu → Retry (tối đa 3 lần)
11. Sanitize content (XSS prevention)
12. Lưu QuizSet + Questions
13. Confirm credit
14. Redirect đến trang quiz
```

**Business Rules:**
- File types: PDF, DOCX
- Max file size: 10MB
- Số câu cho phép: 15, 20, 30, 40
- Mỗi câu có 4 đáp án (A, B, C, D)
- Có đúng 1 đáp án đúng
- Tốn 1 credit/lần tạo

---

### 3.4 Module: Quản Lý Đề Thi

#### 3.4.1 Tạo Đề Thủ Công (MIỄN PHÍ)

**User Story:**
> Là một user, tôi muốn tự tạo đề thi trắc nghiệm mà không tốn credit.

**Flow:**
```
1. Nhập tên đề + mô tả (optional)
2. Tạo quiz set rỗng
3. Thêm từng câu hỏi:
   ├── Question text
   ├── 4 đáp án (A, B, C, D)
   └── Chọn đáp án đúng
4. Có thể sửa/xóa câu hỏi
```

**Business Rules:**
- KHÔNG tốn credit
- Phải có đúng 4 đáp án
- Keys phải là A, B, C, D
- Chỉ có 1 đáp án đúng

---

#### 3.4.2 Làm Bài Thi

**User Story:**
> Là một user, tôi muốn làm bài thi và xem kết quả.

**Flow:**
```
1. Chọn quiz set để làm
2. Hiển thị thông tin: Số câu, thời gian (5-180 phút)
3. Click "Bắt đầu"
4. Hiển thị từng câu hỏi
   ├── Navigation: Next/Previous
   ├── Đánh dấu câu để review
   └── Progress bar
5. Countdown timer
6. Nộp bài (manual hoặc auto khi hết giờ)
7. Tính điểm + hiển thị kết quả
8. Xem chi tiết: Câu đúng/sai + đáp án đúng
9. Lưu vào lịch sử
```

**UI Requirements:**
- Full-screen exam mode
- Clear question navigation
- Timer luôn visible
- Confirm dialog khi nộp bài
- Highlight câu đã làm/chưa làm
- Result summary: Score, time taken, comparison

---

#### 3.4.3 Xem Lịch Sử Làm Bài

**User Story:**
> Là một user, tôi muốn xem lại các bài thi đã làm.

**Hiển thị:**
- Danh sách attempts với: Tên đề, Điểm, Thời gian, Ngày làm
- Filter theo quiz set hoặc date range
- Xem chi tiết từng attempt

---

#### 3.4.4 Export PDF

**User Story:**
> Là một user, tôi muốn xuất đề thi ra file PDF để in.

**Yêu cầu PDF:**
- Header: Tên đề, ngày tạo
- Questions với đáp án (không đánh dấu đúng/sai)
- Có trang đáp án riêng ở cuối
- Format đẹp, dễ đọc

---

### 3.5 Module: Credit System

#### 3.5.1 Xem Credit Balance

**User Story:**
> Là một user, tôi muốn biết mình còn bao nhiêu credit.

**Hiển thị:**
- Total balance
- Credit by type: CHAT, QUIZ_GEN
- Lịch sử transactions

---

#### 3.5.2 Credit Operations

**Reserve/Confirm/Refund Pattern:**
```
RESERVE:  Balance -= Amount (pessimistic)
          Create CreditReservation (status: PENDING)
          
CONFIRM:  Reservation.status = CONFIRMED
          Log CreditTransaction (type: USAGE)
          
REFUND:   Balance += Amount (restore)
          Reservation.status = REFUNDED
          Log CreditTransaction (type: REFUND)
```

**Business Rules:**
- Credit không âm
- Reservation timeout: 5 phút → auto refund
- Optimistic locking với retry (concurrency)

---

### 3.6 Module: Payment (PayOS)

#### 3.6.1 Mua Credit

**User Story:**
> Là một user, tôi muốn mua thêm credit để sử dụng các tính năng AI.

**Gói Credit:**

| Gói | Giá | Credits | Credit Chat | Credit Quiz Gen |
|-----|-----|---------|-------------|-----------------|
| FREE | 0đ | 5 bonus | 3 | 2 |
| STUDENT | 99,000đ | 100 | 50 | 50 |
| PREMIUM | 199,000đ | 300 | 150 | 150 |

**Flow:**
```
1. User chọn gói
2. Check pending payment (tránh duplicate)
   ├── Nếu có pending cùng gói → Reuse link
   └── Nếu pending gói khác → Tạo mới
3. Tạo payment record (status: PENDING)
4. Tạo PayOS checkout URL
5. Redirect user đến PayOS
6. User thanh toán (QR/Banking)
7. PayOS webhook callback
   ├── Verify signature
   ├── Update payment status
   ├── Cộng credits cho user
   └── Gửi email xác nhận
8. Redirect về success page
```

**Error Handling:**
- Webhook retry nếu payment chưa commit
- Idempotency để tránh duplicate credit
- Expired payments cleanup (30 phút)

---

### 3.7 Module: Admin Dashboard

#### 3.7.1 Dashboard Overview

**Hiển thị:**
- Total users (new today, active)
- Total revenue (today, this month)
- AI usage stats (chat, quiz gen)
- Charts: User growth, Revenue trend

---

#### 3.7.2 User Management

**Chức năng:**
- List users với pagination + search
- Filter: By role, plan, status
- View user detail
- Ban/Unban user (with reason)
- Admin activity logging

---

#### 3.7.3 Payment Management

**Chức năng:**
- List payments
- Filter: By status, date range
- View payment detail
- Manual refund (future)

---

## 4. Yêu Cầu UI/UX Design

### 4.1 Design System

#### Color Palette
| Token | Hex | Usage |
|-------|-----|-------|
| Primary | #2563EB | Buttons, links, highlights |
| Secondary | #7C3AED | Accents |
| Success | #10B981 | Positive actions, correct answers |
| Warning | #F59E0B | Warnings, pending states |
| Error | #EF4444 | Errors, wrong answers |
| Neutral-900 | #111827 | Text primary |
| Neutral-50 | #F9FAFB | Background |

#### Typography
| Element | Font | Size | Weight |
|---------|------|------|--------|
| H1 | Inter | 36px | 700 |
| H2 | Inter | 28px | 600 |
| H3 | Inter | 20px | 600 |
| Body | Inter | 16px | 400 |
| Caption | Inter | 14px | 400 |

---

### 4.2 Wireframes Cần Thiết

#### Public Pages
1. **Landing Page** - Hero, features, pricing, testimonials
2. **Login Page** - Form + Google OAuth button
3. **Register Page** - Form + email verification notice
4. **Forgot Password** - Email input → Reset form

#### User Dashboard
5. **Dashboard Home** - Stats summary, quick actions
6. **AI Chat** - Sidebar sessions + Chat area + Citations
7. **Quiz List** - Grid/List view + actions
8. **Quiz Detail** - Info + questions list
9. **Create Quiz** - Step-by-step form
10. **Take Exam** - Full-screen exam interface
11. **Exam Result** - Score + review
12. **Profile** - Info + settings + transaction history
13. **Payment** - Plan selection + checkout flow

#### Admin Pages
14. **Admin Dashboard** - Charts + stats
15. **User List** - Table + search + actions
16. **Payment List** - Table + filters

---

### 4.3 UX Guidelines

#### Chat Experience
- Smooth typing animation
- Citation cards với hover effect
- Copy button appear on hover
- Loading skeleton khi fetch history
- Empty state với suggested questions

#### Exam Experience
- Distraction-free mode
- Clear progress visualization
- Accessible navigation
- Mobile-friendly answer selection
- Celebration animation khi hoàn thành

#### Error Handling
- Friendly error messages (không technical)
- Clear recovery actions
- Toast notifications cho transient errors
- Full-page errors cho critical failures

---

## 5. Yêu Cầu Database Design

### 5.1 Entity Relationship Diagram

```
┌─────────────────┐       ┌─────────────────┐
│      USERS      │───1:N─│   USER_CREDIT   │
└───────┬─────────┘       └─────────────────┘
        │1:N
        │                 ┌─────────────────┐
        ├────────────────→│  REFRESH_TOKEN  │
        │                 └─────────────────┘
        │
        ├───1:N──→┌─────────────────┐
        │         │   QUIZ_SETS     │───1:N──→┌─────────────────┐
        │         └─────────────────┘         │ QUIZ_QUESTIONS  │───1:N──→┌────────────────────────┐
        │                │                    └─────────────────┘         │ QUIZ_QUESTION_OPTIONS  │
        │                │1:N                                              └────────────────────────┘
        │                ↓
        │         ┌─────────────────┐
        ├─────────│  QUIZ_ATTEMPTS  │───1:N──→┌─────────────────┐
        │         └─────────────────┘         │ QUIZ_ANSWERS    │
        │                                      └─────────────────┘
        │
        ├───1:N──→┌─────────────────┐
        │         │  CHAT_SESSIONS  │───1:N──→┌─────────────────┐
        │         └─────────────────┘         │  CHAT_MESSAGES  │
        │                                      └─────────────────┘
        │
        ├───1:N──→┌─────────────────┐
        │         │    PAYMENTS     │
        │         └─────────────────┘
        │
        ├───1:N──→┌─────────────────────────┐
        │         │  CREDIT_TRANSACTIONS    │
        │         └─────────────────────────┘
        │
        └───1:N──→┌─────────────────────────┐
                  │   CREDIT_RESERVATIONS   │
                  └─────────────────────────┘

┌─────────────────┐
│ LEGAL_DOCUMENTS │───1:N──→┌─────────────────┐
└─────────────────┘         │ LEGAL_ARTICLES  │ (with vector embedding)
                            └─────────────────┘

┌─────────────────────────┐
│   SECURITY_AUDIT_LOG    │ (standalone)
└─────────────────────────┘

┌─────────────────────────┐
│   ADMIN_ACTIVITY_LOG    │ (standalone)
└─────────────────────────┘
```

---

### 5.2 Tables Schema

#### users
```sql
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) UNIQUE NOT NULL,
    password_hash   VARCHAR(255),          -- Null for OAuth users
    full_name       VARCHAR(255) NOT NULL,
    avatar_url      VARCHAR(500),
    role            VARCHAR(20) NOT NULL DEFAULT 'USER',  -- USER, ADMIN
    provider        VARCHAR(20) NOT NULL DEFAULT 'LOCAL', -- LOCAL, GOOGLE
    provider_id     VARCHAR(255),          -- Google ID
    plan_code       VARCHAR(20) NOT NULL DEFAULT 'FREE',
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    email_verification_token VARCHAR(100),
    email_verification_expiry TIMESTAMP,
    password_reset_token VARCHAR(100),
    password_reset_expiry TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_users_email ON users(email);
CREATE INDEX ix_users_is_active ON users(is_active);
```

#### user_credits
```sql
CREATE TABLE user_credits (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT UNIQUE NOT NULL REFERENCES users(id),
    chat_credits    INT NOT NULL DEFAULT 0 CHECK (chat_credits >= 0),
    quiz_gen_credits INT NOT NULL DEFAULT 0 CHECK (quiz_gen_credits >= 0),
    version         INT NOT NULL DEFAULT 0,  -- Optimistic locking
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
```

#### refresh_tokens
```sql
CREATE TABLE refresh_tokens (
    id              BIGSERIAL PRIMARY KEY,
    token_hash      VARCHAR(255) UNIQUE NOT NULL,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    is_used         BOOLEAN NOT NULL DEFAULT FALSE,
    is_revoked      BOOLEAN NOT NULL DEFAULT FALSE,
    parent_id       BIGINT REFERENCES refresh_tokens(id),  -- Token chain
    expires_at      TIMESTAMP NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
```

#### quiz_sets
```sql
CREATE TABLE quiz_sets (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    source_type     VARCHAR(20) NOT NULL DEFAULT 'MANUAL',  -- MANUAL, AI
    source_filename VARCHAR(255),
    duration_minutes INT CHECK (duration_minutes BETWEEN 5 AND 180),
    is_public       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_quiz_sets_user ON quiz_sets(user_id);
```

#### quiz_questions
```sql
CREATE TABLE quiz_questions (
    id              BIGSERIAL PRIMARY KEY,
    quiz_set_id     BIGINT NOT NULL REFERENCES quiz_sets(id) ON DELETE CASCADE,
    question        TEXT NOT NULL,
    order_index     INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
```

#### quiz_question_options
```sql
CREATE TABLE quiz_question_options (
    id              BIGSERIAL PRIMARY KEY,
    question_id     BIGINT NOT NULL REFERENCES quiz_questions(id) ON DELETE CASCADE,
    option_key      CHAR(1) NOT NULL CHECK (option_key IN ('A', 'B', 'C', 'D')),
    option_text     TEXT NOT NULL,
    is_correct      BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (question_id, option_key)
);

-- Trigger: Ensure exactly 1 correct answer per question
```

#### quiz_attempts
```sql
CREATE TABLE quiz_attempts (
    id              BIGSERIAL PRIMARY KEY,
    quiz_set_id     BIGINT NOT NULL REFERENCES quiz_sets(id),
    user_id         BIGINT NOT NULL REFERENCES users(id),
    score_percent   DECIMAL(5,2),
    total_questions INT NOT NULL,
    correct_answers INT NOT NULL DEFAULT 0,
    time_taken_seconds INT,
    status          VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS', -- IN_PROGRESS, COMPLETED, ABANDONED
    started_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    finished_at     TIMESTAMP
);

CREATE INDEX ix_attempts_user ON quiz_attempts(user_id);
```

#### quiz_answers
```sql
CREATE TABLE quiz_answers (
    id              BIGSERIAL PRIMARY KEY,
    attempt_id      BIGINT NOT NULL REFERENCES quiz_attempts(id) ON DELETE CASCADE,
    question_id     BIGINT NOT NULL REFERENCES quiz_questions(id),
    selected_key    CHAR(1) CHECK (selected_key IN ('A', 'B', 'C', 'D')),
    is_correct      BOOLEAN
);
```

#### chat_sessions
```sql
CREATE TABLE chat_sessions (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    title           VARCHAR(255),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_chat_sessions_user ON chat_sessions(user_id);
```

#### chat_messages
```sql
CREATE TABLE chat_messages (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL,  -- USER, ASSISTANT
    content         TEXT NOT NULL,
    citations       JSONB,  -- [{articleId, documentName, articleNumber, preview}]
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_chat_messages_session ON chat_messages(session_id);
```

#### payments
```sql
CREATE TABLE payments (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    order_code      BIGINT UNIQUE NOT NULL,
    plan_code       VARCHAR(20) NOT NULL,
    amount          DECIMAL(12,2) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, SUCCESS, FAILED, EXPIRED, CANCELLED
    checkout_url    VARCHAR(500),
    transaction_id  VARCHAR(100),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_payments_user ON payments(user_id);
CREATE INDEX ix_payments_status ON payments(status);
```

#### payment_idempotency_records
```sql
CREATE TABLE payment_idempotency_records (
    id              BIGSERIAL PRIMARY KEY,
    order_code      BIGINT UNIQUE NOT NULL,
    processed       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
```

#### credit_transactions
```sql
CREATE TABLE credit_transactions (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    credit_type     VARCHAR(20) NOT NULL,  -- CHAT, QUIZ_GEN
    type            VARCHAR(20) NOT NULL,  -- PURCHASE, USAGE, BONUS, REFUND
    amount          INT NOT NULL,  -- Positive or negative
    balance_after   INT NOT NULL,
    description     VARCHAR(255),
    reference_id    VARCHAR(100),          -- payment_id, quiz_set_id, etc.
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_trans_user_date ON credit_transactions(user_id, created_at DESC);
```

#### credit_reservations
```sql
CREATE TABLE credit_reservations (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    credit_type     VARCHAR(20) NOT NULL,
    amount          INT NOT NULL,
    operation_type  VARCHAR(50) NOT NULL,  -- AI_CHAT, AI_QUIZ_GEN
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, CONFIRMED, REFUNDED, EXPIRED
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Scheduler: Cleanup reservations > 5 minutes
```

#### legal_documents
```sql
CREATE TABLE legal_documents (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    document_type   VARCHAR(50) NOT NULL,  -- CONSTITUTION, LAW, DECREE, CIRCULAR
    issue_date      DATE,
    effective_date  DATE,
    description     TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
```

#### legal_articles
```sql
-- Requires pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE legal_articles (
    id              BIGSERIAL PRIMARY KEY,
    document_id     BIGINT NOT NULL REFERENCES legal_documents(id) ON DELETE CASCADE,
    article_number  VARCHAR(50) NOT NULL,
    title           VARCHAR(500),
    content         TEXT NOT NULL,
    content_preview VARCHAR(1000),         -- First 500 chars for display
    embedding       VECTOR(1536),          -- OpenAI ada-002 embedding
    keywords        TEXT[],
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- IVFFlat index for vector similarity search
CREATE INDEX ix_legal_articles_embedding ON legal_articles 
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
```

#### security_audit_log
```sql
CREATE TABLE security_audit_log (
    id              BIGSERIAL PRIMARY KEY,
    event_type      VARCHAR(50) NOT NULL,  -- LOGIN_SUCCESS, LOGIN_FAILED, TOKEN_REFRESH, PASSWORD_CHANGE, etc.
    user_id         BIGINT,
    email           VARCHAR(255),
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    endpoint        VARCHAR(255),
    details         TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_audit_event_date ON security_audit_log(event_type, created_at DESC);
```

#### admin_activity_log
```sql
CREATE TABLE admin_activity_log (
    id              BIGSERIAL PRIMARY KEY,
    admin_id        BIGINT NOT NULL REFERENCES users(id),
    action          VARCHAR(50) NOT NULL,  -- BAN_USER, UNBAN_USER, DELETE_QUIZ, etc.
    target_type     VARCHAR(50),           -- USER, QUIZ_SET, PAYMENT
    target_id       BIGINT,
    details         JSONB,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
```

---

### 5.3 Stored Functions & Triggers

#### Trigger: Auto-create credits for new users
```sql
CREATE FUNCTION give_free_credits_to_new_user() RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO user_credits (user_id, chat_credits, quiz_gen_credits)
    VALUES (NEW.id, 3, 2);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_give_free_credits
AFTER INSERT ON users FOR EACH ROW
EXECUTE FUNCTION give_free_credits_to_new_user();
```

#### Trigger: Ensure only 1 correct answer
```sql
CREATE FUNCTION check_only_one_correct_option() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_correct = TRUE THEN
        UPDATE quiz_question_options 
        SET is_correct = FALSE 
        WHERE question_id = NEW.question_id AND id != NEW.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_only_one_correct_option
AFTER INSERT OR UPDATE ON quiz_question_options
FOR EACH ROW WHEN (NEW.is_correct = TRUE)
EXECUTE FUNCTION check_only_one_correct_option();
```

#### Function: Hybrid Search
```sql
CREATE FUNCTION hybrid_search_articles(
    query_embedding VECTOR(1536),
    keywords TEXT[],
    vector_weight FLOAT DEFAULT 0.7,
    keyword_weight FLOAT DEFAULT 0.3,
    similarity_threshold FLOAT DEFAULT 0.7,
    result_limit INT DEFAULT 10
) RETURNS TABLE (
    id BIGINT,
    document_id BIGINT,
    article_number VARCHAR,
    title VARCHAR,
    content TEXT,
    similarity FLOAT,
    keyword_score FLOAT,
    combined_score FLOAT
) AS $$
BEGIN
    RETURN QUERY
    WITH vector_results AS (
        SELECT 
            la.id,
            la.document_id,
            la.article_number,
            la.title,
            la.content,
            1 - (la.embedding <=> query_embedding) AS sim
        FROM legal_articles la
        WHERE la.embedding IS NOT NULL
          AND 1 - (la.embedding <=> query_embedding) >= similarity_threshold
    ),
    keyword_results AS (
        SELECT 
            la.id,
            (SELECT COUNT(*) FROM unnest(keywords) k WHERE la.content ILIKE '%' || k || '%')::FLOAT 
            / GREATEST(array_length(keywords, 1), 1) AS kw_score
        FROM legal_articles la
    )
    SELECT 
        vr.id,
        vr.document_id,
        vr.article_number,
        vr.title,
        vr.content,
        vr.sim,
        COALESCE(kr.kw_score, 0),
        (vr.sim * vector_weight) + (COALESCE(kr.kw_score, 0) * keyword_weight) AS combined
    FROM vector_results vr
    LEFT JOIN keyword_results kr ON vr.id = kr.id
    ORDER BY combined DESC
    LIMIT result_limit;
END;
$$ LANGUAGE plpgsql;
```

---

### 5.4 Views

#### Admin Dashboard Stats
```sql
CREATE VIEW vw_admin_dashboard_stats AS
SELECT
    (SELECT COUNT(*) FROM users) AS total_users,
    (SELECT COUNT(*) FROM users WHERE created_at >= CURRENT_DATE) AS users_today,
    (SELECT COUNT(*) FROM users WHERE is_active = TRUE) AS active_users,
    (SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status = 'SUCCESS') AS total_revenue,
    (SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status = 'SUCCESS' AND created_at >= CURRENT_DATE) AS revenue_today,
    (SELECT COUNT(*) FROM chat_messages WHERE role = 'USER') AS total_chat_messages,
    (SELECT COUNT(*) FROM quiz_sets WHERE source_type = 'AI') AS total_ai_quizzes,
    (SELECT COUNT(*) FROM quiz_attempts) AS total_exam_attempts;
```

---

## 6. Yêu Cầu Test Case

### 6.1 Test Categories

| Category | Coverage | Priority |
|----------|----------|----------|
| Unit Tests | Services, Validators | High |
| Integration Tests | Controllers, Repositories | High |
| E2E Tests | User flows | Medium |
| Performance Tests | API response time, concurrent users | Medium |
| Security Tests | Authentication, authorization, injection | High |

---

### 6.2 Functional Test Cases

#### TC-AUTH-001: User Registration
| Field | Value |
|-------|-------|
| **Title** | Đăng ký user mới thành công |
| **Precondition** | Email chưa tồn tại trong hệ thống |
| **Steps** | 1. Điền email valid<br>2. Điền password đủ mạnh<br>3. Confirm password khớp<br>4. Điền full name<br>5. Submit |
| **Expected** | - Account tạo thành công<br>- Email xác thực được gửi<br>- Redirect đến trang confirm email |

#### TC-AUTH-002: Login với email/password
| Field | Value |
|-------|-------|
| **Title** | Đăng nhập thành công |
| **Precondition** | Account đã verified |
| **Steps** | 1. Nhập email<br>2. Nhập password đúng<br>3. Submit |
| **Expected** | - JWT tokens trả về<br>- Redirect dashboard<br>- Security audit logged |

#### TC-AUTH-003: Account Lockout
| Field | Value |
|-------|-------|
| **Title** | Khóa tài khoản sau 5 lần sai password |
| **Precondition** | Account active |
| **Steps** | 1. Nhập sai password 5 lần |
| **Expected** | - Account bị khóa<br>- Hiển thị thời gian còn lại<br>- Logged trong security audit |

#### TC-AUTH-004: Token Refresh
| Field | Value |
|-------|-------|
| **Title** | Refresh access token |
| **Precondition** | Access token hết hạn, refresh token còn valid |
| **Steps** | 1. API trả 401<br>2. Call refresh endpoint với refresh token |
| **Expected** | - Access token mới được cấp<br>- Refresh token mới (rotation)<br>- Token cũ bị invalidate |

---

#### TC-CHAT-001: Chat với AI
| Field | Value |
|-------|-------|
| **Title** | Gửi câu hỏi pháp luật |
| **Precondition** | User có ≥ 1 credit CHAT |
| **Steps** | 1. Tạo session mới<br>2. Nhập câu hỏi<br>3. Submit |
| **Expected** | - Credit bị trừ 1<br>- Nhận response với citations<br>- Message lưu vào history |

#### TC-CHAT-002: Không đủ credit
| Field | Value |
|-------|-------|
| **Title** | Chat khi hết credit |
| **Precondition** | User có 0 credit CHAT |
| **Steps** | 1. Nhập câu hỏi<br>2. Submit |
| **Expected** | - HTTP 402 Payment Required<br>- Hiển thị thông báo mua credit |

#### TC-CHAT-003: AI Error - Refund Credit
| Field | Value |
|-------|-------|
| **Title** | Refund khi AI lỗi |
| **Precondition** | OpenAI API trả error |
| **Steps** | 1. Gửi câu hỏi<br>2. AI fail |
| **Expected** | - Credit được hoàn lại<br>- Error message hiển thị<br>- Reservation status = REFUNDED |

---

#### TC-QUIZ-001: Tạo đề AI
| Field | Value |
|-------|-------|
| **Title** | Tạo đề từ PDF |
| **Precondition** | User có ≥ 1 credit QUIZ_GEN |
| **Steps** | 1. Upload PDF<br>2. Chọn 20 câu<br>3. Nhập tên đề<br>4. Submit |
| **Expected** | - Quiz set tạo với 20 câu<br>- Mỗi câu có 4 đáp án<br>- 1 credit bị trừ |

#### TC-QUIZ-002: Làm bài thi
| Field | Value |
|-------|-------|
| **Title** | Complete exam |
| **Precondition** | Quiz có ≥ 1 question |
| **Steps** | 1. Start exam<br>2. Answer all questions<br>3. Submit |
| **Expected** | - Score calculated<br>- Attempt saved<br>- Result displayed with correct answers |

#### TC-QUIZ-003: Auto-submit khi hết giờ
| Field | Value |
|-------|-------|
| **Title** | Timeout auto-submit |
| **Precondition** | Exam started |
| **Steps** | 1. Để timer chạy hết |
| **Expected** | - Bài tự động nộp<br>- Điểm tính từ câu đã trả lời |

---

#### TC-PAYMENT-001: Mua credit thành công
| Field | Value |
|-------|-------|
| **Title** | Purchase credit via PayOS |
| **Precondition** | User logged in |
| **Steps** | 1. Chọn gói STUDENT<br>2. Redirect PayOS<br>3. Complete payment<br>4. Webhook callback |
| **Expected** | - Payment status = SUCCESS<br>- Credits cộng: 50 CHAT + 50 QUIZ_GEN<br>- Email confirmation sent |

#### TC-PAYMENT-002: Duplicate payment prevention
| Field | Value |
|-------|-------|
| **Title** | Reuse pending payment |
| **Precondition** | User có pending payment < 30 phút |
| **Steps** | 1. Click mua lại cùng gói |
| **Expected** | - Redirect đến existing checkout URL<br>- Không tạo payment mới |

---

### 6.3 Non-Functional Test Cases

#### TC-PERF-001: API Response Time
| Field | Value |
|-------|-------|
| **Title** | API response < 200ms |
| **Condition** | Non-AI endpoints |
| **Expected** | 95th percentile < 200ms |

#### TC-PERF-002: Concurrent Users
| Field | Value |
|-------|-------|
| **Title** | Handle 100 concurrent users |
| **Steps** | 100 users perform actions simultaneously |
| **Expected** | No errors, response time < 500ms |

#### TC-SEC-001: SQL Injection
| Field | Value |
|-------|-------|
| **Title** | Prevent SQL injection |
| **Steps** | Input: `'; DROP TABLE users; --` |
| **Expected** | Input sanitized, no SQL execution |

#### TC-SEC-002: XSS Prevention
| Field | Value |
|-------|-------|
| **Title** | Prevent XSS in chat |
| **Steps** | Input: `<script>alert('XSS')</script>` |
| **Expected** | Script tags escaped, no execution |

---

## 7. Tech Stack

### 7.1 Backend

| Component | Technology | Version |
|-----------|------------|---------|
| Framework | Spring Boot | 3.2.x |
| Language | Java | 17 |
| Database | PostgreSQL + pgvector | 15+ |
| Migration | Flyway | - |
| Security | Spring Security + JWT | - |
| AI | OpenAI API (GPT-4o-mini) | - |
| Payment | PayOS | - |
| Document Parser | Apache PDFBox, Apache POI | - |
| Build Tool | Maven | - |

### 7.2 Frontend

| Component | Technology |
|-----------|------------|
| Markup | HTML5 |
| Styling | CSS3 + Bootstrap 5 |
| Logic | JavaScript (Vanilla) |
| Charts | Chart.js |
| Icons | Font Awesome |

### 7.3 Infrastructure

| Component | Technology |
|-----------|------------|
| Hosting | Cloud VM / Container |
| SSL | Let's Encrypt |
| Email | SMTP (Brevo/Mailgun) |
| Monitoring | Spring Actuator |

---

## 8. Non-Functional Requirements

### 8.1 Performance

| Metric | Requirement |
|--------|-------------|
| Page load time | < 3 seconds |
| API response (non-AI) | < 200ms (p95) |
| API response (AI) | < 15 seconds |
| Concurrent users | 100+ |
| Database queries | Optimized with indexes |

### 8.2 Security

| Requirement | Implementation |
|-------------|----------------|
| Password hashing | BCrypt |
| Token security | JWT HS512, rotation |
| Data encryption | HTTPS/TLS |
| SQL injection | JPA parameterized queries |
| XSS | Input sanitization |
| CORS | Configured whitelist |
| Rate limiting | Redis-based (recommended) |

### 8.3 Scalability

| Requirement | Implementation |
|-------------|----------------|
| Horizontal scaling | Stateless JWT |
| Database scaling | Read replicas |
| Caching | Redis (future) |
| Async processing | @Async for AI calls |

### 8.4 Availability

| Metric | Target |
|--------|--------|
| Uptime | 99.5% |
| Recovery time | < 1 hour |
| Backup | Daily database backup |

### 8.5 Compatibility

| Platform | Support |
|----------|---------|
| Browsers | Chrome, Firefox, Safari, Edge (latest 2 versions) |
| Mobile | Responsive design |
| Screen sizes | 320px - 4K |

---

## 📎 Appendix

### A. Glossary

| Term | Definition |
|------|------------|
| **RAG** | Retrieval-Augmented Generation - Kỹ thuật kết hợp tìm kiếm và AI |
| **pgvector** | PostgreSQL extension cho vector similarity search |
| **Credit** | Đơn vị tiền ảo để sử dụng tính năng AI |
| **JWT** | JSON Web Token cho authentication |
| **PayOS** | Payment gateway Việt Nam |

### B. References

- [OpenAI API Documentation](https://platform.openai.com/docs)
- [PayOS Documentation](https://payos.vn/docs)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [pgvector GitHub](https://github.com/pgvector/pgvector)

---

> **Tài liệu này được tạo để phục vụ:**
> - 🎨 UI/UX Designer: Hiểu user flows, wireframes, design requirements
> - 🧪 QA/Tester: Tạo test cases, test scenarios
> - 💾 Database Designer: Thiết kế schema, indexes, functions

---

*Phiên bản: 1.0 | Cập nhật: 16/01/2026*
