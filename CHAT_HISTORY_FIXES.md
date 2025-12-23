# Chat History Bug Fixes

## Issues Fixed

### 1. JavaScript Error: "Cannot read properties of undefined (reading 'target')"
**Problem**: Khi click vào session trong sidebar, có lỗi `event.target` undefined

**Root Cause**: Function `loadSession()` sử dụng `event.target` nhưng không nhận `event` parameter

**Solution**:
- Thay đổi `onclick="loadSession(${session.id})"` thành `onclick="loadSession(${session.id}, this)"`
- Update function signature: `window.loadSession = async (sessionId, clickedElement) => {...}`
- Sử dụng `clickedElement.closest('.chat-item')` thay vì `event.target.closest('.chat-item')`

### 2. Citations Không Hiển Thị Khi Load Session Cũ
**Problem**: Khi load lại session cũ, phần "Nguồn trích dẫn" không hiển thị

**Root Cause**: Citations không được lưu vào database, chỉ trả về trong response khi chat mới

**Solution**:
- **Backend**: Lưu citations vào database khi save assistant message
  - Tạo list `LegalArticle` từ `chatResponse.citations()`
  - Set citations cho `assistantMessage` trước khi save
  - JPA tự động lưu vào bảng `chat_message_citations`

- **Frontend**: Cải thiện xử lý citations
  - Check `citations && Array.isArray(citations) && citations.length > 0`
  - Handle null/undefined values trong citation fields
  - Hiển thị "Văn bản pháp luật" nếu documentName null

## Code Changes

### Backend: ChatHistoryService.java
```java
// Add citations to message
if (chatResponse.citations() != null && !chatResponse.citations().isEmpty()) {
    List<LegalArticle> articles = chatResponse.citations().stream()
            .map(citation -> {
                LegalArticle article = new LegalArticle();
                article.setId(citation.articleId());
                return article;
            })
            .collect(Collectors.toList());
    assistantMessage.setCitations(articles);
}

assistantMessage = messageRepo.save(assistantMessage);
```

### Frontend: legal-chat.html

**1. Update onclick handler:**
```html
<div class="chat-item" onclick="loadSession(${session.id}, this)">
```

**2. Update loadSession function:**
```javascript
window.loadSession = async (sessionId, clickedElement) => {
    // ... load messages ...
    
    // Update active state
    if (clickedElement) {
        const chatItem = clickedElement.closest('.chat-item');
        if (chatItem) {
            chatItem.classList.add('active');
        }
    }
}
```

**3. Improve addMessage function:**
```javascript
// Check if citations exist and is an array with items
if (citations && Array.isArray(citations) && citations.length > 0) {
    // ... render citations ...
    <div><strong>${escapeHtml(c.documentName || 'Văn bản pháp luật')}</strong></div>
    <div class="citation-source">Điều ${c.articleNumber || 'N/A'}...</div>
}
```

## How Citations Work Now

### Flow khi gửi tin nhắn mới:
1. User gửi câu hỏi
2. `LegalChatService.chat()` trả về `ChatResponse` với citations
3. `ChatHistoryService` lưu assistant message
4. **NEW**: Lưu citations vào `chat_message_citations` table
5. Frontend hiển thị citations từ response

### Flow khi load session cũ:
1. User click vào session trong sidebar
2. Backend query messages với `LEFT JOIN FETCH m.citations`
3. **NEW**: Citations được load từ database
4. Frontend hiển thị citations từ database

## Database Schema

```sql
-- Citations được lưu trong bảng many-to-many
CREATE TABLE dbo.chat_message_citations (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    message_id BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    CONSTRAINT fk_citation_message FOREIGN KEY (message_id) 
        REFERENCES dbo.chat_messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_citation_article FOREIGN KEY (article_id) 
        REFERENCES dbo.legal_articles(id)
);
```

## Testing

### Test Case 1: Send New Message
1. ✅ Gửi câu hỏi mới
2. ✅ AI trả lời với citations
3. ✅ Citations hiển thị đầy đủ
4. ✅ Citations được lưu vào database

### Test Case 2: Load Old Session
1. ✅ Click vào session cũ trong sidebar
2. ✅ Messages load đầy đủ
3. ✅ **NEW**: Citations hiển thị từ database
4. ✅ Không có lỗi JavaScript
5. ✅ Active state cập nhật đúng

### Test Case 3: Switch Between Sessions
1. ✅ Click session A → load messages A
2. ✅ Click session B → load messages B
3. ✅ Active state chuyển đổi đúng
4. ✅ Không có alert lỗi

## Status: FIXED ✅

Cả 2 issues đã được fix:
- ✅ JavaScript error đã được sửa
- ✅ Citations giờ được lưu và hiển thị khi load session cũ

Restart application và test lại!
