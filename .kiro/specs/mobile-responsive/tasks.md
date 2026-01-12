# Implementation Plan: Mobile Responsive

## Overview

Kế hoạch triển khai cải thiện giao diện responsive cho thiết bị di động. Các tasks được sắp xếp theo thứ tự ưu tiên: Core utilities → Navigation → Page-specific → Enhancement.

## Tasks

- [x] 1. Tạo Mobile Utilities CSS
  - Tạo file `mobile-utils.css` với các utility classes
  - Bao gồm: touch-friendly sizing, typography mobile, responsive images, table scroll
  - _Requirements: 8.1, 8.2, 8.4, 9.1, 9.3, 10.1, 10.2_

- [x] 2. Cải thiện Mobile Navigation
  - [x] 2.1 Cập nhật navbar trong `style.css` với hamburger menu responsive
    - Thêm media queries cho breakpoint 991px và 575px
    - Đảm bảo navbar-toggler hiển thị đúng
    - _Requirements: 1.1, 1.2, 1.3, 1.4_
  
  - [x] 2.2 Cập nhật HTML template để hỗ trợ Bootstrap navbar collapse
    - Kiểm tra và cập nhật `_template.html` nếu cần
    - Đảm bảo data-bs-toggle attributes đúng
    - _Requirements: 1.1, 1.2_

- [x] 3. Cải thiện Legal Chat Mobile
  - [x] 3.1 Thêm sidebar toggle button trong `legal-chat.css`
    - Tạo floating toggle button cho mobile
    - Thêm overlay khi sidebar mở
    - _Requirements: 3.1, 3.2, 3.3_
  
  - [x] 3.2 Cập nhật `legal-chat.html` với toggle button và overlay elements
    - Thêm HTML cho toggle button
    - Thêm overlay div
    - _Requirements: 3.2_
  
  - [x] 3.3 Thêm JavaScript toggle functionality
    - Xử lý click toggle button
    - Xử lý click overlay để đóng sidebar
    - _Requirements: 3.2, 3.3_

- [x] 4. Cải thiện Profile Page Responsive
  - [x] 4.1 Thêm media queries vào `profile.css`
    - Breakpoints: 991px, 767px, 575px
    - Stack cards vertically, adjust avatar size, font sizes
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 5. Cải thiện About Page Responsive
  - [x] 5.1 Thêm media queries vào `about.css`
    - Breakpoints: 991px, 767px, 575px
    - Stack team cards, adjust mission layout
    - _Requirements: 7.1, 7.2_

- [ ] 6. Checkpoint - Kiểm tra Core Pages
  - Ensure all tests pass, ask the user if questions arise.
  - Test trên mobile viewport: 375px, 414px
  - Kiểm tra navigation, chat, profile, about pages

- [x] 7. Cải thiện Quiz Pages Responsive
  - [x] 7.1 Cập nhật `quiz-common.css` với thêm breakpoints
    - Thêm breakpoint 575px
    - Adjust page-wrapper, buttons, titles
    - _Requirements: 4.1, 4.4_
  
  - [x] 7.2 Cập nhật `quiz-pages.css` với mobile enhancements
    - Adjust question grid cho mobile
    - Ensure touch-friendly question buttons
    - _Requirements: 4.5_
  
  - [x] 7.3 Kiểm tra `quiz-take.css` và enhance nếu cần
    - Verify existing responsive styles
    - Add missing mobile optimizations
    - _Requirements: 4.1, 4.2_
  
  - [x] 7.4 Cập nhật `quiz-history.css` cho mobile
    - Stack history items vertically
    - Adjust statistics display
    - _Requirements: 4.3_

- [x] 8. Cải thiện Payment Pages Responsive
  - [x] 8.1 Kiểm tra và enhance `plans.css`
    - Verify pricing cards stack on mobile
    - Ensure QR modal responsive
    - _Requirements: 5.1, 5.3_
  
  - [x] 8.2 Thêm responsive cho payment-history page
    - Add table-responsive wrapper nếu chưa có
    - Ensure horizontal scroll works
    - _Requirements: 5.2_
  
  - [x] 8.3 Kiểm tra `payment-result.css` responsive
    - Ensure content centered on mobile
    - _Requirements: 5.4_

- [x] 9. Cải thiện Contact Page Responsive
  - [x] 9.1 Kiểm tra và enhance `contact.css`
    - Verify existing responsive styles
    - Add missing mobile optimizations
    - _Requirements: 7.3, 7.4_

- [x] 10. Checkpoint - Kiểm tra All User Pages
  - Ensure all tests pass, ask the user if questions arise.
  - Test tất cả pages trên mobile viewport
  - Kiểm tra quiz, payment, contact pages

- [x] 11. Cải thiện Admin Panel Responsive
  - [x] 11.1 Kiểm tra và enhance `admin.css`
    - Verify sidebar collapse on tablet/mobile
    - Ensure data tables have horizontal scroll
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [x] 12. Include Mobile Utils CSS
  - [x] 12.1 Thêm link đến `mobile-utils.css` trong các HTML files
    - Cập nhật `_template.html`
    - Cập nhật các pages chính
    - _Requirements: 8.1, 8.2, 8.4, 9.1, 10.1_

- [x] 13. Final Checkpoint - Full Mobile Testing
  - Ensure all tests pass, ask the user if questions arise.
  - Test toàn bộ application trên các viewport:
    - 320px (small phone)
    - 375px (iPhone SE)
    - 414px (iPhone Plus)
    - 768px (tablet)
  - Kiểm tra tất cả interactive elements có touch-friendly sizing

## Notes

- Các tasks được sắp xếp theo dependency: utilities → navigation → page-specific
- Checkpoints ở task 6, 10, 13 để verify từng phase
- Admin panel (task 11) có priority thấp hơn vì ít user mobile access
- Tất cả CSS changes sử dụng existing breakpoints của Bootstrap 5
