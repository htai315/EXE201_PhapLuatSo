# 📡 API Documentation

REST API documentation cho Pháp Luật Số platform.

## 🔐 Authentication

Tất cả API (trừ login/register) yêu cầu JWT token trong header:
```
Authorization: Bearer <access_token>
```

---

## 1. Authentication APIs

### 1.1 Register
**Endpoint:** `POST /api/auth/register`

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "fullName": "Nguyen Van A"
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "fullName": "Nguyen Van A",
    "role": "STUDENT",
    "planCode": "FREE"
  }
}
```

### 1.2 Login
**Endpoint:** `POST /api/auth/login`

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:** Same as Register

### 1.3 Refresh Token
**Endpoint:** `POST /api/auth/refresh`

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "new_access_token",
  "refreshToken": "new_refresh_token"
}
```

### 1.4 Get Current User
**Endpoint:** `GET /api/auth/me`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "id": 1,
  "email": "user@example.com",
  "fullName": "Nguyen Van A",
  "role": "STUDENT",
  "planCode": "STUDENT",
  "credits": {
    "chatCredits": 50,
    "quizGenCredits": 10
  }
}
```

---

## 2. Quiz APIs

### 2.1 Get My Quiz Sets
**Endpoint:** `GET /api/quiz-sets/my`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "title": "Luật Dân Sự 2015",
    "description": "Bộ đề về Luật Dân Sự",
    "questionCount": 20,
    "createdAt": "2024-12-31T10:00:00",
    "isPublic": false
  }
]
```

### 2.2 Create Quiz Set
**Endpoint:** `POST /api/quiz-sets`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "title": "Luật Hình Sự 2015",
  "description": "Bộ đề về Luật Hình Sự",
  "isPublic": false
}
```

**Response:** `201 Created`
```json
{
  "id": 2,
  "title": "Luật Hình Sự 2015",
  "description": "Bộ đề về Luật Hình Sự",
  "questionCount": 0,
  "createdAt": "2024-12-31T11:00:00"
}
```

**Note:** Tạo quiz thủ công là MIỄN PHÍ (không tốn credit)

### 2.3 Get Quiz Set Details
**Endpoint:** `GET /api/quiz-sets/{id}`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "id": 1,
  "title": "Luật Dân Sự 2015",
  "description": "Bộ đề về Luật Dân Sự",
  "questionCount": 20,
  "questions": [
    {
      "id": 1,
      "question": "Điều 1 Luật Dân Sự quy định về?",
      "optionA": "Phạm vi điều chỉnh",
      "optionB": "Nguyên tắc",
      "optionC": "Đối tượng",
      "optionD": "Hiệu lực",
      "correctAnswer": "A"
    }
  ]
}
```

### 2.4 Add Question
**Endpoint:** `POST /api/quiz-sets/{id}/questions`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "question": "Điều 2 Luật Dân Sự quy định về?",
  "optionA": "Nguyên tắc bình đẳng",
  "optionB": "Nguyên tắc tự do",
  "optionC": "Nguyên tắc thiện chí",
  "optionD": "Tất cả đều đúng",
  "correctAnswer": "D"
}
```

**Response:** `201 Created`

### 2.5 Start Exam
**Endpoint:** `POST /api/quiz-sets/{id}/exam/start`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "attemptId": 1,
  "quizSetId": 1,
  "questions": [
    {
      "id": 1,
      "question": "...",
      "optionA": "...",
      "optionB": "...",
      "optionC": "...",
      "optionD": "..."
    }
  ],
  "startedAt": "2024-12-31T12:00:00"
}
```

### 2.6 Submit Exam
**Endpoint:** `POST /api/quiz-sets/{id}/exam/submit`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "attemptId": 1,
  "answers": {
    "1": "A",
    "2": "D",
    "3": "B"
  }
}
```

**Response:** `200 OK`
```json
{
  "attemptId": 1,
  "totalQuestions": 20,
  "correctCount": 18,
  "scorePercent": 90,
  "results": [
    {
      "questionId": 1,
      "userAnswer": "A",
      "correctAnswer": "A",
      "isCorrect": true
    }
  ]
}
```

### 2.7 Get Exam History
**Endpoint:** `GET /api/quiz-sets/{id}/exam/history`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "quizSetId": 1,
  "quizSetTitle": "Luật Dân Sự 2015",
  "attempts": [
    {
      "attemptId": 1,
      "totalQuestions": 20,
      "correctCount": 18,
      "scorePercent": 90,
      "finishedAt": "2024-12-31T12:30:00"
    }
  ]
}
```

---

## 3. AI APIs

### 3.1 Generate Quiz from Document
**Endpoint:** `POST /api/ai/quiz/generate-from-document`

**Headers:** 
- `Authorization: Bearer <token>`
- `Content-Type: multipart/form-data`

**Request Body (Form Data):**
- `file`: PDF or DOCX file
- `quizSetName`: String
- `description`: String (optional)
- `questionCount`: Integer (default: 10)

**Response:** `200 OK`
```json
{
  "quizSetId": 3,
  "quizSetName": "Luật Lao Động 2019",
  "totalQuestions": 10,
  "questions": [
    {
      "question": "...",
      "optionA": "...",
      "optionB": "...",
      "optionC": "...",
      "optionD": "...",
      "correctAnswer": "A"
    }
  ]
}
```

**Cost:** 1 credit per quiz set

**Error:** `402 Payment Required`
```json
{
  "error": "Bạn đã hết lượt AI tạo đề. Vui lòng nâng cấp lên gói STUDENT.",
  "code": "INSUFFICIENT_CREDITS"
}
```

---

## 4. Chat APIs

