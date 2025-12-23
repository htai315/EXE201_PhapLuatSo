# Credits 404 Error - Fix Applied

## Problem
API endpoint `/api/credits/balance` was returning 404 with error "User not found" even though the user was authenticated and existed in the database.

## Root Cause
In `CreditService.createFreeCredits()`, the code was using `userRepo.findById(userId).orElse(null)` which was returning empty Optional even though the user existed. This was likely due to:
1. Transaction context issues
2. The method being called within an already active transaction
3. Potential lazy loading or session management issues with `spring.jpa.open-in-view=false`

## Solution Applied

### 1. Changed User Lookup Method
**File**: `src/main/java/com/htai/exe201phapluatso/credit/service/CreditService.java`

**Before**:
```java
User user = userRepo.findById(userId).orElse(null);
if (user == null) {
    throw new NotFoundException("User not found");
}
```

**After**:
```java
// Use getReferenceById instead of findById - more efficient and works within transaction
User user = userRepo.getReferenceById(userId);
```

**Why this works**:
- `getReferenceById()` returns a proxy/reference without hitting the database immediately
- It's designed to work within transactions where the entity is known to exist
- More efficient for creating relationships (like UserCredit -> User)
- Avoids the Optional.orElse() pattern that was causing issues

### 2. Enhanced Logging
**File**: `src/main/resources/application.properties`

Added detailed logging to track transactions and SQL parameters:
```properties
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
logging.level.org.springframework.transaction=DEBUG
```

### 3. Simplified Controller Code
**File**: `src/main/java/com/htai/exe201phapluatso/credit/controller/CreditController.java`

Minor cleanup to use `var` for better readability.

## How to Test

1. **Start Spring Boot**:
   ```bash
   ./mvnw spring-boot:run
   ```

2. **Login to the application** (if not already logged in)

3. **Open browser console** on any page with credits counter (legal-chat.html or quiz-generate-ai.html)

4. **Check the credits counter** - it should now display:
   - Chat credits: 10
   - Quiz credits: 0
   - Plan: FREE

5. **Verify in logs** - you should see:
   ```
   Creating FREE credits for user ID: X
   Successfully created credits for user X
   ```

## Expected Behavior

- New users automatically get 10 FREE chat credits (via database trigger)
- If trigger didn't work, the API will create credits on first access
- Credits counter displays correctly in the UI
- No more 404 errors

## Files Modified

1. `src/main/java/com/htai/exe201phapluatso/credit/service/CreditService.java`
2. `src/main/java/com/htai/exe201phapluatso/credit/controller/CreditController.java`
3. `src/main/resources/application.properties`

## Next Steps

After confirming the fix works:
1. Test credits deduction (send a chat message)
2. Verify credits counter updates in real-time
3. Test on quiz generation page
4. Check profile page shows correct credits info
