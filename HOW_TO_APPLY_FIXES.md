# HƯỚNG DẪN ÁP DỤNG PERFORMANCE FIXES CHO CÁC TRANG KHÁC

## 📋 OVERVIEW

Đã fix 3 critical issues cho `quiz-manager.html`. Bây giờ cần apply cho các trang còn lại.

---

## 🎯 CÁC TRANG CẦN UPDATE

### **Trang có API calls cần fix**:

1. ✅ `quiz-manager.html` - **ĐÃ FIX**
2. ⏳ `my-quizzes.html` - Cần fix
3. ⏳ `quiz-add-question.html` - Cần fix
4. ⏳ `quiz-add-quizset.html` - Cần fix
5. ⏳ `quiz-edit-question.html` - Cần fix
6. ⏳ `quiz-take.html` - Cần fix
7. ⏳ `login.html` - Cần fix (không cần token refresh)
8. ⏳ `register.html` - Cần fix (không cần token refresh)

---

## 📝 CHECKLIST CHO MỖI TRANG

### **Bước 1: Import scripts**

Thêm vào `<head>` hoặc trước `</body>`:

```html
<!-- Error Handler & API Client -->
<script src="/scripts/error-handler.js"></script>
<script src="/scripts/api-client.js"></script>
<script src="/scripts/script.js"></script>
```

**Thứ tự quan trọng**:
1. `error-handler.js` - Phải load đầu tiên
2. `api-client.js` - Load sau error-handler
3. `script.js` - Load cuối cùng

### **Bước 2: Thay thế fetch() bằng API_CLIENT**

#### **GET requests**:

```javascript
// ❌ TRƯỚC
const response = await fetch('/api/quiz-sets', {
    headers: { 'Authorization': 'Bearer ' + token }
});

// ✅ SAU
const response = await API_CLIENT.get('/api/quiz-sets');
```

#### **POST requests**:

```javascript
// ❌ TRƯỚC
const response = await fetch('/api/quiz-sets', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token
    },
    body: JSON.stringify(data)
});

// ✅ SAU
const response = await API_CLIENT.post('/api/quiz-sets', data);
```

#### **PUT requests**:

```javascript
// ❌ TRƯỚC
const response = await fetch('/api/quiz-sets/123', {
    method: 'PUT',
    headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token
    },
    body: JSON.stringify(data)
});

// ✅ SAU
const response = await API_CLIENT.put('/api/quiz-sets/123', data);
```

#### **DELETE requests**:

```javascript
// ❌ TRƯỚC
const response = await fetch('/api/quiz-sets/123', {
    method: 'DELETE',
    headers: { 'Authorization': 'Bearer ' + token }
});

// ✅ SAU
const response = await API_CLIENT.delete('/api/quiz-sets/123');
```

### **Bước 3: Thêm loading states**

Wrap async operations với loading spinner:

```javascript
// ❌ TRƯỚC
async function loadData() {
    try {
        const response = await fetch('/api/quiz-sets');
        // Process data
    } catch (error) {
        console.error(error);
    }
}

// ✅ SAU
async function loadData() {
    ERROR_HANDLER.showLoading(true);
    try {
        const response = await API_CLIENT.get('/api/quiz-sets');
        // Process data
    } catch (error) {
        console.error(error);
    } finally {
        ERROR_HANDLER.showLoading(false);
    }
}
```

### **Bước 4: Xóa token management thủ công**

```javascript
// ❌ TRƯỚC - Không cần nữa
const token = localStorage.getItem('accessToken');

// ✅ SAU - API_CLIENT tự động handle
// Không cần làm gì cả!
```

---

## 🔍 VÍ DỤ CỤ THỂ: my-quizzes.html

### **Trước khi fix**:

```html
<script src="/scripts/script.js"></script>
<script>
    async function loadQuizSets() {
        try {
            const token = localStorage.getItem('accessToken');
            const response = await fetch('/api/quiz-sets/my', {
                headers: { 'Authorization': 'Bearer ' + token }
            });
            
            if (!response.ok) {
                throw new Error('Failed to load');
            }
            
            const data = await response.json();
            renderQuizSets(data);
        } catch (error) {
            console.error(error);
            alert('Error loading quiz sets');
        }
    }
</script>
```

### **Sau khi fix**:

```html
<script src="/scripts/error-handler.js"></script>
<script src="/scripts/api-client.js"></script>
<script src="/scripts/script.js"></script>
<script>
    async function loadQuizSets() {
        ERROR_HANDLER.showLoading(true);
        try {
            const response = await API_CLIENT.get('/api/quiz-sets/my');
            
            if (!response.ok) {
                throw new Error('Failed to load');
            }
            
            const data = await response.json();
            renderQuizSets(data);
        } catch (error) {
            console.error(error);
            ERROR_HANDLER.showErrorAlert('Không thể tải danh sách bộ đề');
        } finally {
            ERROR_HANDLER.showLoading(false);
        }
    }
</script>
```

