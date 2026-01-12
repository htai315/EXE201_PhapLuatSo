# 🚀 Setup Guide - Pháp Luật Số

Hướng dẫn cài đặt chi tiết từ A-Z.

## 📋 Yêu Cầu Hệ Thống

### Phần Mềm Cần Thiết
- **Java**: JDK 17 trở lên
- **Database**: PostgreSQL 15+ (với pgvector extension)
- **Build Tool**: Maven 3.6+
- **IDE**: IntelliJ IDEA (khuyến nghị) hoặc Eclipse
- **Git**: Để clone project

### Tài Khoản Cần Thiết
- **OpenAI**: API key (có credit)
- **Google Cloud**: OAuth2 credentials
- **VNPay**: Sandbox account (test)

---

## 1️⃣ Cài Đặt Database

### Bước 1: Cài PostgreSQL
Download và cài đặt PostgreSQL từ https://www.postgresql.org/download/

Hoặc dùng Docker:
```bash
docker run -d --name postgres -p 5432:5432 -e POSTGRES_PASSWORD=postgres pgvector/pgvector:pg16
```

### Bước 2: Tạo Database
```sql
CREATE DATABASE exe201_phapluatso;
```

### Bước 3: Enable pgvector Extension
```sql
\c exe201_phapluatso
CREATE EXTENSION IF NOT EXISTS vector;
```

### Bước 4: Tạo User (Optional)
```sql
CREATE USER phapluatso_user WITH PASSWORD 'YourStrongPassword123!';
GRANT ALL PRIVILEGES ON DATABASE exe201_phapluatso TO phapluatso_user;
```

---

## 2️⃣ Clone & Setup Project

### Bước 1: Clone Repository
```bash
git clone <repository-url>
cd EXE201_PhapLuatSo
```

### Bước 2: Copy Environment File
```bash
copy .env.example .env
```

### Bước 3: Cấu Hình `.env`

Mở file `.env` và điền thông tin:

```env
# ===== DATABASE =====
DB_URL=jdbc:postgresql://localhost:5432/exe201_phapluatso
DB_USERNAME=postgres
DB_PASSWORD=YourStrongPassword123!

# ===== JWT =====
# Generate random secret: openssl rand -base64 64
JWT_SECRET=your-very-long-secret-key-at-least-256-bits-long-for-hs256
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# ===== OPENAI =====
OPENAI_API_KEY=sk-proj-your-openai-api-key-here

# ===== GOOGLE OAUTH2 =====
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-google-client-secret

# ===== VNPAY =====
VNPAY_TMN_CODE=your-vnpay-tmn-code
VNPAY_HASH_SECRET=your-vnpay-hash-secret
VNPAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=http://localhost:8080/html/payment-result.html
```

---

## 3️⃣ Cấu Hình OpenAI

### Bước 1: Tạo API Key
1. Truy cập: https://platform.openai.com/api-keys
2. Đăng nhập/Đăng ký
3. Click "Create new secret key"
4. Copy key và paste vào `.env`

### Bước 2: Nạp Credit
- Vào Billing: https://platform.openai.com/account/billing
- Thêm payment method
- Nạp ít nhất $5 để test

---

## 4️⃣ Cấu Hình Google OAuth2

### Bước 1: Tạo Project
1. Truy cập: https://console.cloud.google.com
2. Tạo project mới: "Phap Luat So"

### Bước 2: Enable APIs
1. Vào "APIs & Services" > "Library"
2. Tìm và enable "Google+ API"

### Bước 3: Tạo OAuth Credentials
1. Vào "APIs & Services" > "Credentials"
2. Click "Create Credentials" > "OAuth client ID"
3. Application type: "Web application"
4. Name: "Phap Luat So Web"
5. Authorized redirect URIs:
   ```
   http://localhost:8080/login/oauth2/code/google
   http://localhost:8080/oauth2/callback/google
   ```
6. Click "Create"
7. Copy Client ID và Client Secret vào `.env`

### Bước 4: Configure OAuth Consent Screen
1. Vào "OAuth consent screen"
2. User Type: "External"
3. App name: "Pháp Luật Số"
4. User support email: your-email@gmail.com
5. Developer contact: your-email@gmail.com
6. Save

---

## 5️⃣ Cấu Hình VNPay Sandbox

### Bước 1: Đăng Ký Sandbox
1. Truy cập: https://sandbox.vnpayment.vn/
2. Đăng ký tài khoản test
3. Đăng nhập vào merchant portal

