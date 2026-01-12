# Design Document: Mobile Responsive

## Overview

Tài liệu này mô tả thiết kế kỹ thuật để cải thiện giao diện responsive cho thiết bị di động của ứng dụng Pháp Luật Số. Thiết kế tuân theo nguyên tắc Mobile-First và sử dụng CSS Media Queries với các breakpoints chuẩn của Bootstrap 5.

## Architecture

### Breakpoint Strategy

Sử dụng breakpoints chuẩn Bootstrap 5:
- **xs**: < 576px (Extra small - phones)
- **sm**: ≥ 576px (Small - landscape phones)
- **md**: ≥ 768px (Medium - tablets)
- **lg**: ≥ 992px (Large - desktops)
- **xl**: ≥ 1200px (Extra large - large desktops)
- **xxl**: ≥ 1400px (Extra extra large)

### CSS Architecture

```
src/main/resources/static/css/
├── common.css          # Shared responsive utilities
├── style.css           # Main styles + navbar responsive
├── profile.css         # + Add responsive styles
├── about.css           # + Add responsive styles
├── legal-chat.css      # + Enhance mobile sidebar
├── quiz-*.css          # + Enhance quiz responsive
├── plans.css           # + Enhance payment responsive
├── admin.css           # Already has responsive (enhance)
└── mobile-utils.css    # NEW: Mobile utility classes
```

## Components and Interfaces

### 1. Mobile Navigation Component

```css
/* Navbar Mobile Toggle */
@media (max-width: 991px) {
    .navbar-toggler {
        display: block;
        border: none;
        padding: 0.5rem;
    }
    
    .navbar-collapse {
        position: fixed;
        top: 60px;
        left: 0;
        right: 0;
        background: white;
        box-shadow: 0 4px 12px rgba(0,0,0,0.1);
        max-height: calc(100vh - 60px);
        overflow-y: auto;
    }
}
```

### 2. Chat Sidebar Toggle Component

```html
<!-- Mobile Toggle Button -->
<button class="chat-sidebar-toggle d-lg-none">
    <i class="fas fa-history"></i>
</button>
```

```css
.chat-sidebar-toggle {
    position: fixed;
    bottom: 100px;
    left: 1rem;
    z-index: 1001;
    width: 50px;
    height: 50px;
    border-radius: 50%;
    background: var(--color-primary);
    color: white;
    border: none;
    box-shadow: 0 4px 12px rgba(0,0,0,0.2);
}
```

### 3. Responsive Table Component

```css
.table-responsive-mobile {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
}

@media (max-width: 767px) {
    .table-responsive-mobile table {
        min-width: 600px;
    }
}
```

## Data Models

Không có thay đổi về data models - đây là cải thiện frontend CSS/HTML thuần túy.

## Detailed Design

### Profile Page Responsive (profile.css)

```css
/* Mobile Responsive */
@media (max-width: 991px) {
    .profile-header {
        padding: 2rem 0 5rem;
    }
    
    .profile-header h1 {
        font-size: 1.5rem;
    }
}

@media (max-width: 767px) {
    .profile-container {
        padding: 0 0.75rem 2rem;
    }
    
    .profile-avatar {
        width: 100px;
        height: 100px;
    }
    
    .profile-card {
        padding: 1.25rem;
        margin-bottom: 1rem;
    }
    
    .profile-stats {
        flex-direction: column;
        gap: 0.5rem;
    }
    
    .profile-stat {
        padding: 0.5rem;
    }
    
    .profile-stat-number {
        font-size: 1.25rem;
    }
}

@media (max-width: 575px) {
    .profile-header {
        padding: 1.5rem 0 4rem;
        margin-bottom: -3rem;
    }
    
    .profile-info-item {
        padding: 0.75rem;
    }
}
```

### About Page Responsive (about.css)

