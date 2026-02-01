# 📚 Pháp Luật Số - Legal AI Platform

Nền tảng AI hỗ trợ học tập và tra cứu pháp luật Việt Nam với tính năng tạo đề thi tự động và chatbot tư vấn pháp luật.

## 🚀 Tính Năng Chính

### 1. 🤖 AI Chat Pháp Luật
- Chat với AI để tư vấn về pháp luật Việt Nam
- Tìm kiếm và trích dẫn điều luật chính xác
- Lưu lịch sử hội thoại
- Tìm kiếm trong lịch sử chat

### 2. 📝 AI Tạo Đề Thi
- Upload file PDF/DOCX về pháp luật
- AI tự động tạo câu hỏi trắc nghiệm
- Tùy chỉnh số lượng câu hỏi
- Lưu và quản lý bộ đề

### 3. 📖 Quản Lý Đề Thi
- Tạo bộ đề thủ công (MIỄN PHÍ)
- Thêm/sửa/xóa câu hỏi
- Làm bài thi và xem kết quả
- Xem lịch sử làm bài

### 4. 💳 Hệ Thống Credit
- **FREE**: Tạo đề thủ công, làm bài thi
- **PAID**: AI Chat (1 credit/message), AI tạo đề (1 credit/quiz)
- 3 gói: FREE (0đ), STUDENT (99,000đ), PREMIUM (199,000đ)
- Thanh toán qua VNPay

### 5. 🔐 Xác Thực
- Đăng ký/Đăng nhập email
- Google OAuth2
- JWT Token với auto-refresh

## 🛠️ Tech Stack

### Backend
- **Framework**: Spring Boot 3.x
- **Database**: PostgreSQL + pgvector
- **Migration**: Flyway
- **Security**: Spring Security + JWT
- **AI**: OpenAI GPT-4
- **Payment**: VNPay Sandbox

### Frontend
- **HTML5/CSS3/JavaScript**
- **Bootstrap 5**
- **Chart.js** (biểu đồ)
- **Responsive Design**

## 📦 Cài Đặt

### 1. Yêu Cầu
- Java 17+
- PostgreSQL 15+ (với pgvector extension)
- Maven
- OpenAI API Key
- VNPay Sandbox Account

### 2. Clone Project
```bash
git clone <repository-url>
cd EXE201_PhapLuatSo
```

### 3. Cấu Hình Database
Tạo database trong PostgreSQL:
```sql
CREATE DATABASE exe201_phapluatso;;;
```

### 4. Cấu Hình Environment
Copy `.env.example` thành `.env` và điền thông tin:
```env
# Database
DB_URL=jdbc:postgresql://localhost:5432/exe201_phapluatso
DB_USERNAME=your_username
DB_PASSWORD=your_password

# JWT
JWT_SECRET=your-secret-key-here
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# OpenAI
OPENAI_API_KEY=sk-your-openai-api-key

# Google OAuth2
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# VNPay
VNPAY_TMN_CODE=your-vnpay-tmn-code
VNPAY_HASH_SECRET=your-vnpay-hash-secret
VNPAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=http://localhost:8080/html/payment-result.html
```

### 5. Build & Run
```bash
# Build project
mvn clean install

# Run application
mvn spring-boot:run
```

Ứng dụng sẽ chạy tại: `http://localhost:8080`

## 📁 Cấu Trúc Project

```
src/
├── main/
│   ├── java/com/htai/exe201phapluatso/
│   │   ├── ai/              # AI services (OpenAI, Quiz Generation)
│   │   ├── auth/            # Authentication & User management
│   │   ├── common/          # Global exception handlers
│   │   ├── config/          # Configuration classes
│   │   ├── credit/          # Credit system
│   │   ├── legal/           # Legal chat & document services
│   │   ├── payment/         # VNPay payment integration
│   │   └── quiz/            # Quiz management
│   └── resources/
│       ├── db/migration/    # Flyway migrations
│       ├── static/          # Frontend files
│       │   ├── html/        # HTML pages
│       │   ├── css/         # Stylesheets
│       │   └── scripts/     # JavaScript files
│       └── application.properties
```

## 🔑 API Endpoints

### Authentication
- `POST /api/auth/register` - Đăng ký
- `POST /api/auth/login` - Đăng nhập
- `POST /api/auth/refresh` - Refresh token
- `GET /api/auth/me` - Thông tin user

### Quiz
- `GET /api/quiz-sets/my` - Danh sách bộ đề
- `POST /api/quiz-sets` - Tạo bộ đề mới
- `POST /api/quiz-sets/{id}/questions` - Thêm câu hỏi
- `POST /api/quiz-sets/{id}/exam/start` - Bắt đầu làm bài
- `POST /api/quiz-sets/{id}/exam/submit` - Nộp bài

### AI
- `POST /api/ai/quiz/generate-from-document` - AI tạo đề từ file
- `POST /api/chat/sessions/messages` - Chat với AI

### Payment
- `POST /api/payment/create` - Tạo thanh toán
- `GET /api/payment/vnpay-return` - VNPay callback

## 📖 Tài Liệu Chi Tiết

- [Setup Guide](SETUP_GUIDE.md) - Hướng dẫn cài đặt chi tiết
- [API Documentation](API_DOCUMENTATION.md) - Tài liệu API đầy đủ
- [Architecture](ARCHITECTURE.md) - Kiến trúc hệ thống
- [Development Guide](DEVELOPMENT_GUIDE.md) - Hướng dẫn phát triển

## 🧪 Testing

### Test VNPay Payment
Sử dụng thẻ test của VNPay Sandbox:
- Số thẻ: `9704198526191432198`
- Tên: `NGUYEN VAN A`
- Ngày phát hành: `07/15`
- OTP: `123456`

### Test AI Features
Cần có OpenAI API key với credit để test AI chat và AI tạo đề.

## 🔒 Security

- JWT authentication với access token (24h) và refresh token (7 days)
- Password hashing với BCrypt
- CORS configuration
- SQL injection prevention
- XSS protection
- Debug endpoints disabled in production

## 📝 License

This project is licensed under the MIT License.

## 👥 Contributors

- Development Team - EXE201

## 📞 Support

Nếu có vấn đề, vui lòng tạo issue trên GitHub hoặc liên hệ qua email.
