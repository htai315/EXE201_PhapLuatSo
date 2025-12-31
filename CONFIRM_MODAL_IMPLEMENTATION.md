# Confirm Modal System Implementation

## Tổng Quan
Đã tạo hệ thống confirm modal đẹp mắt để thay thế `confirm()` của browser, hiển thị ở giữa màn hình với animation và design hiện đại.

## Files Đã Tạo

### 1. JavaScript - Confirm Modal System
**File:** `src/main/resources/static/scripts/confirm-modal.js`

**Tính năng:**
- Class `ConfirmModal` với Bootstrap Modal
- Phương thức `show(options)` - Hiển thị modal với tùy chỉnh
- Convenience methods:
  - `danger(message, title)` - Modal xóa (màu đỏ)
  - `warning(message, title)` - Modal cảnh báo (màu vàng)
  - `info(message, title)` - Modal thông tin (màu xanh)
- Global functions:
  - `confirmAction(message, title)` - Xác nhận chung
  - `confirmDelete(itemName)` - Xác nhận xóa (shorthand)
- Returns Promise<boolean> - true nếu confirm, false nếu cancel

### 2. CSS - Confirm Modal Styles
**File:** `src/main/resources/static/css/confirm-modal.css`

**Đặc điểm:**
- Modal hiển thị ở giữa màn hình
- Border-radius 20px, shadow lớn
- Icon tròn 80px với gradient background
- Animation: scaleIn cho icon, slideDown cho modal
- 3 loại màu sắc:
  - Danger (đỏ): Cho xóa
  - Warning (vàng): Cho cảnh báo
  - Info (xanh): Cho thông tin
- Responsive: Stack buttons trên mobile

## Files Đã Cập Nhật

### 1. quiz-manager.html
**Chức năng xóa đã cập nhật:**

#### Xóa Bộ Đề
**Trước:**
```javascript
if (!confirm('Bạn có chắc muốn xóa toàn bộ Bộ đề này? Hành động không thể hoàn tác.')) return;
```

**Sau:**
```javascript
const confirmed = await confirmDelete('bộ đề này');
if (!confirmed) return;
```

#### Xóa Câu Hỏi
**Trước:**
```javascript
if (!confirm('Bạn có chắc muốn xóa câu hỏi này?')) return;
```

**Sau:**
```javascript
const confirmed = await confirmDelete('câu hỏi này');
if (!confirmed) return;
```

**Thêm:**
- CSS: `confirm-modal.css`
- JS: `confirm-modal.js`
- Toast success sau khi xóa thành công

### 2. legal-upload.html
**Chức năng xóa văn bản:**

**Trước:**
```javascript
if (!confirm('Bạn có chắc muốn xóa văn bản này?')) return;
alert('Có lỗi xảy ra');
```

**Sau:**
```javascript
const confirmed = await confirmDelete('văn bản này');
if (!confirmed) return;
Toast.success('Đã xóa văn bản thành công');
Toast.error('Có lỗi xảy ra khi xóa văn bản');
```

**Thêm:**
- CSS: `toast-notification.css`, `confirm-modal.css`
- JS: `toast-notification.js`, `confirm-modal.js`

## Cách Sử Dụng

### 1. Xác Nhận Xóa (Recommended)
```javascript
// Shorthand cho delete confirmation
const confirmed = await confirmDelete('bộ đề này');
if (!confirmed) return;

// Hoặc với tên item cụ thể
const confirmed = await confirmDelete('câu hỏi số 5');
if (!confirmed) return;
```

### 2. Xác Nhận Chung
```javascript
const confirmed = await confirmAction(
    'Bạn có chắc muốn thực hiện hành động này?',
    'Xác nhận'
);
if (!confirmed) return;
```

### 3. Sử Dụng ConfirmModal Object
```javascript
// Danger modal (màu đỏ)
const confirmed = await ConfirmModal.danger(
    'Bạn có chắc chắn muốn xóa bộ đề này? Hành động này không thể hoàn tác.',
    'Xác nhận xóa'
);

// Warning modal (màu vàng)
const confirmed = await ConfirmModal.warning(
    'Dữ liệu chưa được lưu. Bạn có muốn tiếp tục?',
    'Cảnh báo'
);

// Info modal (màu xanh)
const confirmed = await ConfirmModal.info(
    'Bạn sẽ được chuyển đến trang khác.',
    'Thông báo'
);
```

