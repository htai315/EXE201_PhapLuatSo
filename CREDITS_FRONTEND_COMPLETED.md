# ✅ Credits System Frontend - HOÀN THÀNH

## Tổng Quan
Đã hoàn thành 100% hệ thống credits frontend, bao gồm:
- Credits counter component
- Tích hợp vào các trang chat và quiz
- Trang profile hiển thị chi tiết credits
- Trang pricing với 3 gói

---

## 📁 Files Đã Tạo/Cập Nhật

### 1. Credits Counter Component
**File mới:**
- ✅ `src/main/resources/static/scripts/credits-counter.js` (200 dòng)
- ✅ `src/main/resources/static/css/credits-counter.css` (100 dòng)

**Tính năng:**
- Hiển thị số credits còn lại với icon (💬 chat, 🤖 quiz gen)
- Màu sắc cảnh báo: xanh (>3), vàng (≤3), đỏ (0)
- Toast notification khi còn ít credits
- Modal upgrade khi hết credits
- Auto-refresh sau mỗi lần sử dụng

### 2. Legal Chat Page
**File cập nhật:** `src/main/resources/static/html/legal-chat.html`

**Thay đổi:**
- ✅ Thêm `<div id="chatCreditsCounter"></div>` trong navbar
- ✅ Include `credits-counter.js` và `credits-counter.css`
- ✅ Khởi tạo counter với type='chat'
- ✅ Refresh counter sau mỗi chat request

**Hiển thị:**
```
💬 9 lượt Chat
```

### 3. Quiz Generate AI Page
**File cập nhật:** `src/main/resources/static/html/quiz-generate-ai.html`

**Thay đổi:**
- ✅ Thêm `<div id="quizCreditsCounter"></div>` trong navbar
- ✅ Include `credits-counter.js` và `credits-counter.css`
- ✅ Khởi tạo counter với type='quiz_gen'
- ✅ Refresh counter sau khi tạo quiz

**Hiển thị:**
```
🤖 15 lượt AI Tạo Đề
```

### 4. Profile Page
**File cập nhật:** `src/main/resources/static/html/profile.html`

**Thay đổi:**
- ✅ Thêm card "Thông tin Credits" với:
  - 💬 Chat Credits: X lượt
  - 🤖 AI Tạo Đề: Y lượt
  - 📅 Hạn sử dụng: DD/MM/YYYY
  - ⭐ Gói hiện tại: FREE/REGULAR/STUDENT
  - Button "Nâng cấp gói"
- ✅ Function `loadCreditsInfo()` để fetch và hiển thị

### 5. Plans & Pricing Page
**File mới:** `src/main/resources/static/html/plans.html` (500+ dòng)

**Nội dung:**
- ✅ 3 pricing cards đẹp mắt:
  - **FREE**: 0 VND, 10 chat, vĩnh viễn
  - **REGULAR**: 159,000 VND, 100 chat, 12 tháng (Featured)
  - **STUDENT**: 249,000 VND, 100 chat + 20 quiz gen, 12 tháng
- ✅ Bảng so sánh chi tiết
- ✅ FAQ accordion
- ✅ Hover effects và animations
- ✅ Mobile responsive

---

## 🎨 UI/UX Features

### Credits Counter
```css
/* Màu sắc */
- Xanh lá (>3 credits): #059669
- Vàng (≤3 credits): #f59e0b + pulse animation
- Đỏ (0 credits): #dc2626 + pulse animation

/* Vị trí */
- Desktop: Góc phải navbar
- Mobile: Full width, center aligned
```

### Toast Notification
- Hiện khi còn ≤3 credits
- Tự động ẩn sau 5 giây
- Chỉ hiện 1 lần mỗi session

### Upgrade Modal
- Hiện khi credits = 0 hoặc expired
- Có button "Xem Các Gói" → plans.html
- Có button "Để Sau" để đóng modal

### Profile Credits Card
```
┌─────────────────────────────────┐
│ 💰 Thông tin Credits            │
├─────────────────────────────────┤
│ 💬 Chat Credits: 9 lượt         │
│ 🤖 AI Tạo Đề: 15 lượt           │
│ 📅 Hạn sử dụng: 23/12/2026      │
│ ⭐ Gói hiện tại: [REGULAR]      │
│                                 │
│ [Nâng cấp gói]                  │
└─────────────────────────────────┘
```

### Plans Page
```
┌──────────┐  ┌──────────┐  ┌──────────┐
│   FREE   │  │ REGULAR  │  │ STUDENT  │
│   🎁     │  │   💼     │  │   🎓     │
│          │  │ Phổ biến │  │          │
│  0 VND   │  │ 159K VND │  │ 249K VND │
│          │  │          │  │          │
│ 10 chat  │  │ 100 chat │  │ 100 chat │
│          │  │          │  │ 20 quiz  │
└──────────┘  └──────────┘  └──────────┘
```

---

## 🔄 User Flow

### 1. Đăng ký mới
```
User đăng ký → Database trigger → Tự động có 10 FREE credits
```

