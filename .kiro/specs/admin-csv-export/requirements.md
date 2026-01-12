# Requirements Document

## Introduction

Tính năng cho phép Admin xuất danh sách Users và Payments ra file CSV để phân tích, báo cáo hoặc backup dữ liệu.

## Glossary

- **Admin_Dashboard**: Giao diện quản trị cho admin
- **CSV_Export_Service**: Service xử lý việc tạo file CSV
- **Admin_Controller**: REST controller cho admin endpoints

## Requirements

### Requirement 1: Export Users to CSV

**User Story:** As an admin, I want to export the user list to CSV, so that I can analyze user data in Excel or other tools.

#### Acceptance Criteria

1. WHEN an admin requests user export, THE CSV_Export_Service SHALL generate a CSV file containing user data
2. THE CSV file SHALL include columns: ID, Email, Full Name, Provider, Email Verified, Active, Banned, Created At
3. WHEN the CSV is generated, THE Admin_Controller SHALL return the file with proper Content-Disposition header for download
4. THE CSV file SHALL use UTF-8 encoding with BOM to support Vietnamese characters in Excel
5. WHEN filters are applied (search, status), THE CSV_Export_Service SHALL only export filtered users

### Requirement 2: Export Payments to CSV

**User Story:** As an admin, I want to export the payment list to CSV, so that I can create financial reports.

#### Acceptance Criteria

1. WHEN an admin requests payment export, THE CSV_Export_Service SHALL generate a CSV file containing payment data
2. THE CSV file SHALL include columns: ID, Order Code, User Email, Plan Code, Amount, Status, Payment Method, Created At, Paid At
3. WHEN the CSV is generated, THE Admin_Controller SHALL return the file with proper Content-Disposition header for download
4. THE CSV file SHALL use UTF-8 encoding with BOM to support Vietnamese characters in Excel
5. WHEN date filters are applied, THE CSV_Export_Service SHALL only export payments within the date range

### Requirement 3: Export Limits

**User Story:** As a system administrator, I want to limit export size, so that the server doesn't get overloaded.

#### Acceptance Criteria

1. THE CSV_Export_Service SHALL limit export to maximum 10,000 records per request
2. IF the result exceeds 10,000 records, THE Admin_Controller SHALL return an error message suggesting to apply filters
