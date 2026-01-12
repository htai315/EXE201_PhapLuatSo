# 📊 PHÂN TÍCH & LỘ TRÌNH CẢI THIỆN DỰ ÁN PHÁP LUẬT SỐ

> **Ngày phân tích:** 09/01/2026  
> **Phiên bản:** 1.0

---

## 📋 MỤC LỤC

1. [Tổng quan dự án](#tổng-quan-dự-án)
2. [Module Authentication](#1--module-authentication)
3. [Module Quiz](#2--module-quiz)
4. [Module Legal Chat (RAG)](#3--module-legal-chat-rag)
5. [Module Payment](#4--module-payment)
6. [Module Credit](#5--module-credit)
7. [Module AI (Quiz Generation)](#6--module-ai-quiz-generation)
8. [Module Admin](#7--module-admin)
9. [Frontend](#8--frontend)
10. [Infrastructure & Security](#9--infrastructure--security)
11. [Lộ trình ưu tiên](#-lộ-trình-ưu-tiên-implementation)

---

## TỔNG QUAN DỰ ÁN

**Pháp Luật Số** là nền tảng AI pháp luật Việt Nam với các tính năng:
- AI Chatbot tư vấn pháp luật (RAG-based)
- Tạo đề thi trắc nghiệm bằng AI
- Quản lý bộ đề và làm bài thi
- Quản lý văn bản pháp luật
- Hệ thống credit và thanh toán
- Admin dashboard

**Tech Stack:** Spring Boot 4.0, PostgreSQL + pgvector, OpenAI GPT-4o-mini, PayOS, JWT Auth, OAuth2 (Google)

---

## 1. 🔐 MODULE AUTHENTICATION

### ✅ Điểm mạnh
- JWT + Refresh Token với cơ chế revoke tốt
- OAuth2 Google hoạt động ổn định
- Email verification với token expiry (24h)
- Password reset qua OTP (15 phút)
- Ban/Unban user có hiệu lực ngay lập tức (check trong JwtAuthFilter)
- Scheduled task tự động dọn dẹp token hết hạn
- Avatar upload với file validation

### ❌ Điểm yếu
- [ ] Chưa có rate limiting cho login (có thể bị brute force)
- [ ] Chưa có 2FA (Two-Factor Authentication)
- [ ] Chưa log login attempts thất bại
- [ ] Chưa có "Remember me" option
- [ ] Chưa có login history (IP, device, time)

### 🔧 Cần cải thiện
| Priority | Task | Effort |
|----------|------|--------|
| 🔴 Cao | Thêm rate limiting: max 5 login attempts/15 phút | 2h |
| 🟡 TB | Thêm login history (IP, device, time) | 4h |
| 🟡 TB | Lock account sau 10 lần sai password | 2h |
| 🟢 Thấp | Thêm 2FA với Google Authenticator | 8h |
| 🟢 Thấp | "Remember me" option (extend token expiry) | 2h |

---

## 2. 📝 MODULE QUIZ

### ✅ Điểm mạnh
- Randomize câu hỏi và shuffle đáp án (chống gian lận)
- Server-side validation đáp án (không tin frontend)
- Batch queries tránh N+1 problem
- PDF export với Vietnamese font support
- Timer countdown + auto-submit khi hết giờ
- Lịch sử làm bài với pagination
- Flag câu hỏi để review sau

### ❌ Điểm yếu
- [ ] Chưa implement chia sẻ bộ đề công khai (có field `visibility` nhưng chưa dùng)
- [ ] Chưa có import câu hỏi từ Excel/CSV
- [ ] Chưa có phân loại theo tag/chủ đề pháp luật
- [ ] Chưa có thống kê chi tiết (biểu đồ tiến bộ, điểm mạnh/yếu)
- [ ] Chưa có chế độ ôn tập thông minh (spaced repetition)
- [ ] Duration quiz hardcode 45 phút, chưa cho user tùy chỉnh
- [ ] Chưa có duplicate quiz set

### 🔧 Cần cải thiện
| Priority | Task | Effort |
|----------|------|--------|
| 🔴 Cao | Import câu hỏi từ Excel/CSV | 6h |
| 🔴 Cao | Implement PUBLIC visibility để chia sẻ bộ đề | 4h |
| 🟡 TB | Cho phép user set duration khi tạo quiz | 2h |
| 🟡 TB | Thêm tag/category cho câu hỏi (Dân sự, Hình sự, Lao động...) | 6h |
| 🟡 TB | Thống kê: % đúng theo chủ đề, câu hay sai nhất | 8h |
| 🟡 TB | Duplicate/Clone quiz set | 2h |
| 🟢 Thấp | Chế độ ôn tập thông minh (spaced repetition) | 12h |
| 🟢 Thấp | Leaderboard cho bộ đề công khai | 6h |

---

## 3. 💬 MODULE LEGAL CHAT (RAG)

### ✅ Điểm mạnh
- RAG pipeline: Search → AI Re-ranking → Generate với citations
- Chat history với sessions
- Keyword extraction + scoring algorithm
- Bigram extraction cho context tốt hơn
- Credit checking trước khi chat
- Session search và pagination

### ❌ Điểm yếu
- [ ] Search dựa trên SQL LIKE, không có vector search (semantic search kém)
- [ ] Chưa có conversation memory (mỗi message độc lập)
- [ ] Chưa có streaming response (phải đợi toàn bộ response)
- [ ] Chưa có feedback mechanism (user đánh giá câu trả lời)
- [ ] Regex parse article có thể miss một số format văn bản
- [ ] Chưa có suggested questions

### 🔧 Cần cải thiện
| Priority | Task | Effort |
|----------|------|--------|
| 🔴 Cao | Implement conversation memory (context từ messages trước) | 4h |
| 🔴 Cao | Thêm streaming response cho UX tốt hơn | 6h |
| 🟡 TB | Thêm thumbs up/down để cải thiện chất lượng | 3h |
| 🟡 TB | Suggested questions dựa trên context | 4h |
| 🟡 TB | Cải thiện regex parser cho nhiều format văn bản hơn | 4h |
| 🟢 Thấp | Vector database (Pinecone/Weaviate) cho semantic search | 16h |
| 🟢 Thấp | Export chat history to PDF | 4h |

---

## 4. 💳 MODULE PAYMENT

### ✅ Điểm mạnh
- PayOS integration hoàn chỉnh
- Webhook signature verification
- QR code generation
- Pessimistic locking cho webhook processing (tránh duplicate)
- Scheduled cleanup stale payments
- Retry mechanism cho API calls

### ❌ Điểm yếu
- [ ] Chỉ có PayOS, chưa có payment gateway khác (VNPay, Momo)
- [ ] Chưa có invoice/receipt generation
- [ ] Chưa có refund flow
- [ ] Chưa có subscription model (chỉ one-time purchase)
- [ ] Chưa có promo code/coupon

### 🔧 Cần cải thiện
| Priority | Task | Effort |
|----------|------|--------|
| 🟡 TB | Generate invoice PDF sau thanh toán | 4h |
| 🟡 TB | Thêm VNPay làm backup payment gateway | 8h |
| 🟡 TB | Implement promo code/coupon system | 6h |
| 🟢 Thấp | Implement refund flow | 6h |
| 🟢 Thấp | Subscription model cho enterprise | 12h |

---

## 5. 💰 MODULE CREDIT

### ✅ Điểm mạnh
- Pessimistic locking tránh race condition
- Credit expiry validation
- Transaction logging đầy đủ
- Phân biệt CHAT vs QUIZ_GEN credits
- Plan-based credit allocation

### ❌ Điểm yếu
- [ ] Chưa có credit gifting/transfer
- [ ] Chưa có promotional codes/coupons
- [ ] Chưa có credit refund khi có lỗi AI
- [ ] Chưa có notification khi credit sắp hết
- [ ] Chưa có credit usage analytics

### 🔧 Cần cải thiện
| Priority | Task | Effort |
|----------|------|--------|
| 🔴 Cao | Notification khi credit < 10 (email + in-app) | 3h |
| 🔴 Cao | Auto-refund credit nếu AI request fail | 2h |
| 🟡 TB | Credit usage analytics (biểu đồ sử dụng) | 4h |
| 🟢 Thấp | Credit gifting giữa users | 4h |

---

## 6. 🤖 MODULE AI (Quiz Generation)

### ✅ Điểm mạnh
- Chunking strategy cho large question counts (>20 câu)
- Context passing tránh duplicate questions
- Retry mechanism với exponential backoff
- Support PDF/DOCX/TXT
- Input sanitization
- Vietnamese text handling

### ❌ Điểm yếu
- [ ] Chỉ dùng GPT-4o-mini, chưa có fallback model
- [ ] Chưa có caching generated questions
- [ ] Chưa validate quality của generated questions
- [ ] Max 40 câu/lần, có thể không đủ cho một số use case
- [ ] Chưa có difficulty level selection

### 🔧 Cần cải thiện
| Priority | Task | Effort |
|----------|------|--------|
| 🟡 TB | Thêm fallback model (Claude, Gemini) khi OpenAI fail | 6h |
| 🟡 TB | Cho phép chọn difficulty level (Dễ/TB/Khó) | 3h |
| 🟡 TB | Cho phép generate nhiều hơn 40 câu | 2h |
| 🟢 Thấp | Cache generated questions để reuse | 4h |
| 🟢 Thấp | Quality scoring cho generated questions | 8h |

---

## 7. 👨‍💼 MODULE ADMIN

### ✅ Điểm mạnh
- Dashboard statistics với aggregations
- User management (ban/unban/delete)
- Activity logging cho audit trail
- Batch queries tránh N+1
- Revenue và user growth charts
- Sort field validation (whitelist)

### ❌ Điểm yếu
- [ ] Chưa có export data (Excel/CSV)
- [ ] Chưa có bulk actions (ban nhiều user cùng lúc)
- [ ] Chưa có email notification khi ban user
- [ ] Chưa có quiz management (admin xem/xóa quiz của user)
- [ ] Chưa có system health monitoring
- [ ] Chưa có legal document content preview

### 🔧 Cần cải thiện
| Priority | Task | Effort |
|----------|------|--------|
| 🔴 Cao | Export users/payments to Excel | 4h |
| 🟡 TB | Bulk ban/unban users | 3h |
| 🟡 TB | Email notification khi ban user | 2h |
| 🟡 TB | Admin có thể xem/moderate quiz content | 6h |
| 🟢 Thấp | System health dashboard (API latency, error rates) | 8h |
| 🟢 Thấp | Legal document content preview | 4h |

---

## 8. 🎨 FRONTEND

### ✅ Điểm mạnh
- UI/UX nhất quán với Bootstrap 5
- Toast notifications
- Confirm modals
- API client với auto token refresh
- Error handling tập trung
- Responsive design

### ❌ Điểm yếu
- [ ] Chưa có dark mode
- [ ] Chưa có PWA support (offline mode)
- [ ] Chưa có keyboard shortcuts
- [ ] Loading states có thể cải thiện (skeleton)
- [ ] Chưa có accessibility (ARIA labels)
- [ ] Chưa có i18n (multi-language)

### 🔧 Cần cải thiện
| Priority | Task | Effort |
|----------|------|--------|
| 🟡 TB | Dark mode toggle | 4h |
| 🟡 TB | Skeleton loading states | 3h |
| 🟡 TB | Keyboard shortcuts cho quiz (1-4 chọn đáp án, N/P next/prev) | 2h |
| 🟢 Thấp | PWA với service worker | 8h |
| 🟢 Thấp | ARIA labels cho accessibility | 4h |
| 🟢 Thấp | i18n support (English) | 12h |

---

## 9. 🔧 INFRASTRUCTURE & SECURITY

### ✅ Điểm mạnh
- Flyway migrations
- Environment variables qua .env
- CORS configuration
- Input validation với @Valid
- Custom exceptions với GlobalExceptionHandler
- Database indexes cho common queries

### ❌ Điểm yếu
- [ ] Chưa có rate limiting
- [ ] Chưa có request logging middleware
- [ ] Chưa có health check endpoint
- [ ] Chưa có metrics (Prometheus/Grafana)
- [ ] Chưa có caching layer (Redis)
- [ ] Chưa có API versioning

### 🔧 Cần cải thiện
| Priority | Task | Effort |
|----------|------|--------|
| 🔴 Cao | Rate limiting (bucket4j hoặc resilience4j) | 4h |
| 🔴 Cao | `/actuator/health` endpoint | 1h |
| 🟡 TB | Request/response logging middleware | 3h |
| 🟡 TB | Redis caching cho hot data | 6h |
| 🟢 Thấp | Prometheus metrics | 6h |
| 🟢 Thấp | API versioning (v1, v2) | 4h |

---

## 📈 LỘ TRÌNH ƯU TIÊN IMPLEMENTATION

### 🔴 PHASE 1: Critical (Tuần 1-2)
> **Mục tiêu:** Bảo mật và tính năng cốt lõi

| # | Task | Module | Effort | Status |
|---|------|--------|--------|--------|
| 1 | Rate limiting cho login/API | Auth/Infra | 4h | ⬜ |
| 2 | Import quiz từ Excel/CSV | Quiz | 6h | ⬜ |
| 3 | Notification credit sắp hết | Credit | 3h | ⬜ |
| 4 | Auto-refund credit khi AI fail | Credit | 2h | ⬜ |
| 5 | Health check endpoint | Infra | 1h | ⬜ |
| 6 | Export users/payments to Excel | Admin | 4h | ⬜ |

**Tổng effort Phase 1:** ~20h

---

### 🟡 PHASE 2: Important (Tuần 3-4)
> **Mục tiêu:** Cải thiện UX và tính năng mới

| # | Task | Module | Effort | Status |
|---|------|--------|--------|--------|
| 7 | Chia sẻ bộ đề công khai (PUBLIC visibility) | Quiz | 4h | ⬜ |
| 8 | Conversation memory cho chat | Legal | 4h | ⬜ |
| 9 | Streaming response cho chat | Legal | 6h | ⬜ |
| 10 | User set duration khi tạo quiz | Quiz | 2h | ⬜ |
| 11 | Tag/category cho câu hỏi | Quiz | 6h | ⬜ |
| 12 | Dark mode | Frontend | 4h | ⬜ |
| 13 | Thumbs up/down feedback | Legal | 3h | ⬜ |
| 14 | Invoice PDF generation | Payment | 4h | ⬜ |

**Tổng effort Phase 2:** ~33h

---

### 🟢 PHASE 3: Nice to Have (Tuần 5+)
> **Mục tiêu:** Polish và tính năng nâng cao

| # | Task | Module | Effort | Status |
|---|------|--------|--------|--------|
| 15 | 2FA authentication | Auth | 8h | ⬜ |
| 16 | Vector search (Pinecone) | Legal | 16h | ⬜ |
| 17 | Spaced repetition ôn tập | Quiz | 12h | ⬜ |
| 18 | PWA support | Frontend | 8h | ⬜ |
| 19 | Subscription model | Payment | 12h | ⬜ |
| 20 | System health monitoring | Admin | 8h | ⬜ |
| 21 | Redis caching | Infra | 6h | ⬜ |
| 22 | Leaderboard | Quiz | 6h | ⬜ |

**Tổng effort Phase 3:** ~76h

---

## 📊 TỔNG KẾT

| Metric | Value |
|--------|-------|
| **Tổng số improvements** | 40+ items |
| **Phase 1 (Critical)** | 6 tasks, ~20h |
| **Phase 2 (Important)** | 8 tasks, ~33h |
| **Phase 3 (Nice to Have)** | 8 tasks, ~76h |
| **Tổng effort ước tính** | ~129h |

---

## 📝 GHI CHÚ

- Effort được ước tính cho 1 developer
- Priority có thể thay đổi dựa trên feedback user
- Một số task có thể chạy song song
- Nên test kỹ sau mỗi phase trước khi deploy production

---

*Cập nhật lần cuối: 09/01/2026*
