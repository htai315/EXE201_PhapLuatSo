# Conversation History Feature - AI Context Awareness

## Problem Statement

**Current Behavior**: AI treats each question independently, không nhớ ngữ cảnh của câu hỏi trước.

**Example**:
```
User: "Tuổi kết hôn là bao nhiêu?"
AI: "Nam đủ 20 tuổi, nữ đủ 18 tuổi theo Điều 8..."

User: "Còn điều kiện khác không?"  ← AI không biết "điều kiện" của cái gì
AI: "Xin lỗi, tôi không hiểu câu hỏi..."  ❌ Không nhớ đang nói về kết hôn
```

**Desired Behavior**: AI nhớ ngữ cảnh và trả lời liên quan.

```
User: "Tuổi kết hôn là bao nhiêu?"
AI: "Nam đủ 20 tuổi, nữ đủ 18 tuổi theo Điều 8..."

User: "Còn điều kiện khác không?"
AI: "Ngoài tuổi, kết hôn còn phải tự nguyện, không bị cấm kết hôn theo Điều 9..." ✅
```

## Solution: Pass Conversation History to AI

### Architecture

```
User sends message
    ↓
Load last 5 messages from session
    ↓
Format as conversation history
    ↓
Pass to AI along with question
    ↓
AI uses history for context
    ↓
Generate contextual answer
```

## Implementation Plan

### Step 1: Modify LegalChatService

**Add overloaded method**:
```java
// Existing method (backward compatible)
public ChatResponse chat(String question) {
    return chat(question, List.of());
}

// New method with conversation history
public ChatResponse chat(String question, List<String> conversationHistory) {
    // ... existing logic ...
    
    // Pass history to prompt builder
    String prompt = buildPrompt(question, context, conversationHistory);
    
    // ... rest of logic ...
}
```

**Update prompt builder**:
```java
private String buildPrompt(String question, String context, List<String> conversationHistory) {
    StringBuilder prompt = new StringBuilder();
    
    prompt.append("Bạn là chuyên gia tư vấn pháp luật Việt Nam.\n\n");
    
    // Add conversation history if available
    if (!conversationHistory.isEmpty()) {
        prompt.append("LỊCH SỬ HỘI THOẠI TRƯỚC ĐÓ:\n");
        for (String message : conversationHistory) {
            prompt.append(message).append("\n");
        }
        prompt.append("\n");
        prompt.append("LƯU Ý: Sử dụng lịch sử để hiểu ngữ cảnh, nhưng chỉ trả lời câu hỏi HIỆN TẠI.\n\n");
    }
    
    // ... rest of prompt ...
}
```

### Step 2: Modify ChatHistoryService

**Add helper method**:
```java
private List<String> getConversationHistory(Long sessionId, int limit) {
    List<ChatMessage> recentMessages = messageRepo.findBySessionIdWithCitations(sessionId)
            .stream()
            .sorted((m1, m2) -> m2.getCreatedAt().compareTo(m1.getCreatedAt())) // newest first
            .limit(limit * 2) // Get last N pairs (user + assistant)
            .sorted((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt())) // back to chronological
            .collect(Collectors.toList());

    List<String> history = new ArrayList<>();
    for (ChatMessage msg : recentMessages) {
        String role = msg.getRole().equals("USER") ? "NGƯỜI DÙNG" : "TRỢ LÝ";
        history.add(role + ": " + msg.getContent());
    }
    
    return history;
}
```

**Update sendMessage method**:
```java
public SendMessageResponse sendMessage(String userEmail, Long sessionId, String question) {
    // ... existing code to save user message ...
    
    // Get conversation history (last 5 messages for context)
    List<String> conversationHistory = getConversationHistory(session.getId(), 5);

    // Generate AI response with conversation context
    ChatResponse chatResponse = chatService.chat(question, conversationHistory);
    
    // ... rest of code ...
}
```

## Benefits

### 1. Natural Conversation Flow
```
User: "Thủ tục ly hôn như thế nào?"
AI: "Phải nộp đơn lên UBND, có giấy tờ..."

User: "Mất bao lâu?"  ← AI hiểu "mất bao lâu" là về thủ tục ly hôn
AI: "Thời gian xử lý thường 1-2 tháng..."

User: "Cần giấy gì?"  ← AI hiểu "giấy gì" là giấy tờ ly hôn
AI: "Cần giấy kết hôn, CMND, sổ hộ khẩu..."
```

### 2. Handle Pronouns and References
```
User: "Điều 8 nói gì?"
AI: "Điều 8 quy định tuổi kết hôn..."

User: "Nó có ngoại lệ không?"  ← AI hiểu "nó" = Điều 8
AI: "Có, trong trường hợp đặc biệt..."
```

### 3. Follow-up Questions
```
User: "Hợp đồng mua bán đất cần công chứng không?"
AI: "Có, theo Điều 123..."

User: "Còn cách nào khác?"  ← AI hiểu đang hỏi về hợp đồng đất
AI: "Có thể chứng thực tại UBND..."
```

## Token Cost Impact

### Without Conversation History
```
Input: 
- Prompt: 800 tokens
- Context (3 articles): 1,500 tokens
- Question: 50 tokens
Total: 2,350 tokens
```

