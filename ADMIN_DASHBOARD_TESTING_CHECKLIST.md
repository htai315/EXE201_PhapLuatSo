# 🧪 ADMIN DASHBOARD - TESTING CHECKLIST

**Date:** December 31, 2024  
**Status:** Ready for Testing

---

## 📋 PRE-TESTING SETUP

### Database Setup
- [ ] Run Flyway migration V4__add_admin_features.sql
- [ ] Verify `users` table has new columns: `is_active`, `ban_reason`, `banned_at`, `banned_by`
- [ ] Verify `admin_activity_logs` table exists
- [ ] Verify `user_roles` and `roles` tables exist
- [ ] Insert ADMIN role: `INSERT INTO roles (name) VALUES ('ADMIN');`
- [ ] Assign ADMIN role to test user: `INSERT INTO user_roles (user_id, role_id) VALUES (1, 2);`

### Application Setup
- [ ] Start Spring Boot application
- [ ] Verify no compilation errors
- [ ] Check logs for successful startup
- [ ] Verify all endpoints registered

---

## 🔐 AUTHENTICATION & AUTHORIZATION

### Login & Access
- [ ] Login with regular user → should NOT see admin menu
- [ ] Login with admin user → should see admin menu
- [ ] Access `/html/admin/dashboard.html` without token → redirect to login
- [ ] Access `/html/admin/dashboard.html` with USER role → 403 Forbidden
- [ ] Access `/html/admin/dashboard.html` with ADMIN role → Success

### JWT Token
- [ ] Token includes roles in claims
- [ ] Token auto-refreshes before expiry
- [ ] Expired token redirects to login
- [ ] Logout clears tokens

---

## 📊 DASHBOARD PAGE

### Statistics Cards
- [ ] Total Users displays correct count
- [ ] Active Users displays correct count
- [ ] New Users (30 days) displays correct count
- [ ] Banned Users displays correct count
- [ ] Total Revenue displays correct amount (formatted)
- [ ] Revenue 30 Days displays correct amount
- [ ] Total Payments displays correct count
- [ ] Quiz Sets displays correct count
- [ ] Quiz Attempts displays correct count
- [ ] Chat Sessions displays correct count
- [ ] Chat Messages displays correct count

### Revenue Chart
- [ ] Chart loads successfully
- [ ] Shows last 30 days of data
- [ ] X-axis shows dates (DD/MM format)
- [ ] Y-axis shows currency (VNĐ)
- [ ] Tooltip shows formatted currency
- [ ] Line is smooth with gradient fill
- [ ] Purple color scheme

### User Growth Chart
- [ ] Chart loads successfully
- [ ] Shows last 30 days of data
- [ ] Two lines: New Users (green) and Total Users (purple)
- [ ] Legend displays correctly
- [ ] Data is accurate

### UI/UX
- [ ] Sidebar navigation works
- [ ] Active menu item highlighted
- [ ] Admin name displays in top bar
- [ ] Logout button works
- [ ] Mobile sidebar toggle works
- [ ] Responsive on mobile/tablet/desktop

---

## 👥 USERS MANAGEMENT PAGE

### User List
- [ ] Table loads with users
- [ ] Pagination works (Previous/Next)
- [ ] Page numbers display correctly
- [ ] Total users count displays
- [ ] User data displays correctly (ID, email, name, provider)
- [ ] Credits display (Chat + Quiz Gen)
- [ ] Status badges display correctly (Active, Banned, Disabled)
- [ ] Created date formatted correctly

### Search & Filter
- [ ] Search by email works
- [ ] Search by name works
- [ ] Search on Enter key works
- [ ] Filter by status works (active/banned)
- [ ] Clear search resets results

### View User Detail
- [ ] Click eye icon opens modal
- [ ] Modal shows basic info (ID, email, name, provider)
- [ ] Modal shows email verified status
- [ ] Modal shows enabled/active status
- [ ] Modal shows ban info (if banned)
- [ ] Modal shows credits (chat, quiz gen, expiry)
- [ ] Modal shows statistics (payments, revenue, quizzes, chats)
- [ ] Close modal works

### Ban User
- [ ] Click ban icon opens modal
- [ ] Modal shows user email
- [ ] Reason textarea is required
- [ ] Submit without reason shows warning
- [ ] Submit with reason bans user successfully
- [ ] Toast notification shows success
- [ ] User status changes to "Banned"
- [ ] Ban button changes to Unban button
- [ ] Activity log created

### Unban User
- [ ] Click unban icon shows confirmation
- [ ] Confirm unbans user successfully
- [ ] Toast notification shows success
- [ ] User status changes to "Active"
- [ ] Unban button changes to Ban button
- [ ] Activity log created

### Delete User
- [ ] Click delete icon shows confirmation
- [ ] Confirm deletes user (soft delete)
- [ ] Toast notification shows success
- [ ] User is disabled (not hard deleted)
- [ ] Activity log created

### Error Handling
- [ ] Network error shows error toast
- [ ] Invalid user ID shows error
- [ ] Already banned user shows error
- [ ] Already active user (unban) shows error

---

## 💳 PAYMENTS MANAGEMENT PAGE

### Payment Statistics
- [ ] Total Payments displays correct count
- [ ] Successful Payments displays correct count
- [ ] Failed Payments displays correct count
- [ ] Pending Payments displays correct count
- [ ] Total Revenue displays correct amount
- [ ] Revenue Today displays correct amount
- [ ] Revenue This Week displays correct amount
- [ ] Revenue This Month displays correct amount

