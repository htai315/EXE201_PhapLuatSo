# ✅ ADMIN DASHBOARD - PHASE 4: CONTROLLERS

**Status:** ✅ COMPLETED  
**Date:** December 31, 2024  
**Time Spent:** ~1 hour

---

## 📋 OVERVIEW

Phase 4 focuses on creating the REST API layer for the Admin Dashboard. This includes:
- Creating `AdminController` with all REST endpoints
- Implementing role-based access control (ADMIN role required)
- Creating `@CurrentUser` annotation for injecting authenticated user
- Updating `SecurityConfig` to protect admin endpoints
- Enabling method-level security with `@PreAuthorize`

---

## ✅ COMPLETED TASKS

### 1. Created AdminController

**File:** `src/main/java/com/htai/exe201phapluatso/admin/controller/AdminController.java`

**Base Path:** `/api/admin`

**Security:** All endpoints require `ADMIN` role via `@PreAuthorize("hasRole('ADMIN')")`

#### Dashboard Statistics Endpoints

| Method | Endpoint | Description | Parameters |
|--------|----------|-------------|------------|
| GET | `/api/admin/stats` | Get dashboard statistics | - |
| GET | `/api/admin/stats/revenue` | Get revenue chart data | `from`, `to` (dates) |
| GET | `/api/admin/stats/user-growth` | Get user growth chart data | `from`, `to` (dates) |

**Example Response - Dashboard Stats:**
```json
{
  "totalUsers": 150,
  "activeUsers": 145,
  "bannedUsers": 5,
  "newUsersLast30Days": 25,
  "totalSuccessfulPayments": 80,
  "totalRevenue": 15000000,
  "revenueLast30Days": 5000000,
  "totalQuizSets": 200,
  "totalQuizAttempts": 1500,
  "totalChatSessions": 300,
  "totalChatMessages": 2500,
  "totalLegalDocuments": 0,
  "totalLegalArticles": 0
}
```

**Example Response - Revenue Chart:**
```json
[
  {
    "date": "2024-12-01",
    "revenue": 500000,
    "transactionCount": 5
  },
  {
    "date": "2024-12-02",
    "revenue": 750000,
    "transactionCount": 8
  }
]
```

#### User Management Endpoints

| Method | Endpoint | Description | Parameters |
|--------|----------|-------------|------------|
| GET | `/api/admin/users` | Get all users with pagination | `page`, `size`, `search`, `sort`, `direction` |
| GET | `/api/admin/users/{id}` | Get user detail | `id` (path) |
| POST | `/api/admin/users/{id}/ban` | Ban a user | `id` (path), `BanUserRequest` (body) |
| POST | `/api/admin/users/{id}/unban` | Unban a user | `id` (path) |
| DELETE | `/api/admin/users/{id}` | Delete a user (soft) | `id` (path) |

**Example Request - Ban User:**
```json
POST /api/admin/users/123/ban
{
  "reason": "Spam and inappropriate behavior"
}
```

**Example Response - User List:**
```json
{
  "users": [...],
  "currentPage": 0,
  "totalItems": 150,
  "totalPages": 8,
  "hasNext": true,
  "hasPrevious": false
}
```

#### Payment Management Endpoints

| Method | Endpoint | Description | Parameters |
|--------|----------|-------------|------------|
| GET | `/api/admin/payments` | Get all payments with pagination | `page`, `size`, `sort`, `direction` |
| GET | `/api/admin/payments/stats` | Get payment statistics | - |

**Example Response - Payment Stats:**
```json
{
  "totalPayments": 100,
  "successfulPayments": 80,
  "failedPayments": 15,
  "pendingPayments": 5,
  "totalRevenue": 15000000,
  "revenueToday": 500000,
  "revenueThisWeek": 2000000,
  "revenueThisMonth": 5000000,
  "averagePaymentAmount": 187500.0,
  "successRate": 80.0
}
```

#### Activity Logs Endpoints

| Method | Endpoint | Description | Parameters |
|--------|----------|-------------|------------|
| GET | `/api/admin/activity-logs` | Get all activity logs | `page`, `size` |

---

### 2. Created @CurrentUser Annotation

**File:** `src/main/java/com/htai/exe201phapluatso/auth/security/CurrentUser.java`

**Purpose:** Inject authenticated user into controller methods

**Usage:**
```java
@PostMapping("/users/{id}/ban")
public ResponseEntity<?> banUser(
    @PathVariable Long id,
    @RequestBody BanUserRequest request,
    @CurrentUser User adminUser  // ← Injected automatically
) {
    adminService.banUser(id, request.getReason(), adminUser);
    return ResponseEntity.ok().build();
}
```

**Benefits:**
- Clean controller code
- No need to manually get user from SecurityContext
- Type-safe (User object, not String email)
- Reusable across all controllers

---

### 3. Created CurrentUserArgumentResolver

