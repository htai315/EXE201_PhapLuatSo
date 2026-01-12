# Credit System Improvements - Requirements

## Overview
Cải thiện hệ thống Credit với 4 tính năng: Credit Refund, Admin Credit Management, Credit Usage Analytics, và Optimistic Locking.

## Functional Requirements

### FR-1: Credit Refund (Ưu tiên cao)
**EARS Pattern:** When an AI operation fails after credit deduction, the system shall automatically refund the credit to the user.

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-1.1 | Hệ thống phải hỗ trợ reserve credit trước khi thực hiện AI operation | HIGH |
| FR-1.2 | Hệ thống phải confirm credit deduction khi AI operation thành công | HIGH |
| FR-1.3 | Hệ thống phải refund credit khi AI operation thất bại | HIGH |
| FR-1.4 | Mỗi reservation phải có timeout (default 5 phút) để tự động refund | MEDIUM |
| FR-1.5 | Transaction log phải ghi nhận cả RESERVE, CONFIRM, và REFUND | HIGH |

### FR-2: Admin Credit Management
**EARS Pattern:** When an admin needs to adjust user credits, the system shall provide endpoints to add or remove credits with audit logging.

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-2.1 | Admin có thể add credits (chat và quiz_gen) cho user | HIGH |
| FR-2.2 | Admin có thể remove credits từ user | HIGH |
| FR-2.3 | Mọi thay đổi credit bởi admin phải được log với reason | HIGH |
| FR-2.4 | Admin UI phải hiển thị form để quản lý credits | MEDIUM |
| FR-2.5 | Không cho phép credit balance âm | HIGH |

### FR-3: Credit Usage Analytics
**EARS Pattern:** When an admin views the dashboard, the system shall display credit usage statistics and trends.

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-3.1 | Hiển thị tổng credits đã sử dụng (theo loại) | HIGH |
| FR-3.2 | Hiển thị tổng credits đã mua | HIGH |
| FR-3.3 | Hiển thị tổng credits đã refund | MEDIUM |
| FR-3.4 | Hiển thị usage trend theo ngày (chart data) | MEDIUM |
| FR-3.5 | Hiển thị top users theo credit usage | LOW |

### FR-4: Optimistic Locking
**EARS Pattern:** When multiple concurrent requests modify user credits, the system shall use optimistic locking to reduce database contention.

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-4.1 | UserCredit entity phải có version field cho optimistic locking | HIGH |
| FR-4.2 | Khi version conflict xảy ra, retry tối đa 3 lần | HIGH |
| FR-4.3 | Nếu retry thất bại, throw exception với message rõ ràng | HIGH |
| FR-4.4 | Giữ pessimistic locking cho critical operations (purchase) | MEDIUM |

## Non-Functional Requirements

| ID | Requirement | Category |
|----|-------------|----------|
| NFR-1 | Credit operations phải hoàn thành trong < 100ms | Performance |
| NFR-2 | Reservation timeout phải configurable | Configurability |
| NFR-3 | Tất cả messages phải bằng tiếng Việt | Localization |
| NFR-4 | Code phải clean, không conflict với existing code | Maintainability |

## Acceptance Criteria

### AC-1: Credit Refund
- [ ] Reserve credit trước khi gọi AI
- [ ] Confirm credit khi AI thành công
- [ ] Refund credit khi AI thất bại
- [ ] Expired reservations được tự động cleanup

### AC-2: Admin Credit Management
- [ ] Admin có thể add credits qua API
- [ ] Admin có thể remove credits qua API
- [ ] Activity log ghi nhận mọi thay đổi
- [ ] UI form hoạt động đúng

### AC-3: Credit Usage Analytics
- [ ] API trả về usage statistics
- [ ] Dashboard hiển thị đúng data

### AC-4: Optimistic Locking
- [ ] Version field được thêm vào UserCredit
- [ ] Retry mechanism hoạt động đúng
- [ ] Concurrent requests không gây data corruption