### 2. Sử dụng Chat AI
```
User vào legal-chat.html
  ↓
Navbar hiển thị: "💬 10 lượt Chat" (xanh)
  ↓
User gửi câu hỏi
  ↓
Backend trừ 1 credit
  ↓
Counter refresh: "💬 9 lượt Chat" (xanh)
  ↓
... tiếp tục ...
  ↓
Còn 3 credits → Counter chuyển vàng + toast warning
  ↓
Còn 0 credits → Counter đỏ + modal upgrade
```

### 3. Sử dụng AI Tạo Đề
```
User vào quiz-generate-ai.html
  ↓
Navbar hiển thị: "🤖 20 lượt AI Tạo Đề" (xanh)
  ↓
User upload file và tạo quiz
  ↓
Backend trừ 1 credit
  ↓
Counter refresh: "🤖 19 lượt AI Tạo Đề" (xanh)
```

### 4. Xem thông tin credits
```
User vào profile.html
  ↓
Hiển thị card "Thông tin Credits"
  ↓
User click "Nâng cấp gói"
  ↓
Chuyển đến plans.html
  ↓
User chọn gói → Liên hệ support
```

---

## 🧪 Testing Checklist

### ✅ Credits Counter
- [x] Hiển thị đúng số credits
- [x] Màu sắc thay đổi theo số lượng
- [x] Refresh sau khi sử dụng
- [x] Toast notification khi low credits
- [x] Modal upgrade khi hết credits
- [x] Responsive trên mobile

### ✅ Legal Chat Page
- [x] Counter hiển thị trong navbar
- [x] Counter refresh sau chat
- [x] Modal hiện khi hết credits
- [x] Link đến plans page hoạt động

### ✅ Quiz Generate Page
- [x] Counter hiển thị trong navbar
- [x] Counter refresh sau generate
- [x] Modal hiện khi hết credits
- [x] Link đến plans page hoạt động

### ✅ Profile Page
- [x] Credits info hiển thị đúng
- [x] Expiry date format đúng
- [x] Plan badge màu đúng
- [x] Button "Nâng cấp gói" hoạt động

### ✅ Plans Page
- [x] 3 cards hiển thị đẹp
- [x] Comparison table đầy đủ
- [x] FAQ accordion hoạt động
- [x] Hover effects mượt mà
- [x] Responsive trên mobile

---

## 📱 Responsive Design

### Desktop (>992px)
- Credits counter: Inline trong navbar
- Plans: 3 columns
- Profile: 2 columns (avatar + info)

### Tablet (768px - 991px)
- Credits counter: Full width
- Plans: 2 columns (FREE + REGULAR), STUDENT xuống dòng
- Profile: 2 columns

### Mobile (<768px)
- Credits counter: Full width, centered
- Plans: 1 column, stack vertically
- Profile: 1 column

---

## 🎯 API Endpoints Sử Dụng

### GET /api/credits/balance
**Request:**
```javascript
fetch('/api/credits/balance', {
    headers: {
        'Authorization': `Bearer ${token}`
    }
})
```

**Response:**
```json
{
    "chatCredits": 9,
    "quizGenCredits": 15,
    "expiryDate": "2026-12-23T00:00:00",
    "isExpired": false,
    "planName": "REGULAR"
}
```

---

## 🚀 Deployment Notes

### Files cần deploy:
1. `src/main/resources/static/scripts/credits-counter.js`
2. `src/main/resources/static/css/credits-counter.css`
3. `src/main/resources/static/html/legal-chat.html` (updated)
4. `src/main/resources/static/html/quiz-generate-ai.html` (updated)
5. `src/main/resources/static/html/profile.html` (updated)
6. `src/main/resources/static/html/plans.html` (new)

### Dependencies:
- Bootstrap 5.3.2 (đã có)
- Bootstrap Icons (đã có)
- jQuery KHÔNG cần (pure JavaScript)

### Browser Support:
- Chrome/Edge: ✅
- Firefox: ✅
- Safari: ✅
- IE11: ❌ (không support)

---

## 💡 Tips cho User

### Để test credits system:
1. Đăng ký user mới → Tự động có 10 FREE credits
2. Vào legal-chat.html → Thấy counter "💬 10 lượt Chat"
3. Chat 8 lần → Counter còn 2 (màu vàng) + toast warning
4. Chat thêm 2 lần → Counter = 0 (màu đỏ) + modal upgrade
5. Click "Xem Các Gói" → Chuyển đến plans.html

### Để thêm credits cho user (manual):
```sql
-- Thêm 100 chat credits cho user_id = 1
UPDATE user_credits 
SET chat_credits = chat_credits + 100,
    expiry_date = DATEADD(MONTH, 12, GETDATE())
WHERE user_id = 1;

-- Log transaction
INSERT INTO credit_transactions (user_id, type, amount, description)
VALUES (1, 'PURCHASE', 100, 'Admin added credits');
```

---

## ✅ HOÀN THÀNH

**Status**: Production Ready  
**Date**: December 23, 2025  
**Developer**: Kiro AI Assistant  

Hệ thống credits frontend đã hoàn thiện 100% và sẵn sàng sử dụng! 🎉
