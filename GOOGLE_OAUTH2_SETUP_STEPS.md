# 🔐 Google OAuth2 - Các Bước Cần Làm NGAY

## ⚠️ CẢNH BÁO BẢO MẬT

Bạn đã vô tình chia sẻ Client Secret công khai! Cần hành động ngay:

### 1. RESET Client Secret (BẮT BUỘC)

1. Truy cập: https://console.cloud.google.com/
2. Chọn project "phap-luat-so"
3. Vào "APIs & Services" > "Credentials"
4. Click vào OAuth 2.0 Client ID
5. Click nút "RESET SECRET"
6. Copy Client Secret MỚI
7. Lưu vào nơi an toàn (KHÔNG chia sẻ)

### 2. Cập Nhật application.properties

Mở file: `src/main/resources/application.properties`

Tìm và thay thế:

```properties
# Google OAuth2
spring.security.oauth2.client.registration.google.client-id=583891350366-crtp3bmc5p1gv922a09dgnmfq44bb5s9.apps.googleusercontent.com
spring.security.oauth2.client.registration.google.client-secret=CLIENT_SECRET_MỚI_SAU_KHI_RESET
```

### 3. Thêm vào .gitignore

Đảm bảo file `.gitignore` có:

```
.env
application-local.properties
**/application-*.properties
!**/application.properties
```

### 4. Tạo file .env (Recommended)

Tạo file `.env` ở root project:

```env
GOOGLE_CLIENT_ID=583891350366-crtp3bmc5p1gv922a09dgnmfq44bb5s9.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-new-secret-here
```

Sau đó cập nhật `application.properties`:

```properties
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
```

---

## ✅ Checklist Implementation

- [ ] Reset Client Secret trong Google Console
- [ ] Cập nhật Client Secret mới vào application.properties
- [ ] Thêm .env vào .gitignore
- [ ] Chạy migration database (nếu cần)
- [ ] Test OAuth2 login flow
- [ ] Xác nhận không commit credentials lên Git

---

## 🚀 Các File Đã Tạo

1. ✅ `OAuth2UserInfo.java` - Interface
2. ✅ `GoogleOAuth2UserInfo.java` - Google implementation
3. ⏳ `CustomOAuth2UserService.java` - Service xử lý OAuth2
4. ⏳ `CustomOAuth2User.java` - Custom user object
5. ⏳ `OAuth2Controller.java` - Controller xử lý callback
6. ⏳ `SecurityConfig.java` - Cấu hình Spring Security
7. ⏳ `oauth2-redirect.html` - Frontend redirect page
8. ⏳ Cập nhật `login.html` - Thêm nút Google Login

---

## 📞 Cần Giúp Đỡ?

Sau khi reset Client Secret, cho tôi biết để tôi tiếp tục tạo các file còn lại!

**QUAN TRỌNG**: KHÔNG chia sẻ Client Secret mới công khai nữa!
