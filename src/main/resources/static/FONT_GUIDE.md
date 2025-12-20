# Hướng dẫn Font chữ - AI Luật

## 📝 Font chữ chuẩn cho toàn bộ dự án

### Font chính (Primary Font)
**Inter** - Font sans-serif hiện đại, dễ đọc
- Weights: 300, 400, 500, 600, 700, 800
- Sử dụng cho: Body text, buttons, forms, navigation

### Font tiêu đề (Display Font)
**Playfair Display** - Font serif sang trọng, chuyên nghiệp
- Weights: 600, 700, 800
- Sử dụng cho: Headings, titles, hero sections

## 🔗 Google Fonts Link (BẮT BUỘC cho mọi trang HTML)

```html
<!-- Google Fonts -->
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&family=Playfair+Display:wght@600;700;800&display=swap" rel="stylesheet">
```

## 📋 CSS Variables

```css
:root {
    --font-primary: "Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
    --font-display: "Playfair Display", Georgia, serif;
}
```

## ✅ Checklist khi tạo trang mới

1. ✅ Thêm Google Fonts link vào `<head>`
2. ✅ Import `/css/style.css` (chứa font variables)
3. ✅ Nếu là trang quiz: import `/css/quiz-common.css`
4. ✅ Nếu là trang thi thử: import `/css/quiz-take.css`
5. ✅ Sử dụng class `page-title` cho tiêu đề chính
6. ✅ Sử dụng class `section-subtitle` cho mô tả

## 📁 Các file CSS quan trọng

- `/css/style.css` - CSS chính, chứa font variables và global styles
- `/css/quiz-common.css` - CSS chung cho tất cả trang quiz
- `/css/quiz-take.css` - CSS riêng cho trang thi thử
- `/css/animations.css` - Animations và effects

## 🎨 Typography Classes

### Headings
- `.page-title` - Tiêu đề trang chính (1.75rem, Inter, bold)
- `.section-title` - Tiêu đề section (2.5rem, Playfair Display, bold)
- `.hero-title` - Tiêu đề hero (3.5rem, Playfair Display, bold)

### Body Text
- `.section-subtitle` - Mô tả section (1.125rem, Inter, regular)
- `.helper-text` - Text hướng dẫn (0.875rem, Inter, regular)
- `.question-meta` - Metadata (0.85rem, Inter, regular)

## 🚫 LƯU Ý QUAN TRỌNG

❌ **KHÔNG** sử dụng font khác ngoài Inter và Playfair Display
❌ **KHÔNG** quên thêm Google Fonts link
❌ **KHÔNG** override font-family trực tiếp trong inline styles
✅ **LUÔN** sử dụng CSS variables hoặc classes có sẵn

## 📱 Responsive Font Sizes

```css
/* Desktop */
.page-title { font-size: 1.75rem; }

/* Tablet */
@media (max-width: 991px) {
    .page-title { font-size: 1.5rem; }
}

/* Mobile */
@media (max-width: 767px) {
    .page-title { font-size: 1.35rem; }
}
```

## 🔍 Kiểm tra Font

Để kiểm tra font đã được áp dụng đúng:
1. Mở DevTools (F12)
2. Inspect một element text
3. Kiểm tra Computed styles
4. Font-family phải là: `Inter` hoặc `Playfair Display`

## 📞 Liên hệ

Nếu có thắc mắc về font chữ, vui lòng liên hệ team design.
