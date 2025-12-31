# Toast Notification System Implementation

## Tổng Quan
Đã tạo hệ thống toast notification hiện đại để thay thế các alert() truyền thống trong ứng dụng.

## Files Đã Tạo

### 1. JavaScript - Toast Notification System
**File:** `src/main/resources/static/scripts/toast-notification.js`

**Tính năng:**
- Class `ToastNotification` với các phương thức:
  - `show(message, type, duration)` - Hiển thị toast
  - `success(message, duration)` - Toast thành công (màu xanh lá)
  - `error(message, duration)` - Toast lỗi (màu đỏ)
  - `warning(message, duration)` - Toast cảnh báo (màu vàng)
  - `info(message, duration)` - Toast thông tin (màu xanh dương)
- Global instance: `window.Toast`
- Backward compatibility: `window.showToast()`

### 2. CSS - Toast Styles
**File:** `src/main/resources/static/css/toast-notification.css`

**Đặc điểm:**
- Vị trí: Top-right corner (20px từ trên và phải)
- Animation: Slide in từ phải, fade out
- Duration: 2 giây (có thể tùy chỉnh)
- Responsive: Tự động full-width trên mobile
- Design:
  - Border-left màu theo type
  - Gradient background nhẹ
  - Icon tròn với gradient
  - Close button
  - Shadow và hover effects

## Files Đã Cập Nhật

### Trang Quản Lý Bộ Đề
Đã thêm toast notification vào các trang:

1. **my-quizzes.html** - Danh sách bộ đề
2. **quiz-manager.html** - Quản lý câu hỏi trong bộ đề
3. **quiz-add-quizset.html** - Tạo bộ đề mới
4. **quiz-add-question.html** - Thêm câu hỏi
5. **quiz-edit-question.html** - Sửa câu hỏi

### Thay Đổi Trong Mỗi File

#### 1. Thêm CSS
```html
<link rel="stylesheet" href="/css/toast-notification.css">
```

#### 2. Thêm JavaScript
```html
<script src="/scripts/toast-notification.js"></script>
```

#### 3. Cập Nhật Function showAlert()
**Trước:**
```javascript
function showAlert(message, type = 'danger') {
    const alertArea = document.getElementById('alertArea');
    alertArea.innerHTML = `
        <div class="alert alert-${type} alert-dismissible fade show" role="alert">
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;
}
```

**Sau:**
```javascript
function showAlert(message, type = 'danger') {
    // Use toast notification instead of alert area
    const toastType = type === 'danger' ? 'error' : type;
    Toast.show(message, toastType, 2000);
}
```

## Cách Sử Dụng

### Cách 1: Sử dụng Toast object
```javascript
// Success notification
Toast.success('Đã lưu câu hỏi thành công!');

// Error notification
Toast.error('Không thể xóa bộ đề');

// Warning notification
Toast.warning('Vui lòng kiểm tra lại thông tin');

// Info notification
Toast.info('Đang tải dữ liệu...');

// Custom duration (3 seconds)
Toast.success('Thành công!', 3000);
```

### Cách 2: Sử dụng showToast function
```javascript
showToast('Thông báo', 'success', 2000);
```

### Cách 3: Sử dụng showAlert (backward compatible)
```javascript
showAlert('Đã lưu thành công', 'success');
showAlert('Có lỗi xảy ra', 'danger');
```

## Toast Types & Colors

| Type | Color | Icon | Use Case |
|------|-------|------|----------|
| `success` | Green (#10b981) | Check circle | Thao tác thành công |
| `error` | Red (#ef4444) | X circle | Lỗi, thất bại |
| `warning` | Orange (#f59e0b) | Exclamation triangle | Cảnh báo |
| `info` | Blue (#3b82f6) | Info circle | Thông tin |

## Ví Dụ Thực Tế

### Thêm Câu Hỏi Thành Công
```javascript
Toast.success('Đã lưu câu hỏi. Bạn có thể nhập câu mới.');
```

### Xóa Bộ Đề Thất Bại
```javascript
Toast.error('Không thể xóa bộ đề');
```

### Cảnh Báo Validation
```javascript
Toast.warning('Vui lòng nhập tên bộ đề');
```

### Thông Tin Loading
```javascript
Toast.info('Đang tải dữ liệu...');
```

## Responsive Design

- **Desktop:** Toast hiển thị ở góc trên bên phải (320-420px width)
- **Mobile:** Toast full-width với margin 10px hai bên
- **Multiple toasts:** Xếp chồng theo chiều dọc với gap 12px

## Animation Details

- **Slide in:** translateX(400px) → translateX(0) trong 0.3s
- **Fade in:** opacity 0 → 1
- **Hover:** translateX(-4px) với shadow tăng
- **Slide out:** translateX(0) → opacity 0 trong 0.3s

## Browser Compatibility

- Chrome/Edge: ✅
- Firefox: ✅
- Safari: ✅
- Mobile browsers: ✅

## Next Steps

Có thể mở rộng thêm:
1. Thêm toast vào các trang khác (legal-chat, legal-upload, profile, etc.)
2. Thêm sound effects khi hiển thị toast
3. Thêm progress bar cho duration
4. Thêm action buttons trong toast
5. Lưu toast history

## Notes

- Toast tự động biến mất sau 2 giây (có thể tùy chỉnh)
- User có thể click nút X để đóng sớm
- Nhiều toast có thể hiển thị cùng lúc
- Toast không block UI như alert()
- Toast có thể stack lên nhau
