# Credit System Improvements - Tasks

## Task 1: Database Migration
- [x] Create `V7__credit_improvements.sql`
  - Add `version` column to `user_credits` table
  - Create `credit_reservations` table
  - Add indexes

**Files:** `src/main/resources/db/migration/V7__credit_improvements.sql`

---

## Task 2: Entity Updates
- [x] Add `@Version` field to `UserCredit.java`
- [x] Create `CreditReservation.java` entity
- [x] Create `CreditReservationRepo.java` repository

**Files:**
- `src/main/java/com/htai/exe201phapluatso/auth/entity/UserCredit.java`
- `src/main/java/com/htai/exe201phapluatso/credit/entity/CreditReservation.java`
- `src/main/java/com/htai/exe201phapluatso/credit/repo/CreditReservationRepo.java`

---

## Task 3: Credit Refund Implementation
- [x] Add `reserveCredit()` method to `CreditService`
- [x] Add `confirmReservation()` method to `CreditService`
- [x] Add `refundReservation()` method to `CreditService`
- [x] Add retry mechanism with optimistic locking

**Files:** `src/main/java/com/htai/exe201phapluatso/credit/service/CreditService.java`

---

## Task 4: Reservation Cleanup Scheduler
- [x] Create `CreditReservationCleanupScheduler.java`
- [x] Implement expired reservation cleanup logic
- [x] Add configuration properties

**Files:**
- `src/main/java/com/htai/exe201phapluatso/credit/scheduler/CreditReservationCleanupScheduler.java`
- `src/main/resources/application.properties`

---

## Task 5: Admin Credit Service
- [x] Create `AdminCreditService.java`
- [x] Implement `addCredits()` method
- [x] Implement `removeCredits()` method
- [x] Implement `getCreditAnalytics()` method

**Files:** `src/main/java/com/htai/exe201phapluatso/admin/service/AdminCreditService.java`

---

## Task 6: DTOs
- [x] Create `AdminCreditAdjustRequest.java`
- [x] Create `CreditAnalyticsResponse.java`

**Files:**
- `src/main/java/com/htai/exe201phapluatso/admin/dto/AdminCreditAdjustRequest.java`
- `src/main/java/com/htai/exe201phapluatso/admin/dto/CreditAnalyticsResponse.java`

---

## Task 7: Admin Controller Endpoints
- [x] Add `POST /api/admin/users/{id}/credits/add` endpoint
- [x] Add `POST /api/admin/users/{id}/credits/remove` endpoint
- [x] Add `GET /api/admin/credits/analytics` endpoint

**Files:** `src/main/java/com/htai/exe201phapluatso/admin/controller/AdminController.java`

---

## Task 8: Analytics Queries
- [x] Add analytics queries to `CreditTransactionRepo`
  - Total usage by type
  - Total purchased by type
  - Total refunded
  - Daily usage aggregation

**Files:** `src/main/java/com/htai/exe201phapluatso/auth/repo/CreditTransactionRepo.java`

---

## Task 9: Integration with AI Services
- [x] Update `LegalChatService` to use reserve/confirm/refund pattern
- [x] Update `AIQuizService` to use reserve/confirm/refund pattern

**Files:**
- `src/main/java/com/htai/exe201phapluatso/legal/service/LegalChatService.java`
- `src/main/java/com/htai/exe201phapluatso/ai/service/AIQuizService.java`

---

## Task 10: Admin UI
- [x] Add credit management section to user detail modal
- [x] Add credit analytics section to dashboard

**Files:**
- `src/main/resources/static/html/admin/users.html`
- `src/main/resources/static/scripts/admin-users.js`
- `src/main/resources/static/html/admin/dashboard.html`
- `src/main/resources/static/scripts/admin-dashboard.js`
- `src/main/resources/static/css/admin.css`

---

## Dependencies
- Task 2 depends on Task 1 (migration must run first)
- Task 3 depends on Task 2 (entities must exist)
- Task 4 depends on Task 3 (reservation methods must exist)
- Task 5 depends on Task 2
- Task 7 depends on Task 5, Task 6
- Task 8 can run in parallel with Task 5
- Task 9 depends on Task 3
- Task 10 depends on Task 7