---

## 🚫 TRANG KHÔNG CẦN TOKEN REFRESH

Các trang này **KHÔNG** cần `API_CLIENT` (vì chưa login):

1. `login.html` - Chỉ cần `error-handler.js`
2. `register.html` - Chỉ cần `error-handler.js`
3. `index.html` - Chỉ cần `error-handler.js` (nếu có API calls)

**Ví dụ login.html**:

```html
<!-- Chỉ import error-handler -->
<script src="/scripts/error-handler.js"></script>
<script>
    async function login(email, password) {
        ERROR_HANDLER.showLoading(true);
        try {
            // Dùng fetch bình thường (không cần token)
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });
            
            if (!response.ok) {
                throw new Error('Login failed');
            }
            
            const data = await response.json();
            localStorage.setItem('accessToken', data.accessToken);
            localStorage.setItem('refreshToken', data.refreshToken);
            
            window.location.href = '/html/my-quizzes.html';
        } catch (error) {
            ERROR_HANDLER.showErrorAlert('Đăng nhập thất bại');
        } finally {
            ERROR_HANDLER.showLoading(false);
        }
    }
</script>
```

---

## ✅ TESTING CHECKLIST

Sau khi apply fixes, test các scenarios sau:

### **1. Token Refresh**
- [ ] Login vào app
- [ ] Đợi 15 phút (access token hết hạn)
- [ ] Thực hiện action (load data, delete, etc.)
- [ ] **Expected**: Action thành công, không bị logout

### **2. Error Handling**
- [ ] Tắt internet
- [ ] Thực hiện action
- [ ] **Expected**: Hiển thị error alert, không crash

### **3. Loading States**
- [ ] Thực hiện action
- [ ] **Expected**: Hiển thị loading spinner

### **4. Token Expired**
- [ ] Xóa refresh token: `localStorage.removeItem('refreshToken')`
- [ ] Thực hiện action
- [ ] **Expected**: Redirect to login page

---

## 🐛 TROUBLESHOOTING

### **Lỗi: "API_CLIENT is not defined"**

**Nguyên nhân**: Chưa import `api-client.js`

**Giải pháp**:
```html
<script src="/scripts/api-client.js"></script>
```

### **Lỗi: "ERROR_HANDLER is not defined"**

**Nguyên nhân**: Chưa import `error-handler.js`

**Giải pháp**:
```html
<script src="/scripts/error-handler.js"></script>
```

### **Lỗi: Scripts load sai thứ tự**

**Nguyên nhân**: Import scripts không đúng thứ tự

**Giải pháp**: Đảm bảo thứ tự:
```html
<script src="/scripts/error-handler.js"></script>  <!-- 1. Đầu tiên -->
<script src="/scripts/api-client.js"></script>     <!-- 2. Thứ hai -->
<script src="/scripts/script.js"></script>         <!-- 3. Cuối cùng -->
```

### **Token refresh không hoạt động**

**Nguyên nhân**: Backend refresh token API có vấn đề

**Debug**:
1. Mở DevTools Console
2. Xem logs: "Access token expired, attempting refresh..."
3. Check Network tab: `/api/auth/refresh` request
4. Verify response có `accessToken` và `refreshToken`

---

## 📊 PROGRESS TRACKING

| Trang | Status | Notes |
|-------|--------|-------|
| quiz-manager.html | ✅ Done | Reference implementation |
| my-quizzes.html | ✅ Done | Applied all fixes |
| quiz-add-question.html | ✅ Done | Applied all fixes |
| quiz-add-quizset.html | ✅ Done | Applied all fixes |
| quiz-edit-question.html | ✅ Done | Applied all fixes |
| quiz-take.html | ✅ Done | Applied all fixes |
| login.html | ⏳ Todo | Only error-handler (optional) |
| register.html | ⏳ Todo | Only error-handler (optional) |

**Status**: 6/6 quiz pages completed (100%) ✅

---

## 🎯 PRIORITY

**High Priority** (Làm trước):
1. `my-quizzes.html` - Trang chính
2. `quiz-take.html` - Trang thi
3. `quiz-add-question.html` - Trang thêm câu hỏi

**Medium Priority**:
4. `quiz-edit-question.html`
5. `quiz-add-quizset.html`

**Low Priority**:
6. `login.html`
7. `register.html`

---

**Người hướng dẫn**: Kiro AI  
**Ngày**: 19/12/2024