### Payment List
- [ ] Table loads with payments
- [ ] Pagination works
- [ ] Total payments count displays
- [ ] Payment data displays correctly (ID, Order ID, User, Plan, Amount)
- [ ] Status badges display correctly (Success, Failed, Pending)
- [ ] Payment method displays
- [ ] Created date formatted correctly
- [ ] User name and email display

### UI/UX
- [ ] Sidebar navigation works
- [ ] Active menu item highlighted
- [ ] Responsive design works

---

## 📜 ACTIVITY LOGS PAGE

### Activity Logs List
- [ ] Table loads with logs
- [ ] Pagination works (50 per page)
- [ ] Total logs count displays
- [ ] Log data displays correctly (ID, Admin, Action, Target, Description, Time)
- [ ] Admin name and email display
- [ ] Action badges display correctly (Ban User, Unban User, Delete User)
- [ ] Target type and ID display
- [ ] Description displays
- [ ] Timestamp formatted correctly

### Logs Content
- [ ] Ban user action logged
- [ ] Unban user action logged
- [ ] Delete user action logged
- [ ] Logs sorted by newest first

---

## 📱 RESPONSIVE DESIGN

### Desktop (≥992px)
- [ ] Sidebar always visible (260px)
- [ ] Main content margin-left: 260px
- [ ] All statistics cards in grid
- [ ] Tables display full width
- [ ] Charts display correctly

### Tablet (768px - 991px)
- [ ] Sidebar hidden by default
- [ ] Toggle button visible
- [ ] Sidebar slides in from left
- [ ] Statistics cards in 2 columns
- [ ] Tables scrollable

### Mobile (<768px)
- [ ] Sidebar overlay
- [ ] Toggle button visible
- [ ] Statistics cards in 1 column
- [ ] Tables scrollable
- [ ] Smaller fonts and padding
- [ ] Touch-friendly buttons

---

## 🔒 SECURITY TESTING

### Authorization
- [ ] Non-admin users cannot access admin pages
- [ ] Non-admin users get 403 on admin API calls
- [ ] Admin users can access all admin features

### XSS Prevention
- [ ] User input escaped in tables (email, name)
- [ ] Ban reason escaped in display
- [ ] Description escaped in activity logs
- [ ] No script injection possible

### CSRF Protection
- [ ] POST requests require valid token
- [ ] DELETE requests require valid token

---

## 🐛 ERROR SCENARIOS

### Network Errors
- [ ] API timeout shows error message
- [ ] Network offline shows error message
- [ ] Server error (500) shows error message

### Invalid Data
- [ ] Invalid user ID shows error
- [ ] Invalid page number handled gracefully
- [ ] Empty search results show message

### Edge Cases
- [ ] Zero users shows empty state
- [ ] Zero payments shows empty state
- [ ] Zero logs shows empty state
- [ ] Very long email/name truncated or wrapped
- [ ] Special characters in search handled

---

## ⚡ PERFORMANCE TESTING

### Load Time
- [ ] Dashboard loads in < 2 seconds
- [ ] User list loads in < 2 seconds
- [ ] Payment list loads in < 2 seconds
- [ ] Charts render in < 1 second

### Pagination
- [ ] Page navigation is instant
- [ ] No lag when switching pages
- [ ] Smooth scrolling

### Charts
- [ ] Charts render smoothly
- [ ] No lag on hover
- [ ] Responsive to window resize

---

## 🎨 UI/UX TESTING

### Visual Design
- [ ] Colors consistent (purple gradient theme)
- [ ] Fonts consistent (Inter)
- [ ] Icons display correctly (Bootstrap Icons)
- [ ] Spacing consistent
- [ ] Shadows and borders consistent

### Interactions
- [ ] Hover effects work on buttons
- [ ] Hover effects work on table rows
- [ ] Hover effects work on cards
- [ ] Active states display correctly
- [ ] Focus states visible for accessibility

### Feedback
- [ ] Toast notifications display correctly
- [ ] Toast auto-dismiss after 3 seconds
- [ ] Loading spinners show during API calls
- [ ] Success messages clear and helpful
- [ ] Error messages clear and actionable

---

## 📝 DOCUMENTATION TESTING

### Code Documentation
- [ ] All Java classes have Javadoc
- [ ] All methods have comments
- [ ] Complex logic explained

### User Documentation
- [ ] README updated with admin features
- [ ] API documentation includes admin endpoints
- [ ] Setup guide includes admin role creation

---

## ✅ FINAL CHECKS

### Code Quality
- [ ] No console errors in browser
- [ ] No compilation errors
- [ ] No linting errors
- [ ] Code follows project conventions

### Database
- [ ] Migration runs successfully
- [ ] No data loss
- [ ] Indexes created
- [ ] Foreign keys working

### Deployment Ready
- [ ] All environment variables set
- [ ] Database connection configured
- [ ] CORS configured if needed
- [ ] Production build tested

---

## 🚀 SIGN-OFF

**Tested By:** _________________  
**Date:** _________________  
**Status:** ☐ Pass ☐ Fail  
**Notes:** _________________

---

## 📊 TEST RESULTS SUMMARY

| Category | Total Tests | Passed | Failed | Notes |
|----------|-------------|--------|--------|-------|
| Authentication | 8 | - | - | |
| Dashboard | 15 | - | - | |
| Users Management | 20 | - | - | |
| Payments Management | 12 | - | - | |
| Activity Logs | 8 | - | - | |
| Responsive Design | 12 | - | - | |
| Security | 6 | - | - | |
| Error Handling | 9 | - | - | |
| Performance | 7 | - | - | |
| UI/UX | 12 | - | - | |
| **TOTAL** | **109** | **-** | **-** | |

---

**Testing Complete:** ☐ Yes ☐ No  
**Ready for Production:** ☐ Yes ☐ No