### 4. Custom Options
```javascript
const confirmed = await ConfirmModal.show({
    title: 'Xác nhận xuất file',
    message: 'Bạn có muốn xuất file Excel không?',
    confirmText: 'Xuất file',
    cancelText: 'Không',
    type: 'info'
});
```

## Modal Structure

```html
<div class="modal fade" id="confirmModal">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content confirm-modal-content">
            <div class="modal-body">
                <!-- Icon (80x80px, tròn, gradient) -->
                <div class="confirm-modal-icon">
                    <i class="bi bi-exclamation-triangle-fill"></i>
                </div>
                
                <!-- Title -->
                <h5 class="confirm-modal-title">Xác nhận</h5>
                
                <!-- Message -->
                <p class="confirm-modal-message">...</p>
                
                <!-- Actions -->
                <div class="confirm-modal-actions">
                    <button class="btn btn-secondary">Hủy</button>
                    <button class="btn btn-danger">Xác nhận</button>
                </div>
            </div>
        </div>
    </div>
</div>
```

## Modal Types & Colors

| Type | Icon | Icon BG | Button | Use Case |
|------|------|---------|--------|----------|
| `danger` | Exclamation Triangle | Red gradient | Red | Xóa, hành động nguy hiểm |
| `warning` | Exclamation Circle | Yellow gradient | Yellow | Cảnh báo, xác nhận quan trọng |
| `info` | Info Circle | Blue gradient | Blue | Thông tin, xác nhận thông thường |

## Design Details

### Icon
- Size: 80x80px (70px trên mobile)
- Border-radius: 50% (tròn)
- Background: Gradient theo type
- Animation: scaleIn (0.3s)

### Modal
- Border-radius: 20px
- Shadow: 0 20px 60px rgba(0,0,0,0.3)
- Padding: 3rem 2.5rem (2rem 1.5rem trên mobile)
- Animation: slideDown (0.3s)

### Buttons
- Min-width: 120px
- Padding: 0.75rem 1.5rem
- Border-radius: 10px
- Gradient background
- Hover: translateY(-2px) + shadow

## Responsive Design

**Desktop:**
- Modal width: auto (max-width từ Bootstrap)
- Buttons: Horizontal layout
- Icon: 80x80px

**Mobile (<576px):**
- Modal: Full width với margin
- Buttons: Stack vertical (full width)
- Icon: 70x70px
- Reduced padding

## Animation Details

### Icon Animation (scaleIn)
```css
from: scale(0), opacity(0)
to: scale(1), opacity(1)
duration: 0.3s
```

### Modal Animation (slideDown)
```css
from: translateY(-50px), opacity(0)
to: translateY(0), opacity(1)
duration: 0.3s
```

### Button Hover
```css
transform: translateY(-2px)
box-shadow: increased
```

## Integration Pattern

Khi thêm confirm modal vào trang mới:

1. **Thêm CSS:**
```html
<link rel="stylesheet" href="/css/confirm-modal.css">
```

2. **Thêm JS:**
```html
<script src="/scripts/confirm-modal.js"></script>
```

3. **Thay thế confirm():**
```javascript
// Old
if (!confirm('Bạn có chắc?')) return;

// New
const confirmed = await confirmDelete('item này');
if (!confirmed) return;
```

4. **Thêm toast success (optional):**
```javascript
Toast.success('Đã xóa thành công');
```

## Browser Compatibility

- Chrome/Edge: ✅
- Firefox: ✅
- Safari: ✅
- Mobile browsers: ✅
- Requires Bootstrap 5.x

## Next Steps

Có thể mở rộng thêm:
1. Thêm confirm modal vào các trang khác có chức năng xóa
2. Thêm input field trong modal (ví dụ: nhập "DELETE" để xác nhận)
3. Thêm countdown timer (3...2...1 trước khi cho phép confirm)
4. Thêm checkbox "Không hiển thị lại"
5. Thêm sound effects
6. Custom icon cho từng loại action

## Notes

- Modal tự động đóng khi click backdrop
- Modal trả về false nếu đóng bằng backdrop hoặc ESC
- Modal sử dụng Bootstrap Modal API
- Có thể có nhiều modal cùng lúc (nhưng không khuyến khích)
- Modal blocking - user phải chọn trước khi tiếp tục