```css
/* Mobile Responsive */
@media (max-width: 991px) {
    .team-avatar {
        width: 100px;
        height: 100px;
    }
    
    .team-name {
        font-size: 1.1rem;
    }
}

@media (max-width: 767px) {
    .mission-item {
        flex-direction: column;
        text-align: center;
    }
    
    .mission-icon {
        margin: 0 auto 0.5rem;
    }
    
    .team-card {
        padding: 1.5rem;
    }
    
    .team-description {
        font-size: 0.85rem;
    }
}

@media (max-width: 575px) {
    .team-avatar {
        width: 80px;
        height: 80px;
    }
}
```

### Legal Chat Enhanced Mobile (legal-chat.css)

```css
/* Enhanced Mobile Sidebar */
@media (max-width: 768px) {
    .chat-sidebar {
        position: fixed;
        left: -100%;
        top: 55px;
        width: 100%;
        height: calc(100vh - 55px);
        z-index: 1000;
        transition: left 0.3s ease;
        background: white;
    }
    
    .chat-sidebar.show {
        left: 0;
    }
    
    .chat-sidebar-overlay {
        display: none;
        position: fixed;
        top: 55px;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0,0,0,0.5);
        z-index: 999;
    }
    
    .chat-sidebar-overlay.show {
        display: block;
    }
    
    .chat-sidebar-toggle {
        display: flex;
        position: fixed;
        bottom: 100px;
        left: 1rem;
        z-index: 1001;
        width: 50px;
        height: 50px;
        border-radius: 50%;
        background: linear-gradient(135deg, #1a4b84 0%, #0f3054 100%);
        color: white;
        border: none;
        box-shadow: 0 4px 12px rgba(26, 75, 132, 0.3);
        align-items: center;
        justify-content: center;
        font-size: 1.2rem;
    }
    
    .chat-header {
        padding: 1rem 1.5rem;
    }
    
    .chat-header h1 {
        font-size: 1.2rem;
    }
    
    .chat-messages {
        padding: 1rem 1rem 0.5rem;
    }
    
    .chat-input-area {
        padding: 1rem 1rem 1.25rem;
    }
    
    .message-content {
        max-width: 90%;
    }
    
    .example-questions {
        grid-template-columns: 1fr;
        gap: 0.5rem;
    }
}
```

### Quiz Pages Enhanced (quiz-common.css, quiz-pages.css)

```css
/* quiz-common.css additions */
@media (max-width: 575px) {
    .page-wrapper {
        padding-top: 80px;
        padding-bottom: 1.5rem;
    }
    
    .page-title {
        font-size: 1.5rem;
    }
    
    .page-subtitle {
        font-size: 0.9rem;
    }
    
    .btn {
        padding: 0.75rem 1rem;
        font-size: 0.9rem;
    }
}

/* quiz-pages.css additions */
@media (max-width: 767px) {
    .question-list-card,
    .question-detail-card {
        padding: 1rem;
    }
    
    #questionListContainer {
        grid-template-columns: repeat(5, 1fr);
        gap: 0.4rem;
    }
    
    .question-number-btn {
        width: 36px;
        height: 36px;
        font-size: 0.8rem;
    }
}

@media (max-width: 575px) {
    #questionListContainer {
        grid-template-columns: repeat(5, 1fr);
        gap: 0.35rem;
    }
    
    .question-number-btn {
        width: 32px;
        height: 32px;
        font-size: 0.75rem;
    }
}
```

### Touch-Friendly Utilities (mobile-utils.css)

