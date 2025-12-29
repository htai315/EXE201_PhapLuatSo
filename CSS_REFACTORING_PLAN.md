# CSS Refactoring Plan - Tách CSS Riêng Cho Từng Trang

## 🎯 Mục Tiêu
Tách CSS từ inline `<style>` trong HTML ra thành các file CSS riêng biệt, giữ phần dùng chung trong `style.css`

## 📊 Phân Tích Hiện Tại

### File CSS Hiện Có:
1. ✅ `style.css` - CSS chung (navbar, footer, buttons, cards, etc.)
2. ✅ `animations.css` - Animations chung
3. ✅ `index-enhanced.css` - Riêng cho index.html
4. ✅ `quiz-common.css` - Dùng chung cho các trang quiz
5. ✅ `quiz-pages.css` - Riêng cho quiz pages
6. ✅ `quiz-take.css` - Riêng cho quiz-take.html
7. ✅ `credits-counter.css` - Component credits counter

### File HTML Có Inline CSS (Cần Tách):
1. ❌ `index.html` - Có inline CSS cho pricing section
2. ❌ `plans.html` - Có inline CSS
3. ❌ `legal-upload.html` - Có inline CSS
4. ❌ `legal-chat.html` - Có inline CSS
5. ❌ `payment-result.html` - Có inline CSS
6. ❌ `profile.html` - Có inline CSS
7. ❌ `quiz-generate-ai.html` - Có inline CSS
8. ❌ `contact.html` - Có inline CSS
9. ❌ `about.html` - Có inline CSS

## 📝 Kế Hoạch Tách CSS

### 1. Phần Dùng Chung (Giữ trong style.css)
- CSS Variables (colors, fonts, spacing)
- Global styles (*, body, html)
- Typography (h1-h6, p, a)
- Navbar (toàn bộ)
- Footer (toàn bộ)
- Buttons (btn, btn-primary, btn-outline, etc.)
- Cards (card, card-header, card-body)
- Forms (form-control, form-label, form-select)
- Badges
- Alerts
- Utilities (section-padding, text-gradient, etc.)

### 2. File CSS Mới Cần Tạo

#### `plans.css` - Cho plans.html
- `.plans-header` và variants
- `.plans-container`
- `.pricing-card` và variants (nếu khác với style.css)
- `.pricing-badge`, `.pricing-header`, `.pricing-features`
- Animations riêng cho plans

#### `legal-upload.css` - Cho legal-upload.html
- `.legal-hero`
- `.legal-container`
- `.upload-card`
- `.upload-zone` và variants
- `.stats-card`
- `.document-card`
- Pagination styles
- Search box styles

#### `legal-chat.css` - Cho legal-chat.html
- `.chat-layout`
- `.chat-sidebar` và components
- `.chat-column`
- `.chat-header`, `.chat-body`, `.chat-composer`
- `.message-row`, `.user-message`, `.ai-message`
- `.session-item`
- Search box styles
- Responsive chat styles

#### `payment-result.css` - Cho payment-result.html
- `.result-container`
- `.result-card`
- `.result-icon` và variants
- `.result-title`, `.result-message`
- `.result-details`
- Animations (slideUp, scaleIn)

#### `profile.css` - Cho profile.html
- `.profile-header`
- `.profile-container`
- `.profile-avatar-card`
- `.profile-avatar`
- `.profile-card` và variants
- `.profile-info-item`
- `.profile-stats`
- `.transaction-item`

#### `quiz-generate-ai.css` - Cho quiz-generate-ai.html
- `.ai-hero`
- `.ai-container`
- `.upload-card` (nếu khác legal-upload)
- `.upload-zone` (nếu khác legal-upload)
- `.generation-progress`
- `.question-preview`

#### `contact.css` - Cho contact.html
- `.contact-hero`
- `.contact-section`
- `.contact-card`
- `.contact-form-card`
- `.contact-list`
- Map styles
- Form styles riêng

#### `about.css` - Cho about.html
- `.about-hero`
- `.about-section`
- `.values-list`
- `.value-item`
- `.team-section`
- `.team-card`
- `.team-avatar`
- `.cta-section`

## 🔧 Quy Tắc Tách CSS

### Giữ Trong style.css (Dùng Chung):
✅ CSS Variables
✅ Global resets
✅ Typography base
✅ Navbar (tất cả trang dùng)
✅ Footer (tất cả trang dùng)
✅ Buttons base (btn, btn-primary, btn-outline)
✅ Cards base (card, card-header, card-body)
✅ Forms base (form-control, form-label)
✅ Utilities (section-padding, fade-in, etc.)

### Tách Ra File Riêng:
❌ Page-specific layouts (hero, container)
❌ Page-specific components
❌ Page-specific animations
❌ Page-specific colors/gradients
❌ Page-specific responsive rules

## 📋 Thứ Tự Thực Hiện

### Phase 1: Tạo File CSS Mới
1. ✅ `plans.css`
2. ✅ `legal-upload.css`
3. ✅ `legal-chat.css`
4. ✅ `payment-result.css`
5. ✅ `profile.css`
6. ✅ `quiz-generate-ai.css`
7. ✅ `contact.css`
8. ✅ `about.css`

### Phase 2: Tách CSS Từ HTML
1. Copy CSS từ `<style>` tag
2. Paste vào file CSS tương ứng
3. Xóa `<style>` tag khỏi HTML
4. Link file CSS mới vào HTML
5. Test để đảm bảo không bị lỗi

### Phase 3: Dọn Dẹp style.css
1. Xóa CSS trùng lặp
2. Organize lại theo sections
3. Add comments rõ ràng
4. Optimize code

### Phase 4: Testing
1. Test từng trang một
2. Check responsive
3. Check animations
4. Check hover effects
5. Cross-browser testing

## 📁 Cấu Trúc File CSS Sau Khi Tách

```
css/
├── style.css              # CSS chung (navbar, footer, buttons, cards, forms)
├── animations.css         # Animations chung
├── index-enhanced.css     # Riêng cho index.html
├── plans.css             # Riêng cho plans.html
├── legal-upload.css      # Riêng cho legal-upload.html
├── legal-chat.css        # Riêng cho legal-chat.html
├── payment-result.css    # Riêng cho payment-result.html
├── profile.css           # Riêng cho profile.html
├── quiz-common.css       # Dùng chung cho quiz pages
├── quiz-pages.css        # Riêng cho quiz manager/add/edit
├── quiz-take.css         # Riêng cho quiz-take.html
├── quiz-generate-ai.css  # Riêng cho quiz-generate-ai.html
├── contact.css           # Riêng cho contact.html
├── about.css             # Riêng cho about.html
└── credits-counter.css   # Component credits counter
```

## ✅ Lợi Ích

1. **Dễ Quản Lý** - Mỗi trang có CSS riêng, dễ tìm và sửa
2. **Không Conflict** - CSS không đụng chạm nhau
3. **Performance** - Chỉ load CSS cần thiết cho từng trang
4. **Maintainability** - Dễ maintain và scale
5. **Team Work** - Nhiều người có thể làm việc song song
6. **Clean Code** - Code sạch, organized

## 🚀 Bắt Đầu!

Sẽ tạo từng file CSS và tách dần từ HTML.
