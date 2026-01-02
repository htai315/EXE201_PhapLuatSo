# 🎯 Admin Dashboard - Phase 2: DTOs & Responses

**Status:** ✅ COMPLETED  
**Date:** December 31, 2024

---

## 📋 OVERVIEW

Phase 2 tạo tất cả các DTO (Data Transfer Object) classes cần thiết để:
- Truyền dữ liệu giữa Controller và Service layers
- Format response data cho frontend
- Validate request data từ frontend

---

## ✅ COMPLETED TASKS

### 1. Request DTOs

#### BanUserRequest.java
**Purpose:** Request để ban một user

**Fields:**
- `reason` (String, required, 10-500 chars) - Lý do ban user

**Validation:**
- `@NotBlank` - Reason không được empty
- `@Size(min=10, max=500)` - Reason phải từ 10-500 ký tự

**Usage:**
```java
POST /api/admin/users/{id}/ban
Body: {
  "reason": "Spam, vi phạm điều khoản sử dụng"
}
```

---

### 2. Response DTOs

#### AdminStatsResponse.java
**Purpose:** Dashboard statistics tổng quan

**Fields:**
- **User Stats:** totalUsers, activeUsers, bannedUsers, newUsersLast30Days
- **Payment Stats:** totalSuccessfulPayments, totalRevenue, revenueLast30Days
- **Quiz Stats:** totalQuizSets, totalQuizAttempts
- **Chat Stats:** totalChatSessions, totalChatMessages
- **Legal Docs:** totalLegalDocuments, totalLegalArticles
- **Charts:** usersByPlan (Map), revenueChart (List), userGrowthChart (List)

**Usage:**
```java
GET /api/admin/stats
Response: AdminStatsResponse
```

---

#### AdminUserListResponse.java
**Purpose:** User list với pagination

**Fields:**
- **Basic Info:** id, email, fullName, provider
- **Status:** emailVerified, enabled, active, banReason, bannedAt
- **Credits:** chatCredits, quizGenCredits
- **Stats:** totalPayments, totalQuizSets, totalChatSessions
- **Timestamps:** createdAt

**Usage:**
```java
GET /api/admin/users?page=0&size=20&search=email
Response: Page<AdminUserListResponse>
```

---

#### AdminUserDetailResponse.java
**Purpose:** Chi tiết đầy đủ của một user

**Fields:**
- **Basic Info:** id, email, fullName, avatarUrl, provider, providerId
- **Status:** emailVerified, enabled, active, banReason, bannedAt, bannedByUserId, bannedByUserName
- **Credits:** chatCredits, quizGenCredits, creditsExpiresAt
- **Statistics:** totalPayments, totalRevenue, totalQuizSets, totalQuizAttempts, totalChatSessions, totalChatMessages
- **Recent Activities:**
  - `recentPayments` (List<RecentPayment>) - 5 payments gần nhất
  - `recentQuizzes` (List<RecentQuiz>) - 5 quizzes gần nhất
  - `recentChats` (List<RecentChat>) - 5 chats gần nhất

**Inner Classes:**
- `RecentPayment`: id, orderId, amount, status, createdAt
- `RecentQuiz`: id, title, questionCount, createdAt
- `RecentChat`: id, title, messageCount, createdAt

**Usage:**
```java
GET /api/admin/users/{id}
Response: AdminUserDetailResponse
```

---

#### AdminPaymentListResponse.java
**Purpose:** Payment list với pagination

**Fields:**
- **Payment Info:** id, orderId, planCode, amount, status, paymentMethod, transactionNo
- **User Info:** userId, userEmail, userName
- **Timestamps:** createdAt, paidAt

**Usage:**
```java
GET /api/admin/payments?page=0&size=20
Response: Page<AdminPaymentListResponse>
```

---

#### AdminPaymentStatsResponse.java
**Purpose:** Payment statistics chi tiết

**Fields:**
- **Counts:** totalPayments, successfulPayments, failedPayments, pendingPayments
- **Revenue:** totalRevenue, revenueToday, revenueThisWeek, revenueThisMonth
- **Metrics:** averagePaymentAmount, successRate
- **Breakdown:** revenueByPlan (Map), paymentCountByPlan (Map)

**Usage:**
```java
GET /api/admin/payments/stats
Response: AdminPaymentStatsResponse
```

---

### 3. Chart Data DTOs

#### RevenueByDate.java
**Purpose:** Revenue chart data (Chart.js)

**Fields:**
- `date` (LocalDate) - Ngày
- `revenue` (Long) - Doanh thu trong ngày
- `paymentCount` (Integer) - Số lượng payments

**Usage:**
```java
GET /api/admin/stats/revenue?from=2024-01-01&to=2024-12-31
Response: List<RevenueByDate>
```

**Chart.js Example:**
```javascript
{
  labels: data.map(d => d.date),
  datasets: [{
    label: 'Revenue',
    data: data.map(d => d.revenue)
  }]
}
```

---

#### UserGrowth.java
**Purpose:** User growth chart data

**Fields:**
- `date` (LocalDate) - Ngày
- `newUsers` (Long) - Số user mới trong ngày
- `totalUsers` (Long) - Tổng số users đến ngày đó

**Usage:**
```java
GET /api/admin/stats/user-growth?from=2024-01-01&to=2024-12-31
Response: List<UserGrowth>
```