### 4.1 Send Message (New Session)
**Endpoint:** `POST /api/chat/sessions/messages`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "question": "Điều 1 Luật Dân Sự quy định về gì?"
}
```

**Response:** `200 OK`
```json
{
  "sessionId": 1,
  "userMessage": {
    "content": "Điều 1 Luật Dân Sự quy định về gì?",
    "timestamp": "2024-12-31T13:00:00"
  },
  "assistantMessage": {
    "content": "Điều 1 Luật Dân Sự 2015 quy định về phạm vi điều chỉnh...",
    "citations": [
      {
        "documentName": "Luật Dân Sự 2015",
        "articleNumber": "Điều 1",
        "content": "..."
      }
    ],
    "timestamp": "2024-12-31T13:00:05"
  }
}
```

**Cost:** 1 credit per message

### 4.2 Send Message (Existing Session)
**Endpoint:** `POST /api/chat/sessions/{sessionId}/messages`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "question": "Còn Điều 2 thì sao?"
}
```

**Response:** Same as 4.1

### 4.3 Get Chat Sessions
**Endpoint:** `GET /api/chat/sessions?page=0&size=20&search=dân sự`

**Headers:** `Authorization: Bearer <token>`

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)
- `search`: Search query (optional)

**Response:** `200 OK`
```json
{
  "sessions": [
    {
      "id": 1,
      "title": "Điều 1 Luật Dân Sự quy định về gì?",
      "lastMessage": "Điều 1 quy định về phạm vi...",
      "messageCount": 5,
      "createdAt": "2024-12-31T13:00:00",
      "updatedAt": "2024-12-31T13:10:00"
    }
  ],
  "hasMore": true
}
```

### 4.4 Get Session Messages
**Endpoint:** `GET /api/chat/sessions/{sessionId}/messages`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
[
  {
    "role": "user",
    "content": "Điều 1 Luật Dân Sự quy định về gì?",
    "timestamp": "2024-12-31T13:00:00"
  },
  {
    "role": "assistant",
    "content": "Điều 1 quy định về...",
    "citations": [...],
    "timestamp": "2024-12-31T13:00:05"
  }
]
```

### 4.5 Delete Session
**Endpoint:** `DELETE /api/chat/sessions/{sessionId}`

**Headers:** `Authorization: Bearer <token>`

**Response:** `204 No Content`

---

## 5. Payment APIs

### 5.1 Create Payment
**Endpoint:** `POST /api/payment/create`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "planCode": "STUDENT"
}
```

**Response:** `200 OK`
```json
{
  "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=...",
  "orderId": "ORDER123456",
  "amount": 99000
}
```

**Plans:**
- `FREE`: 0đ (0 credits)
- `STUDENT`: 99,000đ (100 credits)
- `PREMIUM`: 199,000đ (300 credits)

### 5.2 VNPay Return
**Endpoint:** `GET /api/payment/vnpay-return`

**Query Parameters:** (Từ VNPay)
- `vnp_ResponseCode`: Response code
- `vnp_TransactionNo`: Transaction number
- `vnp_SecureHash`: Signature
- ... other VNPay params

**Response:** Redirect to `/html/payment-result.html?status=success`

---

## 6. Credit APIs

### 6.1 Get Credits
**Endpoint:** `GET /api/credits/balance`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "chatCredits": 50,
  "quizGenCredits": 10,
  "totalCredits": 60
}
```

### 6.2 Get Credit Transactions
**Endpoint:** `GET /api/credits/transactions?page=0&size=20`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "transactions": [
    {
      "id": 1,
      "creditType": "CHAT",
      "amount": -1,
      "description": "AI Chat message",
      "createdAt": "2024-12-31T13:00:00"
    },
    {
      "id": 2,
      "creditType": "QUIZ_GEN",
      "amount": -1,
      "description": "AI Quiz Generation",
      "createdAt": "2024-12-31T12:00:00"
    }
  ],
  "hasMore": false
}
```

---

## 📝 Error Responses

### 400 Bad Request
```json
{
  "error": "Validation failed",
  "fields": {
    "email": "Email không hợp lệ",
    "password": "Mật khẩu phải có ít nhất 6 ký tự"
  }
}
```

### 401 Unauthorized
```json
{
  "error": "Token không hợp lệ hoặc đã hết hạn"
}
```

### 402 Payment Required
```json
{
  "error": "Bạn đã hết lượt AI tạo đề. Vui lòng nâng cấp lên gói STUDENT.",
  "code": "INSUFFICIENT_CREDITS"
}
```

### 403 Forbidden
```json
{
  "error": "Bạn không có quyền truy cập tài nguyên này"
}
```

### 404 Not Found
```json
{
  "error": "Không tìm thấy quiz set với ID: 999"
}
```

### 500 Internal Server Error
```json
{
  "error": "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau"
}
```

---

## 🔒 Rate Limiting

- **AI APIs**: 10 requests/minute per user
- **Other APIs**: 100 requests/minute per user

Khi vượt limit:
```json
{
  "error": "Too many requests. Please try again later.",
  "retryAfter": 60
}
```

---

## 📊 Response Codes

| Code | Meaning |
|------|---------|
| 200 | Success |
| 201 | Created |
| 204 | No Content |
| 400 | Bad Request |
| 401 | Unauthorized |
| 402 | Payment Required |
| 403 | Forbidden |
| 404 | Not Found |
| 500 | Internal Server Error |

---

## 🧪 Testing with Postman

Import collection: [Download Postman Collection](postman_collection.json)

### Setup Environment
```
BASE_URL: http://localhost:8080
ACCESS_TOKEN: <your_token>
```

### Test Flow
1. Register/Login → Get token
2. Create quiz set
3. Add questions
4. Start exam
5. Submit exam
6. Check results

---

## 📞 Support

Nếu có vấn đề với API, vui lòng tạo issue trên GitHub.
