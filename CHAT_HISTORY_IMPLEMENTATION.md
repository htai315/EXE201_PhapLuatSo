# Chat History Implementation - Complete ✅

## Overview
Đã hoàn thành tính năng lưu lịch sử trò chuyện cho chatbot pháp luật. Người dùng có thể xem lại các cuộc hội thoại cũ và tiếp tục chat từ bất kỳ session nào.

## Database Schema (Migration V9)

### Tables Created:
1. **chat_sessions** - Lưu các cuộc hội thoại
   - id, user_id, title, created_at, updated_at
   - CASCADE DELETE khi xóa user

2. **chat_messages** - Lưu tin nhắn trong mỗi session
   - id, session_id, role (USER/ASSISTANT), content, created_at
   - CASCADE DELETE khi xóa session

3. **chat_message_citations** - Liên kết tin nhắn với điều luật trích dẫn
   - id, message_id, article_id
   - Many-to-many relationship

## Backend Implementation

### Entities
- `ChatSession.java` - Entity cho session
- `ChatMessage.java` - Entity cho message với citations

### DTOs
- `ChatSessionDTO` - Thông tin session cho sidebar
- `ChatMessageDTO` - Tin nhắn với citations
- `SendMessageRequest` - Request gửi tin nhắn
- `SendMessageResponse` - Response trả về sau khi gửi
- `CitationDTO` - Thông tin điều luật trích dẫn

### Repositories
- `ChatSessionRepo` - Query sessions theo user
- `ChatMessageRepo` - Query messages với citations

### Service Layer
`ChatHistoryService.java` cung cấp:
- `getUserSessions()` - Lấy danh sách sessions
- `getSessionMessages()` - Lấy tin nhắn trong session
- `sendMessage()` - Gửi tin nhắn (tạo session mới hoặc tiếp tục)
- `deleteSession()` - Xóa session

### REST API Endpoints
`ChatHistoryController.java`:
- `GET /api/chat/sessions` - Lấy tất cả sessions của user
- `GET /api/chat/sessions/{id}/messages` - Lấy tin nhắn trong session
- `POST /api/chat/sessions/messages` - Tạo session mới và gửi tin nhắn
- `POST /api/chat/sessions/{id}/messages` - Gửi tin nhắn trong session có sẵn
- `DELETE /api/chat/sessions/{id}` - Xóa session

## Frontend Implementation

### UI Layout
- **Sidebar (trái)**: Hiển thị lịch sử chat, nhóm theo ngày
  - Hôm nay
  - Hôm qua
  - Tuần này
  - Cũ hơn
- **Main Area (phải)**: Khu vực chat chính

### JavaScript Functions
- `loadChatSessions()` - Load danh sách sessions vào sidebar
- `loadSession(sessionId)` - Load một session cụ thể
- `startNewChat()` - Bắt đầu chat mới
- `groupSessionsByDate()` - Nhóm sessions theo ngày
- `addMessage()` - Thêm tin nhắn vào UI
- `renderChatSessions()` - Render sidebar

### Features
1. ✅ Tự động tạo session mới khi gửi tin nhắn đầu tiên
2. ✅ Lưu tất cả tin nhắn USER và ASSISTANT
3. ✅ Hiển thị citations (điều luật trích dẫn) trong mỗi câu trả lời
4. ✅ Sidebar hiển thị lịch sử, nhóm theo ngày
5. ✅ Click vào session để load lại cuộc hội thoại
6. ✅ Nút "Chat mới" để bắt đầu session mới
7. ✅ Active state cho session hiện tại

## How It Works

### Flow khi gửi tin nhắn:
1. User nhập câu hỏi và submit
2. Frontend gửi POST request đến:
   - `/api/chat/sessions/messages` (nếu chưa có session)
   - `/api/chat/sessions/{id}/messages` (nếu đã có session)