### With Conversation History (5 messages)
```
Input:
- Prompt: 800 tokens
- History (5 messages): 500 tokens  ← NEW
- Context (3 articles): 1,500 tokens
- Question: 50 tokens
Total: 2,850 tokens (+21%)
```

### Cost Analysis
```
Per request:
- Without history: $0.000353 input
- With history: $0.000428 input
- Increase: $0.000075 (+21%)

Per 1,000 requests:
- Without history: $0.35
- With history: $0.43
- Increase: $0.08 (+21%)
```

**Verdict**: Tăng 21% cost nhưng UX tốt hơn NHIỀU!

## Configuration Options

### Option 1: Always Include History (Recommended)
```java
// Always pass last 5 messages
List<String> history = getConversationHistory(sessionId, 5);
ChatResponse response = chatService.chat(question, history);
```

**Pros**: Best UX, natural conversation
**Cons**: 21% more tokens

### Option 2: Smart History (Conditional)
```java
// Only include history if question seems to reference previous context
if (questionNeedsContext(question)) {
    List<String> history = getConversationHistory(sessionId, 5);
    response = chatService.chat(question, history);
} else {
    response = chatService.chat(question);
}

private boolean questionNeedsContext(String question) {
    // Check for pronouns, short questions, etc.
    return question.length() < 20 || 
           question.matches(".*\\b(nó|đó|thế|còn|khác)\\b.*");
}
```

**Pros**: Saves tokens on simple questions
**Cons**: More complex logic, might miss some cases

### Option 3: Configurable Limit
```java
// Allow users to configure history length
private static final int HISTORY_LIMIT = 5; // Can be changed

List<String> history = getConversationHistory(sessionId, HISTORY_LIMIT);
```

**Pros**: Flexible, can tune based on usage
**Cons**: Need to find optimal value

## Implementation Complexity

### Difficulty: ⭐⭐ (Easy-Medium)

**Time Estimate**: 30-45 minutes

**Changes Required**:
1. Add overloaded method to `LegalChatService` (5 min)
2. Update `buildPrompt()` to include history (10 min)
3. Add `getConversationHistory()` helper (10 min)
4. Update `ChatHistoryService.sendMessage()` (5 min)
5. Testing (10 min)

**Risk**: Low - backward compatible, no breaking changes

## Testing Scenarios

### Test 1: Simple Follow-up
```
Q1: "Tuổi kết hôn là bao nhiêu?"
A1: "Nam 20, nữ 18..."

Q2: "Còn điều kiện khác không?"
Expected: AI mentions "kết hôn" in answer ✅
```

### Test 2: Pronoun Resolution
```
Q1: "Điều 8 nói gì?"
A1: "Điều 8 về tuổi kết hôn..."

Q2: "Nó có ngoại lệ không?"
Expected: AI understands "nó" = Điều 8 ✅
```

### Test 3: Context Switching
```
Q1: "Thủ tục ly hôn?"
A1: "Nộp đơn lên UBND..."

Q2: "Tuổi kết hôn là bao nhiêu?"  ← New topic
Expected: AI answers about marriage age, not divorce ✅
```

### Test 4: Long Conversation
```
Q1-Q5: Various questions about marriage
Q6: "Còn về ly hôn thì sao?"
Expected: AI switches context to divorce ✅
```

## Monitoring

### Metrics to Track
```java
log.info("Chat with {} history messages, question length: {}", 
         conversationHistory.size(), question.length());
```

**Track**:
- Average history length used
- Token usage increase
- User satisfaction (fewer "không hiểu" responses)

## Alternative Approaches

### Alternative 1: No History (Current)
**Pros**: Simple, cheap
**Cons**: Poor UX, can't handle follow-ups

### Alternative 2: Full Session History
**Pros**: Maximum context
**Cons**: Very expensive, slow, may confuse AI

### Alternative 3: Semantic History (Advanced)
```java
// Only include relevant previous messages
List<String> relevantHistory = findRelevantMessages(question, allMessages);
```
**Pros**: Optimal context, lower cost
**Cons**: Complex, requires embedding search

### Alternative 4: Conversation History (Recommended)
**Pros**: Good balance of UX and cost
**Cons**: Slight cost increase

## Recommendation

### ✅ IMPLEMENT Conversation History

**Reasons**:
1. **Much Better UX**: Natural conversation flow
2. **Reasonable Cost**: Only 21% increase (~$0.08 per 1,000 requests)
3. **Easy to Implement**: 30-45 minutes
4. **Low Risk**: Backward compatible
5. **Competitive Feature**: Most modern chatbots have this

**When to Implement**:
- After current features are stable
- When you have time for 30-45 min implementation
- Before launching to users (important for UX)

**Priority**: HIGH (should do soon)

## Code Summary

### Files to Modify:
1. `LegalChatService.java` - Add overloaded method, update prompt
2. `ChatHistoryService.java` - Add history helper, update sendMessage

### Lines of Code: ~50 lines total

### Breaking Changes: None (backward compatible)

## Example Implementation

See the code changes I attempted earlier (before file corruption). The approach is:

1. Add `chat(String question, List<String> history)` method
2. Update `buildPrompt()` to include history section
3. Add `getConversationHistory()` helper in ChatHistoryService
4. Call with history in `sendMessage()`

That's it! Simple and effective.

## Conclusion

Conversation history is a **must-have feature** for modern chatbots. The 21% cost increase is worth it for the significantly better user experience. Recommend implementing after current features are stable.
