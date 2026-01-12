# Design Document

## Overview

Thiết kế tính năng Export Users/Payments to CSV cho Admin Dashboard. Sử dụng streaming approach để xử lý large datasets hiệu quả.

## Architecture

### Component Diagram

```
┌─────────────────────┐     ┌──────────────────────┐     ┌─────────────────┐
│  AdminController    │────▶│  AdminCsvExportService│────▶│  UserRepo       │
│  (REST Endpoints)   │     │  (CSV Generation)     │     │  PaymentRepo    │
└─────────────────────┘     └──────────────────────┘     └─────────────────┘
         │
         ▼
┌─────────────────────┐
│  HTTP Response      │
│  (CSV Download)     │
└─────────────────────┘
```

## Design Details

### 1. AdminCsvExportService

**Location:** `src/main/java/com/htai/exe201phapluatso/admin/service/AdminCsvExportService.java`

**Responsibilities:**
- Generate CSV content với UTF-8 BOM cho Vietnamese characters
- Apply filters (search, status, date range)
- Enforce 10,000 record limit

**Methods:**

```java
public class AdminCsvExportService {
    
    private static final int MAX_EXPORT_RECORDS = 10000;
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    
    // Export users to CSV
    public byte[] exportUsersToCsv(String search, String status);
    
    // Export payments to CSV  
    public byte[] exportPaymentsToCsv(LocalDate fromDate, LocalDate toDate);
    
    // Helper methods
    private String escapeCsvField(String field);
    private String formatDateTime(LocalDateTime dateTime);
}
```

### 2. CSV Format

**Users CSV Columns:**
| Column | Source |
|--------|--------|
| ID | user.id |
| Email | user.email |
| Họ tên | user.fullName |
| Provider | user.provider |
| Email đã xác thực | user.emailVerified |
| Trạng thái | user.active ? "Hoạt động" : "Bị khóa" |
| Ngày tạo | user.createdAt |

**Payments CSV Columns:**
| Column | Source |
|--------|--------|
| ID | payment.id |
| Mã đơn hàng | payment.orderId |
| Email người dùng | payment.user.email |
| Gói | payment.plan.code |
| Số tiền | payment.amount |
| Trạng thái | payment.status |
| Phương thức | payment.paymentMethod |
| Ngày tạo | payment.createdAt |
| Ngày thanh toán | payment.paidAt |

### 3. REST Endpoints

**Export Users:**
```
GET /api/admin/users/export
Query params: search (optional), status (optional)
Response: CSV file download
Content-Type: text/csv; charset=UTF-8
Content-Disposition: attachment; filename="users_export_2026-01-12.csv"
```

**Export Payments:**
```
GET /api/admin/payments/export
Query params: from (optional), to (optional) - ISO date format
Response: CSV file download
Content-Type: text/csv; charset=UTF-8
Content-Disposition: attachment; filename="payments_export_2026-01-12.csv"
```

### 4. Error Handling

| Scenario | Response |
|----------|----------|
| Records > 10,000 | 400 Bad Request với message yêu cầu filter |
| No records found | 200 OK với CSV chỉ có header |
| Database error | 500 Internal Server Error |

## Security Considerations

1. **Authorization:** Chỉ ADMIN role mới access được endpoints
2. **Data Exposure:** Không export sensitive fields (password, tokens)
3. **Rate Limiting:** Inherits từ existing admin rate limiting

## Dependencies

- Existing `UserRepo` và `PaymentRepo`
- Existing `AdminService` filter logic
- No new dependencies required