```css
/* Touch-Friendly Utilities */
@media (max-width: 991px) {
    /* Minimum touch target size */
    .btn,
    .nav-link,
    .form-control,
    .form-select,
    input[type="checkbox"],
    input[type="radio"] {
        min-height: 44px;
    }
    
    /* Adequate spacing */
    .btn + .btn {
        margin-left: 0.5rem;
    }
    
    /* Disable hover effects on touch */
    @media (hover: none) {
        .btn:hover,
        .card:hover,
        .nav-link:hover {
            transform: none;
            box-shadow: inherit;
        }
    }
    
    /* Full-width buttons on mobile */
    .btn-mobile-full {
        width: 100%;
        margin-bottom: 0.5rem;
    }
}

/* Typography Mobile */
@media (max-width: 767px) {
    body {
        font-size: 16px;
        line-height: 1.6;
    }
    
    h1 { font-size: 1.75rem; }
    h2 { font-size: 1.5rem; }
    h3 { font-size: 1.25rem; }
    h4 { font-size: 1.1rem; }
    
    p, li {
        font-size: 1rem;
        line-height: 1.6;
    }
}

/* Responsive Images */
img {
    max-width: 100%;
    height: auto;
}

.img-responsive {
    width: 100%;
    height: auto;
    object-fit: cover;
}

/* Responsive Video */
.video-responsive {
    position: relative;
    padding-bottom: 56.25%; /* 16:9 */
    height: 0;
    overflow: hidden;
}

.video-responsive iframe,
.video-responsive video {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
}

/* Table Responsive */
.table-mobile-scroll {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
}

@media (max-width: 767px) {
    .table-mobile-scroll table {
        min-width: 600px;
    }
    
    .table-mobile-scroll th,
    .table-mobile-scroll td {
        white-space: nowrap;
        padding: 0.75rem 0.5rem;
    }
}

/* Hide on mobile */
@media (max-width: 767px) {
    .hide-mobile { display: none !important; }
}

/* Show only on mobile */
@media (min-width: 768px) {
    .show-mobile-only { display: none !important; }
}
```

### Navbar Mobile Enhancement (style.css)

```css
/* Enhanced Mobile Navbar */
@media (max-width: 991px) {
    .navbar {
        padding: 0.5rem 0;
    }
    
    .navbar .container {
        flex-wrap: wrap;
        justify-content: space-between;
        padding: 0 1rem;
    }
    
    .navbar-brand {
        order: 1;
    }
    
    .navbar-toggler {
        order: 2;
        display: flex;
        align-items: center;
        justify-content: center;
        width: 44px;
        height: 44px;
        padding: 0;
        border: none;
        background: transparent;
        color: var(--color-dark);
    }
    
    .navbar-collapse {
        order: 3;
        width: 100%;
        background: white;
        border-radius: 12px;
        margin-top: 0.75rem;
        box-shadow: 0 4px 20px rgba(0,0,0,0.1);
        overflow: hidden;
    }
    
    .navbar-nav {
        padding: 0.5rem;
    }
    
    .navbar-nav .nav-item {
        width: 100%;
    }
    
    .navbar-nav .nav-link {
        padding: 0.875rem 1rem !important;
        border-radius: 8px;
    }
    
    .navbar-nav .nav-link:hover {
        background: #f8fafc;
    }
    
    .navbar-nav .btn {
        margin: 0.5rem;
        width: calc(100% - 1rem);
    }
}

@media (max-width: 575px) {
    .navbar-brand span {
        font-size: 1.1rem;
    }
    
    .navbar-brand img {
        height: 36px;
        width: 36px;
    }
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

Vì đây là cải thiện CSS/UI, các correctness properties tập trung vào visual testing và layout verification. Dựa trên prework analysis, các properties đã được consolidate để tránh redundancy:

**Property 1: Vertical Stacking on Mobile**
*For any* card-based layout (profile cards, pricing cards, team cards, statistics cards, quiz history cards, contact info cards) when viewed on Mobile_Device (< 768px), the cards SHALL be displayed in a single-column vertical stack.
**Validates: Requirements 2.1, 4.1, 4.3, 5.1, 6.4, 7.1, 7.4**

**Property 2: Touch Target Sizing**
*For any* interactive element (button, link, form input, checkbox, radio button) on Mobile_Device, the element SHALL have minimum height of 44px and minimum touch area of 44x44 pixels.
**Validates: Requirements 1.4, 4.2, 8.1, 8.4**

**Property 3: Mobile Navigation Toggle**
*For any* viewport width less than 992px, the hamburger menu icon SHALL be visible, and clicking it SHALL toggle the visibility of the navigation panel.
**Validates: Requirements 1.1, 1.2, 1.3**

**Property 4: Chat Sidebar Toggle**
*For any* Mobile_Device viewing the legal chat page, a toggle button SHALL be visible, and clicking it SHALL show/hide the chat history sidebar as a full-screen overlay.
**Validates: Requirements 3.1, 3.2, 3.3**

**Property 5: Table Horizontal Scrolling**
*For any* data table (payment history, admin tables) on Mobile_Device, the table container SHALL have overflow-x: auto enabled, allowing horizontal scrolling when content exceeds viewport width.
**Validates: Requirements 5.2, 6.3**

**Property 6: Image Responsiveness**
*For any* image element in the application, the image SHALL have max-width: 100% and maintain its aspect ratio, ensuring it never exceeds its container width.
**Validates: Requirements 10.1, 10.2, 10.3**

**Property 7: No Horizontal Overflow**
*For any* page in the application when viewed on Mobile_Device, no element SHALL cause horizontal scrolling of the viewport (except designated scrollable containers like tables).
**Validates: Requirements 9.4**

**Property 8: Typography Readability**
*For any* body text on Mobile_Device, the font-size SHALL be at least 16px and line-height SHALL be at least 1.5 for readability.
**Validates: Requirements 9.1, 9.3**

**Property 9: Form Input Full Width**
*For any* form input on Mobile_Device, the input width SHALL equal 100% of its container width for easy interaction.
**Validates: Requirements 2.4, 7.3**

**Property 10: Element Spacing**
*For any* adjacent clickable elements on Mobile_Device, there SHALL be at least 8px spacing between them to prevent accidental taps.
**Validates: Requirements 8.2**

## Error Handling

### CSS Fallbacks

```css
/* Fallback for older browsers */
.chat-sidebar {
    left: -280px; /* Fallback */
    left: -100%; /* Modern */
}

