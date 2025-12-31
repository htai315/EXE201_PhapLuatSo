# API_CLIENT Migration Summary

## Vấn Đề
Khi migrate từ `fetch` sang `API_CLIENT`, nhiều chỗ vẫn gọi `.json()` trên kết quả - nhưng `API_CLIENT` đã return data (đã parse JSON) rồi!

## Pattern Lỗi
```javascript
// ❌ SAI
const response = await API_CLIENT.get('/api/...');
const data = await response.json(); // Lỗi! response đã là data

// ✅ ĐÚNG
const data = await API_CLIENT.get('/api/...');
// data đã là object, dùng trực tiếp
```

## Files Đã Fix

### 1. login.html ✅
**Trước:**
```javascript
const response = await API_CLIENT.post("/api/auth/login", { email, password });
const data = await response.json();
```

**Sau:**
```javascript
const data = await API_CLIENT.post("/api/auth/login", { email, password });
```

### 2. quiz-history.js ✅
**Trước:**
```javascript
const setsRes = await API_CLIENT.get(`${API_BASE}/my`);
if (!setsRes.ok) throw new Error('...');
const quizSets = await setsRes.json();

const historyRes = await API_CLIENT.get(`${API_BASE}/${set.id}/exam/history`);
if (historyRes.ok) {
    const historyData = await historyRes.json();
}
```

**Sau:**
```javascript
const quizSets = await API_CLIENT.get(`${API_BASE}/my`);

const historyData = await API_CLIENT.get(`${API_BASE}/${set.id}/exam/history`);
if (historyData.attempts && historyData.attempts.length > 0) {
    // ...
}
```

## Files Đã Kiểm Tra - OK ✅

Các files sau dùng `API_CLIENT` đúng cách (không gọi `.json()`):

1. **reset-password.html** ✅
   ```javascript
   const response = await API_CLIENT.post(...);
   showToast(response.message || '...'); // Dùng trực tiếp
   ```

2. **register.html** ✅
   ```javascript
   const response = await API_CLIENT.post(...);
   Toast.success('...'); // Không cần .json()
   ```

3. **forgot-password.html** ✅
   ```javascript
   const response = await API_CLIENT.post(...);
   showToast(response.message || '...'); // Dùng trực tiếp
   ```

4. **payment-history.html** ✅
   ```javascript
   const response = await API_CLIENT.get(...);
   allPayments = Array.isArray(response) ? response : (response.payments || []);
   ```

5. **quiz-take.html** ✅
   ```javascript
   const data = await API_CLIENT.get(...);
   examQuestions = data.questions || [];
   ```

6. **quiz-manager.html** ✅
   ```javascript
   await API_CLIENT.delete(...);
   const questions = await API_CLIENT.get(...);
   ```

7. **quiz-edit-question.html** ✅
8. **quiz-add-quizset.html** ✅
9. **quiz-add-question.html** ✅
10. **my-quizzes.html** ✅

## Kết Luận

✅ **Đã fix:** 2 files có lỗi
✅ **Đã kiểm tra:** 10+ files khác - tất cả OK

Tất cả các trang giờ đã dùng `API_CLIENT` đúng cách và có auto-refresh token!
