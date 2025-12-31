# Complete Notification System Implementation Guide

## Tổng Quan
Hướng dẫn chi tiết để thay thế tất cả `alert()` và `confirm()` trong toàn bộ ứng dụng bằng Toast Notification và Confirm Modal.

## Files Đã Hoàn Thành ✅

### 1. Quiz Management Pages
- ✅ `my-quizzes.html` - Toast notifications
- ✅ `quiz-manager.html` - Toast + Confirm modal (xóa bộ đề, xóa câu hỏi)
- ✅ `quiz-add-quizset.html` - Toast notifications
- ✅ `quiz-add-question.html` - Toast notifications
- ✅ `quiz-edit-question.html` - Toast notifications

### 2. Legal Pages
- ✅ `legal-upload.html` - Toast + Confirm modal (xóa văn bản)

### 3. Payment Pages
- ✅ `index.html` - Toast + Confirm modal (payment)
- ✅ `plans.html` - Toast + Confirm modal (payment)

## Files Cần Cập Nhật 📝

### 1. register.html
**Alerts hiện tại:**
```javascript
alert('Đăng ký thành công! Vui lòng đăng nhập');
```

**Cần thay thế:**
```javascript
Toast.success('Đăng ký thành công! Vui lòng đăng nhập');
setTimeout(() => {
    window.location.href = "login.html";
}, 1000);
```

**Thêm vào head:**
```html
<link rel="stylesheet" href="../css/toast-notification.css">
```

**Thêm vào scripts:**
```html
<script src="../scripts/toast-notification.js"></script>
```

---

### 2. profile.html
**Alerts hiện tại:**
```javascript
alert('Kích thước ảnh không được vượt quá 5MB');
alert('Vui lòng chọn file ảnh (JPG, PNG, GIF)');
alert('Cập nhật ảnh đại diện thành công');
alert('Có lỗi xảy ra khi tải ảnh lên. Vui lòng thử lại');
alert('Đổi mật khẩu thành công!');
```

**Cần thay thế:**
```javascript
Toast.warning('Kích thước ảnh không được vượt quá 5MB');
Toast.warning('Vui lòng chọn file ảnh (JPG, PNG, GIF)');
Toast.success('Cập nhật ảnh đại diện thành công');
Toast.error('Có lỗi xảy ra khi tải ảnh lên. Vui lòng thử lại');
Toast.success('Đổi mật khẩu thành công!');
```

**Thêm vào head:**
```html
<link rel="stylesheet" href="/css/toast-notification.css">
```

**Thêm vào scripts:**
```html
<script src="/scripts/toast-notification.js"></script>
```

---

### 3. quiz-generate-ai.html
**Alerts hiện tại:**
```javascript
alert('Chỉ hỗ trợ file PDF, DOCX, TXT');
alert('File không được vượt quá 10MB');
alert('Vui lòng chọn file');
alert('Vui lòng nhập tên bộ đề');
alert('Lỗi: ' + error.message);
```

**Cần thay thế:**
```javascript
Toast.warning('Chỉ hỗ trợ file PDF, DOCX, TXT');
Toast.warning('File không được vượt quá 10MB');
Toast.warning('Vui lòng chọn file');
Toast.warning('Vui lòng nhập tên bộ đề');
Toast.error('Lỗi: ' + error.message);
```

**Thêm vào head:**
```html
<link rel="stylesheet" href="/css/toast-notification.css">
```

**Thêm vào scripts:**
```html
<script src="/scripts/toast-notification.js"></script>
```

---

### 4. oauth2-redirect.html
**Alerts hiện tại:**
```javascript
alert('Đăng nhập thất bại: ' + error);
alert('Không tìm thấy token đăng nhập');
```

**Cần thay thế:**
```javascript
Toast.error('Đăng nhập thất bại: ' + error);
setTimeout(() => {
    window.location.href = '/html/login.html';
}, 1000);

Toast.error('Không tìm thấy token đăng nhập');
setTimeout(() => {
    window.location.href = '/html/login.html';
}, 1000);
```

**Thêm vào head:**
```html
<link rel="stylesheet" href="/css/toast-notification.css">
```

**Thêm vào scripts:**
```html
<script src="/scripts/toast-notification.js"></script>
```

---

### 5. quiz-take.html
**showAlert function đã có, chỉ cần thêm CSS/JS:**

**Thêm vào head:**
```html
<link rel="stylesheet" href="/css/toast-notification.css">
```

**Thêm vào scripts:**
```html
<script src="/scripts/toast-notification.js"></script>
```

**Cập nhật showAlert function:**
```javascript
function showAlert(message, type = 'danger') {
    const toastType = type === 'danger' ? 'error' : type;
    Toast.show(message, toastType, 2000);
}
```

---

### 6. legal-analyze.html
**Alerts hiện tại:**
```javascript
alert('Chọn file PDF');
```