**Chart.js Example:**
```javascript
{
  labels: data.map(d => d.date),
  datasets: [
    {
      label: 'New Users',
      data: data.map(d => d.newUsers)
    },
    {
      label: 'Total Users',
      data: data.map(d => d.totalUsers)
    }
  ]
}
```

---

## 📁 FILES CREATED

### DTOs Created (8 files):
1. ✅ `BanUserRequest.java` - Request to ban user
2. ✅ `AdminStatsResponse.java` - Dashboard statistics
3. ✅ `AdminUserListResponse.java` - User list
4. ✅ `AdminUserDetailResponse.java` - User details
5. ✅ `AdminPaymentListResponse.java` - Payment list
6. ✅ `AdminPaymentStatsResponse.java` - Payment statistics
7. ✅ `RevenueByDate.java` - Revenue chart data
8. ✅ `UserGrowth.java` - User growth chart data

### Documentation:
9. ✅ `ADMIN_DASHBOARD_PHASE2.md` (this file)

**Total:** 9 files

---

## 🎨 DTO DESIGN PATTERNS

### 1. Separation of Concerns
- **Request DTOs:** Validate input data
- **Response DTOs:** Format output data
- **Chart DTOs:** Specialized for Chart.js

### 2. Nested DTOs
`AdminUserDetailResponse` uses inner classes for recent activities:
- Keeps related data together
- Easier to serialize/deserialize
- Better code organization

### 3. Validation Annotations
Using Jakarta Validation:
- `@NotBlank` - Field không được empty
- `@Size(min, max)` - Giới hạn độ dài string

### 4. Naming Conventions
- **Request:** `{Action}{Entity}Request` (e.g., BanUserRequest)
- **Response:** `Admin{Entity}{Type}Response` (e.g., AdminUserListResponse)
- **Chart Data:** `{Metric}By{Dimension}` (e.g., RevenueByDate)

---

## 🔄 DATA FLOW

### Example: Get User List

```
Frontend Request
    ↓
GET /api/admin/users?page=0&size=20&search=john
    ↓
AdminController.getAllUsers()
    ↓
AdminService.getAllUsers(pageable, search)
    ↓
UserRepository.findAll(specification, pageable)
    ↓
Map<User, AdminUserListResponse>
    ↓
Page<AdminUserListResponse>
    ↓
JSON Response to Frontend
```

### Example: Ban User

```
Frontend Request
    ↓
POST /api/admin/users/5/ban
Body: { "reason": "Spam" }
    ↓
AdminController.banUser(5, BanUserRequest)
    ↓
Validate BanUserRequest (@Valid)
    ↓
AdminService.banUser(5, reason)
    ↓
Update User entity (active=false, banReason, bannedAt)
    ↓
Log to AdminActivityLog
    ↓
Success Response
```

---

## 📊 DTO USAGE SUMMARY

| DTO | Endpoint | Purpose |
|-----|----------|---------|
| BanUserRequest | POST /api/admin/users/{id}/ban | Ban user |
| AdminStatsResponse | GET /api/admin/stats | Dashboard overview |
| AdminUserListResponse | GET /api/admin/users | User list with pagination |
| AdminUserDetailResponse | GET /api/admin/users/{id} | User details |
| AdminPaymentListResponse | GET /api/admin/payments | Payment list |
| AdminPaymentStatsResponse | GET /api/admin/payments/stats | Payment statistics |
| RevenueByDate | GET /api/admin/stats/revenue | Revenue chart |
| UserGrowth | GET /api/admin/stats/user-growth | User growth chart |

---

## 🎯 NEXT STEPS (Phase 3)

Phase 3 will implement Service layer:

1. **AdminService.java** - Main admin service with methods:
   - `getDashboardStats()` - Get dashboard statistics
   - `getRevenueChart()` - Get revenue chart data
   - `getUserGrowthChart()` - Get user growth data
   - `getAllUsers()` - Get all users with pagination
   - `getUserDetail()` - Get user details
   - `banUser()` - Ban a user
   - `unbanUser()` - Unban a user
   - `deleteUser()` - Delete a user
   - `getAllPayments()` - Get all payments
   - `getPaymentStats()` - Get payment statistics

2. **AdminActivityLogService.java** - Log admin actions

3. **Update UserService.java** - Add admin-related methods

---

## ✅ VERIFICATION

To verify DTOs are created correctly:

```bash
# Check all DTO files exist
ls src/main/java/com/htai/exe201phapluatso/admin/dto/

# Expected output:
# AdminPaymentListResponse.java
# AdminPaymentStatsResponse.java
# AdminStatsResponse.java
# AdminUserDetailResponse.java
# AdminUserListResponse.java
# BanUserRequest.java
# RevenueByDate.java
# UserGrowth.java
```

---

## 📝 NOTES

- All DTOs use standard Java getters/setters (no Lombok to avoid dependencies)
- All DTOs have default constructors for JSON deserialization
- Validation annotations are only on Request DTOs
- Response DTOs are designed for easy JSON serialization
- Chart DTOs are optimized for Chart.js library

---

**Phase 2 Status:** ✅ COMPLETED  
**Ready for Phase 3:** ✅ YES

**Next Command:** "Hãy làm phase 3 đi"

