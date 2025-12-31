# 📚 Documentation Index

Chỉ mục tài liệu dự án Pháp Luật Số.

## 📖 Tài Liệu Chính

### 1. [README.md](README.md)
**Mô tả:** Tổng quan về dự án, tính năng, tech stack

**Nội dung:**
- Giới thiệu dự án
- Tính năng chính
- Tech stack
- Cài đặt nhanh
- Cấu trúc project
- License

**Dành cho:** Tất cả mọi người (developers, users, stakeholders)

---

### 2. [SETUP_GUIDE.md](SETUP_GUIDE.md)
**Mô tả:** Hướng dẫn cài đặt chi tiết từ A-Z

**Nội dung:**
- Yêu cầu hệ thống
- Cài đặt database
- Cấu hình environment
- Setup OpenAI API
- Setup Google OAuth2
- Setup VNPay Sandbox
- Build & Run
- Troubleshooting
- Production deployment

**Dành cho:** Developers mới, DevOps

---

### 3. [API_DOCUMENTATION.md](API_DOCUMENTATION.md)
**Mô tả:** Tài liệu API đầy đủ

**Nội dung:**
- Authentication APIs
- Quiz APIs
- AI APIs
- Chat APIs
- Payment APIs
- Credit APIs
- Error responses
- Rate limiting
- Testing với Postman

**Dành cho:** Frontend developers, API consumers, Testers

---

### 4. [ARCHITECTURE.md](ARCHITECTURE.md)
**Mô tả:** Kiến trúc hệ thống

**Nội dung:**
- Overall architecture
- Layer architecture
- Security architecture
- AI architecture (RAG)
- Payment architecture
- Credit system architecture
- Database schema
- Request flow examples
- Deployment architecture
- Scalability considerations

**Dành cho:** Architects, Senior developers, Technical leads

---

### 5. [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md)
**Mô tả:** Hướng dẫn phát triển và đóng góp

**Nội dung:**
- Development workflow
- Coding standards (Java, JavaScript, CSS)
- Project structure
- Common development tasks
- Testing guidelines
- Debugging tips
- Dependencies management
- Deployment
- Contributing guidelines

**Dành cho:** Developers, Contributors

---

## 🗂️ Cấu Trúc Tài Liệu

```
Documentation/
├── README.md                    # Tổng quan dự án
├── SETUP_GUIDE.md              # Hướng dẫn cài đặt
├── API_DOCUMENTATION.md        # Tài liệu API
├── ARCHITECTURE.md             # Kiến trúc hệ thống
├── DEVELOPMENT_GUIDE.md        # Hướng dẫn phát triển
└── DOCUMENTATION_INDEX.md      # File này
```

---

## 🎯 Tài Liệu Theo Vai Trò

### 👨‍💼 Project Manager / Stakeholder
**Đọc:**
1. README.md - Hiểu tổng quan dự án
2. ARCHITECTURE.md - Hiểu kiến trúc hệ thống

### 👨‍💻 New Developer
**Đọc theo thứ tự:**
1. README.md - Tổng quan
2. SETUP_GUIDE.md - Cài đặt môi trường
3. ARCHITECTURE.md - Hiểu kiến trúc
4. DEVELOPMENT_GUIDE.md - Bắt đầu code
5. API_DOCUMENTATION.md - Tham khảo API

### 🎨 Frontend Developer
**Đọc:**
1. README.md - Tổng quan
2. SETUP_GUIDE.md - Cài đặt
3. API_DOCUMENTATION.md - API endpoints
4. DEVELOPMENT_GUIDE.md - Coding standards

### 🔧 Backend Developer
**Đọc:**
1. README.md - Tổng quan
2. SETUP_GUIDE.md - Cài đặt
3. ARCHITECTURE.md - Kiến trúc
4. DEVELOPMENT_GUIDE.md - Coding standards
5. API_DOCUMENTATION.md - API design