**Cần thay thế:**
```javascript
Toast.warning('Vui lòng chọn file PDF');
```

**Thêm vào head:**
```html
<link rel="stylesheet" href="/css/toast-notification.css">
```

**Thêm vào scripts:**
```html
<script src="/scripts/toast-notification.js"></script>
```

---

### 7. legal-chat.html
**Alerts hiện tại:**
```javascript
alert('Không thể tải cuộc trò chuyện');
```

**Cần thay thế:**
```javascript
Toast.error('Không thể tải cuộc trò chuyện');
```

**Thêm vào head:**
```html
<link rel="stylesheet" href="/css/toast-notification.css">
```

**Thêm vào scripts:**
```html
<script src="/scripts/toast-notification.js"></script>
```

---

## Pattern Thay Thế

### Alert → Toast
```javascript
// OLD
alert('Message');

// NEW - Success
Toast.success('Message');

// NEW - Error
Toast.error('Message');

// NEW - Warning
Toast.warning('Message');

// NEW - Info
Toast.info('Message');
```

### Confirm → Confirm Modal
```javascript
// OLD
if (confirm('Bạn có chắc?')) {
    // do something
}

// NEW - Delete
const confirmed = await confirmDelete('item này');
if (confirmed) {
    // do something
}

// NEW - General
const confirmed = await confirmAction('Message', 'Title');
if (confirmed) {
    // do something
}

// NEW - Custom
const confirmed = await ConfirmModal.show({
    title: 'Title',
    message: 'Message',
    type: 'danger', // or 'warning', 'info'
    confirmText: 'OK',
    cancelText: 'Hủy'
});
if (confirmed) {
    // do something
}
```

### Alert với redirect
```javascript
// OLD
alert('Success!');
window.location.href = '/page.html';

// NEW
Toast.success('Success!');
setTimeout(() => {
    window.location.href = '/page.html';
}, 1000); // Delay 1s để user thấy toast
```

## Checklist Cập Nhật File

Khi cập nhật một file mới:

- [ ] Thêm CSS vào `<head>`:
  ```html
  <link rel="stylesheet" href="/css/toast-notification.css">
  <link rel="stylesheet" href="/css/confirm-modal.css"> <!-- Nếu có confirm -->
  ```

- [ ] Thêm JS trước `</body>`:
  ```html
  <script src="/scripts/toast-notification.js"></script>
  <script src="/scripts/confirm-modal.js"></script> <!-- Nếu có confirm -->
  ```

- [ ] Thay thế tất cả `alert()`:
  - Success → `Toast.success()`
  - Error → `Toast.error()`
  - Warning → `Toast.warning()`
  - Info → `Toast.info()`

- [ ] Thay thế tất cả `confirm()`:
  - Delete → `await confirmDelete('item')`
  - General → `await confirmAction('msg', 'title')`
  - Custom → `await ConfirmModal.show({...})`

- [ ] Thêm delay cho redirect sau toast:
  ```javascript
  setTimeout(() => { window.location.href = '...'; }, 1000);
  ```

- [ ] Test trên browser

## Toast Types Guide

| Situation | Toast Type | Example |
|-----------|-----------|---------|
| Thành công | `success` | Lưu thành công, Xóa thành công |
| Lỗi | `error` | Không thể kết nối, Lỗi server |
| Cảnh báo | `warning` | File quá lớn, Chưa nhập đủ thông tin |
| Thông tin | `info` | Đang xử lý, Vui lòng đợi |

## Confirm Modal Types Guide

| Situation | Modal Type | Example |
|-----------|-----------|---------|
| Xóa | `danger` | Xóa bộ đề, Xóa câu hỏi, Xóa văn bản |
| Cảnh báo | `warning` | Dữ liệu chưa lưu, Thay đổi quan trọng |
| Thông tin | `info` | Chuyển trang, Yêu cầu đăng nhập |

## Testing Checklist

Sau khi cập nhật, test các scenarios:

- [ ] Toast hiển thị đúng vị trí (top-right)
- [ ] Toast tự động biến mất sau 2s
- [ ] Toast có thể đóng bằng nút X
- [ ] Nhiều toast có thể hiển thị cùng lúc
- [ ] Confirm modal hiển thị ở giữa màn hình
- [ ] Confirm modal có backdrop
- [ ] Click backdrop để cancel
- [ ] Buttons hoạt động đúng
- [ ] Responsive trên mobile

## Summary

**Đã hoàn thành:** 8/15 files (53%)
**Còn lại:** 7 files

**Files ưu tiên cao:**
1. profile.html (nhiều alerts)
2. quiz-generate-ai.html (nhiều alerts)
3. register.html (success message)
4. oauth2-redirect.html (error handling)

**Files ưu tiên thấp:**
5. quiz-take.html (đã có showAlert function)
6. legal-analyze.html (1 alert)
7. legal-chat.html (1 alert)
