# AI Quiz Generation - Fix Thiếu Câu Hỏi

## ❌ Vấn Đề
User chọn tạo 20 câu hỏi nhưng chỉ nhận được 17 câu.

## 🔍 Nguyên Nhân
1. **Token limit quá thấp**: `max_tokens: 8000` không đủ cho 20 câu hỏi phức tạp
2. **Thiếu logging**: Không biết AI trả về bao nhiêu câu

## ✅ Giải Pháp

### 1. Tăng Token Limit
```java
"max_tokens", 16000  // Tăng từ 8000 lên 16000
```

### 2. Thêm Logging
```java
System.out.println("Requesting " + count + " questions");  // Log số câu yêu cầu
System.out.println("Parsed " + questions.size() + " questions from OpenAI response");  // Log số câu nhận được
```

## 🚀 Test Lại

### Bước 1: Rebuild
Rebuild project trong IntelliJ

### Bước 2: Test Generate Quiz
1. Vào: `http://localhost:8080/html/quiz-generate-ai.html`
2. Upload file PDF/DOCX
3. Chọn **20 câu hỏi**
4. Generate

### Bước 3: Xem Console Logs
Phải thấy:
```
Requesting 20 questions
Calling OpenAI API with model: gpt-4o-mini
OpenAI response received
Parsed 20 questions from OpenAI response
Quiz generation completed for user X. Created quiz set Y with 20 questions
```

### Bước 4: Kiểm Tra Kết Quả
- Phải có **đúng 20 câu hỏi** trong quiz set
- Mỗi câu có 4 đáp án (A, B, C, D)
- Có giải thích cho đáp án đúng

## 📊 Token Limit Giải Thích

### Trước (8000 tokens)
- ~400 tokens/câu hỏi
- 8000 / 400 = **20 câu** (lý thuyết)
- Thực tế: **15-17 câu** (do prompt + overhead)

### Sau (16000 tokens)
- ~400 tokens/câu hỏi
- 16000 / 400 = **40 câu** (lý thuyết)
- Thực tế: **30-35 câu** (đủ cho 20 câu yêu cầu)

## 🎯 Lưu Ý

### Nếu Vẫn Thiếu Câu
Có thể do:
1. **Nội dung tài liệu quá ngắn**: AI không đủ thông tin để tạo 20 câu
2. **AI tự giới hạn**: GPT-4o-mini quyết định chỉ tạo số câu phù hợp với nội dung

### Giải Pháp
- Kiểm tra console logs xem AI trả về bao nhiêu câu
- Nếu tài liệu ngắn, giảm số câu yêu cầu xuống 10-15
- Hoặc upload tài liệu dài hơn

## 📝 Files Đã Sửa

- `src/main/java/com/htai/exe201phapluatso/ai/service/OpenAIService.java`
  - Tăng `max_tokens` từ 8000 → 16000
  - Thêm logging số câu yêu cầu và nhận được

---

**Rebuild và test lại nhé!** 🚀