/* Feature detection */
@supports (backdrop-filter: blur(10px)) {
    .modal-backdrop {
        backdrop-filter: blur(10px);
    }
}

/* Safe area for notched phones */
@supports (padding: env(safe-area-inset-bottom)) {
    .chat-input-area {
        padding-bottom: calc(1.25rem + env(safe-area-inset-bottom));
    }
}
```

### JavaScript Fallbacks

```javascript
// Check for touch support
const isTouchDevice = 'ontouchstart' in window || navigator.maxTouchPoints > 0;

if (isTouchDevice) {
    document.body.classList.add('touch-device');
}
```

## Testing Strategy

### Manual Testing Checklist

1. **Device Testing**
   - iPhone SE (375px)
   - iPhone 12/13 (390px)
   - iPhone 12/13 Pro Max (428px)
   - Samsung Galaxy S21 (360px)
   - iPad Mini (768px)
   - iPad Pro (1024px)

2. **Browser Testing**
   - Chrome Mobile
   - Safari iOS
   - Samsung Internet
   - Firefox Mobile

3. **Orientation Testing**
   - Portrait mode
   - Landscape mode

### Automated Testing

```javascript
// Example Playwright test
test('mobile navigation works', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/');
    
    // Hamburger should be visible
    await expect(page.locator('.navbar-toggler')).toBeVisible();
    
    // Click hamburger
    await page.click('.navbar-toggler');
    
    // Navigation should be visible
    await expect(page.locator('.navbar-collapse')).toBeVisible();
});
```

### Visual Regression Testing

Sử dụng Percy hoặc Chromatic để capture screenshots ở các breakpoints:
- 320px, 375px, 414px (mobile)
- 768px, 1024px (tablet)
- 1280px, 1920px (desktop)

## Implementation Priority

1. **High Priority** (Core UX)
   - Mobile Navigation (Requirement 1)
   - Legal Chat Sidebar Toggle (Requirement 3)
   - Touch-Friendly Interactions (Requirement 8)

2. **Medium Priority** (Page-specific)
   - Profile Page Responsive (Requirement 2)
   - Quiz Pages Responsive (Requirement 4)
   - Payment Pages Responsive (Requirement 5)

3. **Lower Priority** (Enhancement)
   - About/Contact Pages (Requirement 7)
   - Admin Panel (Requirement 6)
   - Typography/Images (Requirements 9, 10)