### 🧪 QA / Tester
**Đọc:**
1. README.md - Tổng quan tính năng
2. SETUP_GUIDE.md - Cài đặt môi trường test
3. API_DOCUMENTATION.md - Test APIs
4. DEVELOPMENT_GUIDE.md - Testing guidelines

### 🚀 DevOps
**Đọc:**
1. SETUP_GUIDE.md - Setup & deployment
2. ARCHITECTURE.md - Infrastructure requirements
3. DEVELOPMENT_GUIDE.md - Build & deployment

---

## 📝 Tài Liệu Theo Chủ Đề

### 🔐 Authentication & Security
- **SETUP_GUIDE.md** → Google OAuth2 setup
- **ARCHITECTURE.md** → Security architecture
- **API_DOCUMENTATION.md** → Auth APIs

### 🤖 AI Features
- **README.md** → AI features overview
- **ARCHITECTURE.md** → RAG architecture
- **API_DOCUMENTATION.md** → AI APIs
- **SETUP_GUIDE.md** → OpenAI setup

### 💳 Payment System
- **ARCHITECTURE.md** → Payment architecture
- **API_DOCUMENTATION.md** → Payment APIs
- **SETUP_GUIDE.md** → VNPay setup

### 💰 Credit System
- **README.md** → Credit system overview
- **ARCHITECTURE.md** → Credit architecture
- **API_DOCUMENTATION.md** → Credit APIs

### 📝 Quiz Management
- **README.md** → Quiz features
- **API_DOCUMENTATION.md** → Quiz APIs
- **DEVELOPMENT_GUIDE.md** → Add quiz features

### 🗄️ Database
- **SETUP_GUIDE.md** → Database setup
- **ARCHITECTURE.md** → Database schema
- **DEVELOPMENT_GUIDE.md** → Add new tables

---

## 🔍 Quick Reference

### Cài Đặt Nhanh
```bash
# 1. Clone project
git clone <repo-url>

# 2. Setup .env
copy .env.example .env

# 3. Run
mvn spring-boot:run
```
→ Chi tiết: [SETUP_GUIDE.md](SETUP_GUIDE.md)

### API Endpoint Nhanh
```
POST /api/auth/login          # Login
GET  /api/quiz-sets/my        # Get quizzes
POST /api/chat/sessions/messages  # Chat AI
POST /api/payment/create      # Create payment
```
→ Chi tiết: [API_DOCUMENTATION.md](API_DOCUMENTATION.md)

### Thêm API Mới
```java
// 1. Create DTO
// 2. Add service method
// 3. Add controller endpoint
// 4. Test
```
→ Chi tiết: [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md)

---

## 🆘 Troubleshooting

### Không kết nối được database?
→ [SETUP_GUIDE.md](SETUP_GUIDE.md#troubleshooting)

### API trả về 401 Unauthorized?
→ [API_DOCUMENTATION.md](API_DOCUMENTATION.md#error-responses)

### Làm sao thêm feature mới?
→ [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md#common-development-tasks)

### Kiến trúc hệ thống như thế nào?
→ [ARCHITECTURE.md](ARCHITECTURE.md)

---

## 📞 Support

Nếu không tìm thấy thông tin cần thiết:
1. Tìm kiếm trong tài liệu (Ctrl+F)
2. Check GitHub Issues
3. Hỏi team
4. Tạo issue mới

---

## ✅ Documentation Checklist

Khi cập nhật code, nhớ cập nhật tài liệu:

- [ ] API mới → Update API_DOCUMENTATION.md
- [ ] Feature mới → Update README.md
- [ ] Thay đổi kiến trúc → Update ARCHITECTURE.md
- [ ] Thay đổi setup → Update SETUP_GUIDE.md
- [ ] Coding standard mới → Update DEVELOPMENT_GUIDE.md

---

## 📅 Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2024-12-31 | Initial documentation |

---

**Last Updated:** 2024-12-31  
**Maintained By:** Development Team