### Bước 2: Lấy Thông Tin
1. Vào "Cấu hình"
2. Copy:
   - **TMN Code**: Mã website
   - **Hash Secret**: Secret key
3. Paste vào `.env`

### Bước 3: Test Card
Sử dụng thẻ test:
- Số thẻ: `9704198526191432198`
- Tên: `NGUYEN VAN A`
- Ngày phát hành: `07/15`
- OTP: `123456`

---

## 6️⃣ Build & Run

### Bước 1: Install Dependencies
```bash
mvn clean install
```

### Bước 2: Run Application
```bash
mvn spring-boot:run
```

Hoặc trong IntelliJ:
1. Mở `Exe201PhapLuatSoApplication.java`
2. Click nút Run (▶️)

### Bước 3: Verify
Mở browser: `http://localhost:8080`

Nếu thấy trang chủ → Setup thành công! 🎉

---

## 7️⃣ Database Migration

Flyway sẽ tự động chạy migrations khi start app.

### Check Migration Status
```sql
USE phapluatso;
GO

SELECT * FROM flyway_schema_history;
GO
```

### Manual Migration (nếu cần)
```bash
mvn flyway:migrate
```

---

## 8️⃣ Troubleshooting

### Lỗi: "Cannot connect to database"
**Giải pháp:**
1. Check PostgreSQL đang chạy
2. Check connection string trong `.env`
3. Check firewall cho phép port 5432
4. Check database đã được tạo: `psql -l`

### Lỗi: "OpenAI API key invalid"
**Giải pháp:**
1. Check API key trong `.env`
2. Check credit trong OpenAI account
3. Check API key chưa bị revoke

### Lỗi: "Google OAuth2 redirect_uri_mismatch"
**Giải pháp:**
1. Check redirect URI trong Google Console
2. Phải match chính xác với URL trong config
3. Không có trailing slash

### Lỗi: "VNPay signature invalid"
**Giải pháp:**
1. Check TMN Code và Hash Secret
2. Check URL encoding
3. Check thứ tự parameters

### Lỗi: "Port 8080 already in use"
**Giải pháp:**
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac
lsof -ti:8080 | xargs kill -9
```

---

## 9️⃣ IntelliJ Setup (Khuyến Nghị)

### Bước 1: Install EnvFile Plugin
1. File > Settings > Plugins
2. Search "EnvFile"
3. Install và restart

### Bước 2: Configure Run Configuration
1. Run > Edit Configurations
2. Chọn Spring Boot application
3. Tab "EnvFile"
4. Enable "Enable EnvFile"
5. Add `.env` file
6. Apply

### Bước 3: Enable Lombok
1. File > Settings > Plugins
2. Install "Lombok"
3. File > Settings > Build > Compiler > Annotation Processors
4. Enable "Enable annotation processing"

---

## 🔟 Production Deployment

### Environment Variables
Không dùng `.env` file trong production. Set environment variables:

```bash
# Linux/Mac
export DB_URL="jdbc:postgresql://your-db-host:5432/exe201_phapluatso"
export DB_USERNAME="..."
export DB_PASSWORD="..."
# ... other vars

# Windows
set DB_URL=jdbc:postgresql://your-db-host:5432/exe201_phapluatso
set DB_USERNAME=...
set DB_PASSWORD=...
```

### Build JAR
```bash
mvn clean package -DskipTests
```

JAR file: `target/exe201phapluatso-0.0.1-SNAPSHOT.jar`

### Run JAR
```bash
java -jar target/exe201phapluatso-0.0.1-SNAPSHOT.jar
```

### Production Checklist
- [ ] Change JWT secret
- [ ] Use production database
- [ ] Use production VNPay credentials
- [ ] Enable HTTPS
- [ ] Configure CORS properly
- [ ] Set `spring.profiles.active=production`
- [ ] Disable debug endpoints
- [ ] Setup logging
- [ ] Setup monitoring

---

## ✅ Verification Checklist

- [ ] Database connected
- [ ] Flyway migrations ran
- [ ] Application starts without errors
- [ ] Homepage loads at http://localhost:8080
- [ ] Can register new user
- [ ] Can login with email
- [ ] Can login with Google
- [ ] Can create manual quiz (free)
- [ ] Can upload document for AI quiz (costs credit)
- [ ] Can chat with AI (costs credit)
- [ ] Can make payment with VNPay test card
- [ ] Credits added after successful payment

---

## 📞 Need Help?

Nếu gặp vấn đề:
1. Check logs trong console
2. Check database connection
3. Check `.env` configuration
4. Tạo issue trên GitHub

Happy coding! 🚀