**File:** `src/main/java/com/htai/exe201phapluatso/auth/security/CurrentUserArgumentResolver.java`

**Purpose:** Resolve `@CurrentUser` annotation by extracting user from SecurityContext

**How it works:**
1. Checks if parameter has `@CurrentUser` annotation
2. Gets Authentication from SecurityContext
3. Extracts user ID from `AuthUserPrincipal`
4. Loads User entity from database
5. Returns User object to controller method

**Supports:**
- JWT authentication (via AuthUserPrincipal)
- OAuth2 authentication (fallback to email)

---

### 4. Created WebMvcConfig

**File:** `src/main/java/com/htai/exe201phapluatso/config/WebMvcConfig.java`

**Purpose:** Register custom argument resolvers

**Configuration:**
```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
}
```

---

### 5. Updated SecurityConfig

**File:** `src/main/java/com/htai/exe201phapluatso/auth/security/SecurityConfig.java`

**Changes:**
1. Added `@EnableMethodSecurity(prePostEnabled = true)` to enable `@PreAuthorize`
2. Added admin endpoint protection: `.requestMatchers("/api/admin/**").hasRole("ADMIN")`

**Security Rules:**
```java
// Public endpoints
.requestMatchers("/", "/css/**", "/scripts/**", ...).permitAll()
.requestMatchers("/api/auth/register", "/api/auth/login", ...).permitAll()

// Admin endpoints (ADMIN role required)
.requestMatchers("/api/admin/**").hasRole("ADMIN")

// Protected endpoints (any authenticated user)
.requestMatchers("/api/auth/me").authenticated()

// Everything else requires authentication
.anyRequest().authenticated()
```

---

### 6. Updated CurrentUserArgumentResolver

**Enhancement:** Support for `AuthUserPrincipal`

**Logic:**
```java
if (principal instanceof AuthUserPrincipal) {
    // Get user by ID (more efficient)
    return userRepo.findById(authPrincipal.getUserId()).orElse(null);
} else {
    // Fallback: get by email (for OAuth2)
    return userRepo.findByEmail(email).orElse(null);
}
```

---

## 🔒 SECURITY FEATURES

### Role-Based Access Control (RBAC)

**Three levels of security:**

1. **URL-level security** (SecurityConfig):
   ```java
   .requestMatchers("/api/admin/**").hasRole("ADMIN")
   ```

2. **Controller-level security** (AdminController):
   ```java
   @PreAuthorize("hasRole('ADMIN')")
   public class AdminController { ... }
   ```

3. **Method-level security** (if needed):
   ```java
   @PreAuthorize("hasRole('SUPER_ADMIN')")
   public void deleteUser() { ... }
   ```

### JWT Token with Roles

**Token structure:**
```json
{
  "sub": "admin@example.com",
  "uid": 1,
  "roles": ["USER", "ADMIN"],
  "iat": 1704067200,
  "exp": 1704153600
}
```

**Authorities in SecurityContext:**
- `ROLE_USER`
- `ROLE_ADMIN`

### Authentication Flow

1. User logs in → JWT token generated with roles
2. User sends request with `Authorization: Bearer <token>`
3. `JwtAuthFilter` validates token and extracts roles
4. Roles converted to `GrantedAuthority` with `ROLE_` prefix
5. `UsernamePasswordAuthenticationToken` created with authorities
6. SecurityContext stores authentication
7. Spring Security checks `@PreAuthorize` and URL patterns
8. If authorized → controller method executes
9. `@CurrentUser` resolver injects User entity

---

## 📊 API ENDPOINTS SUMMARY

### Dashboard (3 endpoints)
- ✅ GET `/api/admin/stats` - Dashboard statistics
- ✅ GET `/api/admin/stats/revenue` - Revenue chart
- ✅ GET `/api/admin/stats/user-growth` - User growth chart

### User Management (5 endpoints)
- ✅ GET `/api/admin/users` - List users (pagination, search)
- ✅ GET `/api/admin/users/{id}` - User detail
- ✅ POST `/api/admin/users/{id}/ban` - Ban user
- ✅ POST `/api/admin/users/{id}/unban` - Unban user
- ✅ DELETE `/api/admin/users/{id}` - Delete user

### Payment Management (2 endpoints)
- ✅ GET `/api/admin/payments` - List payments (pagination)
- ✅ GET `/api/admin/payments/stats` - Payment statistics

### Activity Logs (1 endpoint)
- ✅ GET `/api/admin/activity-logs` - List activity logs

**Total: 11 REST endpoints**

---

## 🎯 FEATURES IMPLEMENTED

### Pagination
- Configurable page size (default: 20 for users, 50 for logs)
- Configurable page number (default: 0)
- Sort by any field (default: createdAt)
- Sort direction (ASC/DESC, default: DESC)
- Response includes: `currentPage`, `totalItems`, `totalPages`, `hasNext`, `hasPrevious`