3. Backend:
   - Tạo session mới (nếu cần) với title = 50 ký tự đầu của câu hỏi
   - Lưu tin nhắn USER
   - Gọi `LegalChatService.chat()` để generate câu trả lời
   - Lưu tin nhắn ASSISTANT
   - Trả về response với sessionId và cả 2 messages
4. Frontend:
   - Update currentSessionId
   - Hiển thị tin nhắn USER và ASSISTANT
   - Reload sidebar để cập nhật danh sách sessions

### Flow khi load session cũ:
1. User click vào session trong sidebar
2. Frontend gọi `GET /api/chat/sessions/{id}/messages`
3. Backend trả về tất cả messages trong session
4. Frontend clear chat area và render lại tất cả messages

## Testing Checklist

Sau khi restart application, test các tính năng sau:

### ✅ Database Migration
- [ ] Migration V9 chạy thành công
- [ ] 3 tables được tạo: chat_sessions, chat_messages, chat_message_citations

### ✅ Chat Functionality
- [ ] Gửi tin nhắn đầu tiên tạo session mới
- [ ] Session xuất hiện trong sidebar
- [ ] Tin nhắn được lưu và hiển thị đúng
- [ ] Citations hiển thị đầy đủ

### ✅ Session Management
- [ ] Click vào session cũ load lại đúng cuộc hội thoại
- [ ] Nút "Chat mới" tạo session mới
- [ ] Sessions được nhóm theo ngày (Hôm nay, Hôm qua, etc.)
- [ ] Active state hiển thị đúng session hiện tại

### ✅ Data Persistence
- [ ] Refresh trang vẫn giữ được lịch sử
- [ ] Logout và login lại vẫn thấy lịch sử cũ
- [ ] Mỗi user chỉ thấy sessions của mình

## Notes

### Citations Handling
- Citations KHÔNG được lưu vào database (simplified approach)
- Citations chỉ được trả về trong API response
- Khi load session cũ, citations sẽ rỗng (có thể cải thiện sau)

### Performance
- Sidebar load tất cả sessions (có thể thêm pagination sau)
- Messages load toàn bộ khi click session (có thể thêm lazy loading sau)

### Security
- Mỗi user chỉ có thể xem/xóa sessions của mình
- `ChatHistoryService` check ownership trước khi thao tác

## Future Improvements

1. **Delete Button**: Thêm nút xóa cho từng session trong sidebar
2. **Search**: Tìm kiếm trong lịch sử chat
3. **Pagination**: Phân trang cho sessions và messages
4. **Edit Title**: Cho phép đổi tên session
5. **Export**: Xuất lịch sử chat ra file
6. **Save Citations**: Lưu citations vào DB để hiển thị khi load session cũ

## API Examples

### Get all sessions
```bash
GET /api/chat/sessions
Authorization: Bearer {token}

Response:
[
  {
    "id": 1,
    "title": "Thủ tục ly hôn như thế nào?",
    "createdAt": "2024-12-23T10:00:00",
    "updatedAt": "2024-12-23T10:05:00",
    "messageCount": 4
  }
]
```

### Send message (new session)
```bash
POST /api/chat/sessions/messages
Authorization: Bearer {token}
Content-Type: application/json

{
  "question": "Thủ tục ly hôn như thế nào?"
}

Response:
{
  "sessionId": 1,
  "userMessage": {...},
  "assistantMessage": {
    "id": 2,
    "role": "ASSISTANT",
    "content": "...",
    "citations": [...]
  }
}
```

### Load session messages
```bash
GET /api/chat/sessions/1/messages
Authorization: Bearer {token}

Response:
[
  {
    "id": 1,
    "role": "USER",
    "content": "Thủ tục ly hôn như thế nào?",
    "citations": [],
    "createdAt": "2024-12-23T10:00:00"
  },
  {
    "id": 2,
    "role": "ASSISTANT",
    "content": "...",
    "citations": [...],
    "createdAt": "2024-12-23T10:00:05"
  }
]
```

## Status: READY FOR TESTING ✅

Tất cả code đã được implement và không có lỗi compilation. Bạn có thể restart application và test ngay!
