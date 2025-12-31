# Fix: Đăng Nhập Bị Out Thỉnh Thoảng

## Vấn Đề
- JWT access token hết hạn sau 15 phút → bị logout
- Một số API calls không dùng `apiClient` → không auto-refresh token

## Giải Pháp Đã Áp Dụng

### 1. Tăng Thời Gian Token
**File:** `src/main/resources/application.properties`
```properties
# Trước: 15 phút
app.jwt.access-minutes=15

# Sau: 24 giờ (1440 phút)
app.jwt.access-minutes=1440
app.jwt.refresh-days=30
```

### 2. Fix Auto-Refresh Token
Đã sửa các file để dùng `apiClient` thay vì `fetch` trực tiếp:

**JavaScript files đã fix:**
- ✅ `src/main/resources/static/scripts/script.js` - Navbar auth check
- ✅ `src/main/resources/static/scripts/credits-counter.js` - Credits balance

**HTML files đã thêm api-client.js:**
- ✅ `index.html`
- ✅ `quiz-generate-ai.html` (quan trọng - dùng credits-counter)
- ✅ `legal-chat.html` (quan trọng - dùng credits-counter)
- ✅ `profile.html`
- ✅ `plans.html`
- ✅ `legal-upload.html`
- ✅ `guide.html`
- ✅ `contact.html`
- ✅ `about.html`
- ✅ `_template.html`

**Các trang khác đã có sẵn:**
- login.html, register.html, quiz-*.html, payment-history.html, my-quizzes.html, forgot-password.html, reset-password.html

**Cách hoạt động:**
```javascript
// ❌ Cũ: Không auto-refresh
const res = await fetch("/api/auth/me", {
    headers: { "Authorization": "Bearer " + accessToken }
})

// ✅ Mới: Tự động refresh khi token hết hạn
const data = await window.apiClient.get("/api/auth/me")
```

## Kết Quả
- Token giờ tồn tại 24 giờ thay vì 15 phút
- Khi token hết hạn, tự động refresh và retry request
- Không bị logout đột ngột nữa

## Test
1. Đăng nhập vào hệ thống
2. Để trình duyệt mở > 15 phút
3. Thử click vào các trang khác nhau
4. Kiểm tra console: Nếu thấy "Access token expired, attempting refresh..." → đang hoạt động đúng

## Lưu Ý
- Refresh token có thời hạn 30 ngày
- Sau 30 ngày không đăng nhập → phải đăng nhập lại
- Nếu vẫn bị logout → check console log để debug