### Search & Filtering
- User search by email or full name (case-insensitive)
- Uses JPA Specification for dynamic queries
- Partial match with `LIKE %search%`

### Date Range Filtering
- Revenue chart: filter by date range
- User growth chart: filter by date range
- Default: last 30 days
- Format: ISO date (YYYY-MM-DD)

### Error Handling
- Service layer throws `RuntimeException` with clear messages
- Spring Boot converts to HTTP 500 (can be improved with @ControllerAdvice)
- Validation errors handled by Spring Validation

### Response Format
- Consistent JSON structure
- Success responses: 200 OK with data
- Error responses: 4xx/5xx with error message
- Pagination metadata included

---

## 🧪 TESTING NOTES

### Manual Testing with Postman/cURL

#### 1. Get Dashboard Stats
```bash
curl -X GET http://localhost:8080/api/admin/stats \
  -H "Authorization: Bearer <admin-token>"
```

#### 2. Get Revenue Chart (last 7 days)
```bash
curl -X GET "http://localhost:8080/api/admin/stats/revenue?from=2024-12-24&to=2024-12-31" \
  -H "Authorization: Bearer <admin-token>"
```

#### 3. List Users (page 0, size 10, search "john")
```bash
curl -X GET "http://localhost:8080/api/admin/users?page=0&size=10&search=john" \
  -H "Authorization: Bearer <admin-token>"
```

#### 4. Get User Detail
```bash
curl -X GET http://localhost:8080/api/admin/users/123 \
  -H "Authorization: Bearer <admin-token>"
```

#### 5. Ban User
```bash
curl -X POST http://localhost:8080/api/admin/users/123/ban \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"reason": "Spam and inappropriate behavior"}'
```

#### 6. Unban User
```bash
curl -X POST http://localhost:8080/api/admin/users/123/unban \
  -H "Authorization: Bearer <admin-token>"
```

#### 7. List Payments
```bash
curl -X GET "http://localhost:8080/api/admin/payments?page=0&size=20" \
  -H "Authorization: Bearer <admin-token>"
```

#### 8. Get Payment Stats
```bash
curl -X GET http://localhost:8080/api/admin/payments/stats \
  -H "Authorization: Bearer <admin-token>"
```

### Test Cases

**Authorization Tests:**
- [ ] Access admin endpoint without token → 401 Unauthorized
- [ ] Access admin endpoint with USER role → 403 Forbidden
- [ ] Access admin endpoint with ADMIN role → 200 OK

**Dashboard Tests:**
- [ ] Get stats with empty database → returns zeros
- [ ] Get stats with sample data → returns correct counts
- [ ] Get revenue chart with date range → returns data
- [ ] Get user growth chart → returns cumulative data

**User Management Tests:**
- [ ] List users with pagination → returns paginated data
- [ ] Search users by email → returns matching users
- [ ] Get user detail → returns full user info
- [ ] Ban user → user becomes inactive
- [ ] Unban user → user becomes active
- [ ] Ban already banned user → error
- [ ] Unban active user → error

**Payment Tests:**
- [ ] List payments → returns paginated data
- [ ] Get payment stats → returns correct calculations

---

## 📁 FILES CREATED/MODIFIED

### Created Files
1. `src/main/java/com/htai/exe201phapluatso/admin/controller/AdminController.java`
2. `src/main/java/com/htai/exe201phapluatso/auth/security/CurrentUser.java`
3. `src/main/java/com/htai/exe201phapluatso/auth/security/CurrentUserArgumentResolver.java`
4. `src/main/java/com/htai/exe201phapluatso/config/WebMvcConfig.java`
5. `ADMIN_DASHBOARD_PHASE4.md` (this file)

### Modified Files
1. `src/main/java/com/htai/exe201phapluatso/auth/security/SecurityConfig.java`
   - Added `@EnableMethodSecurity`
   - Added admin endpoint protection

---

## 🚀 NEXT STEPS

**Phase 5: Frontend HTML**

Create admin dashboard HTML pages:
- `admin/dashboard.html` - Main dashboard with statistics and charts
- `admin/users.html` - User management page
- `admin/payments.html` - Payment management page
- `admin/activity-logs.html` - Activity logs viewer

**Features to implement:**
- Responsive layout with sidebar navigation
- Statistics cards with icons
- Chart.js integration for revenue and user growth
- Data tables with pagination
- Search and filter forms
- Ban/unban modals
- Toast notifications for actions

**Command to continue:**
```
"Hãy làm phase 5 đi"
```

---

## ✅ VALIDATION

All files compiled successfully with no errors:
- ✅ AdminController.java - No diagnostics
- ✅ CurrentUser.java - No diagnostics
- ✅ CurrentUserArgumentResolver.java - No diagnostics
- ✅ WebMvcConfig.java - No diagnostics
- ✅ SecurityConfig.java - No diagnostics

---

**Phase 4 Status:** ✅ COMPLETE  
**Overall Progress:** 57% (4/7 phases complete)
