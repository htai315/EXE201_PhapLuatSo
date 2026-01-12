# Implementation Tasks

## Task 1: Create AdminCsvExportService

**Requirements:** REQ-1, REQ-2, REQ-3

**File:** `src/main/java/com/htai/exe201phapluatso/admin/service/AdminCsvExportService.java`

**Acceptance Criteria:**
- [x] Create service class với UTF-8 BOM support
- [x] Implement `exportUsersToCsv(search, status)` method
- [x] Implement `exportPaymentsToCsv(fromDate, toDate)` method
- [x] Implement CSV field escaping (handle commas, quotes, newlines)
- [x] Enforce 10,000 record limit với exception khi vượt quá
- [x] Vietnamese column headers

---

## Task 2: Add Export Endpoints to AdminController

**Requirements:** REQ-1, REQ-2

**File:** `src/main/java/com/htai/exe201phapluatso/admin/controller/AdminController.java`

**Acceptance Criteria:**
- [x] Add `GET /api/admin/users/export` endpoint
- [x] Add `GET /api/admin/payments/export` endpoint
- [x] Set proper Content-Type và Content-Disposition headers
- [x] Handle date parameters cho payment export
- [x] Return proper error response khi exceed limit

---

## Task 3: Add Repository Query Methods (if needed)

**Requirements:** REQ-1, REQ-2, REQ-3

**Files:** 
- `src/main/java/com/htai/exe201phapluatso/auth/repo/UserRepo.java`
- `src/main/java/com/htai/exe201phapluatso/payment/repo/PaymentRepo.java`

**Acceptance Criteria:**
- [x] Add count methods for limit checking (used existing JpaSpecificationExecutor)
- [x] Add findAll with filters for export (reused existing + added JpaSpecificationExecutor to PaymentRepo)
