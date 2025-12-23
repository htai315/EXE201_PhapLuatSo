# Hệ Thống Quản Lý Văn Bản Pháp Luật

## 📋 Tổng Quan

Phase 1 đã hoàn thành: **Admin Upload & Parse Legal Documents**

Hệ thống cho phép admin upload file PDF văn bản pháp luật, tự động parse và lưu vào database theo từng điều luật.

---

## 🎯 Chức Năng Đã Implement

### ✅ Backend

1. **Database Schema**
   - `legal_documents`: Lưu thông tin văn bản (tên, mã số, loại, ngày hiệu lực...)
   - `legal_articles`: Lưu từng điều luật (số điều, tiêu đề, nội dung)
   - Migration: `V7__legal_documents.sql`

2. **Entities**
   - `LegalDocument`: Entity cho văn bản pháp luật
   - `LegalArticle`: Entity cho điều luật
   - Relationship: OneToMany với cascade

3. **Services**
   - `LegalDocumentParserService`: Parse PDF, extract articles bằng regex
   - `LegalDocumentService`: Business logic (upload, save, delete)
   - Sử dụng lại `DocumentParserService` (đã có từ AI Quiz)

4. **API Endpoints**
   - `POST /api/legal/documents/upload`: Upload file PDF
   - `GET /api/legal/documents`: Lấy danh sách văn bản
   - `DELETE /api/legal/documents/{id}`: Xóa văn bản

### ✅ Frontend

1. **Admin Upload Page** (`legal-upload.html`)
   - Drag & drop upload multiple PDF files
   - Preview danh sách file đã chọn
   - Upload progress tracking
   - Hiển thị danh sách văn bản đã upload
   - Xóa văn bản

---

## 🔧 Cách Hoạt Động

### **Flow Upload:**

```
1. Admin chọn/kéo thả file PDF
   ↓
2. Frontend validate (PDF, max 10MB)
   ↓
3. POST /api/legal/documents/upload
   ↓
4. Backend:
   - Save file to uploads/legal/
   - Parse PDF → Extract text
   - Regex pattern: "Điều 1.", "Điều 2."...
   - Split thành các điều luật
   - Save to database
   ↓
5. Response: {documentId, documentName, totalArticles}
```

### **Regex Pattern:**

```java
Pattern: "Điều\\s+(\\d+)\\.\\s*([^\\n]*)"

Ví dụ match:
- "Điều 1. Nhiệm vụ của Bộ luật hình sự"
- "Điều 123. Tội cố ý gây thương tích"

Captures:
- Group 1: Số điều (1, 2, 123...)
- Group 2: Tiêu đề điều (optional)
```

### **Database Structure:**

```sql
legal_documents:
- id: 1
- document_name: "Bộ luật Hình sự 2015"
- total_articles: 426
- created_at: 2025-12-21

legal_articles:
- id: 1, document_id: 1, article_number: 1, title: "Nhiệm vụ...", content: "..."
- id: 2, document_id: 1, article_number: 2, title: "Cơ sở...", content: "..."
- ... (426 rows)
```

---

## 🚀 Cách Sử Dụng

### **1. Chạy Migration**

```bash
# Migration sẽ tự động chạy khi start app (Flyway)
# Hoặc chạy manual trong SQL Server:
# Execute: V7__legal_documents.sql
```

### **2. Start Application**

```bash
mvn spring-boot:run
```

### **3. Truy Cập Admin Page**

```
http://localhost:8080/html/legal-upload.html
```

### **4. Upload File PDF**

1. Đăng nhập với tài khoản admin
2. Kéo thả hoặc chọn file PDF (có thể chọn nhiều file)
3. Click "Upload Tất Cả"
4. Đợi xử lý (mỗi file ~5-10 giây)
5. Xem kết quả trong danh sách

---

## 📊 Kết Quả Mẫu

### **Input:**
- File: `Bo-luat-Hinh-su-2015.pdf` (5MB, 300 trang)

### **Output Database:**

```
legal_documents:
- id: 1
- document_name: "Bộ luật Hình sự 2015"
- total_articles: 426

legal_articles: (426 rows)
- Điều 1: Nhiệm vụ của Bộ luật hình sự
- Điều 2: Cơ sở của trách nhiệm hình sự
- ...
- Điều 426: Hiệu lực thi hành
```

---

## 🔍 Testing

### **Test Case 1: Upload 1 file**

```bash
curl -X POST http://localhost:8080/api/legal/documents/upload \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@bo-luat-hinh-su.pdf" \
  -F "documentName=Bộ luật Hình sự 2015"
```

**Expected Response:**
```json
{
  "documentId": 1,
  "documentName": "Bộ luật Hình sự 2015",
  "totalArticles": 426,
  "message": "Đã import thành công 426 điều luật"
}
```

### **Test Case 2: Get all documents**

```bash
curl http://localhost:8080/api/legal/documents \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected Response:**
```json
[
  {
    "id": 1,
    "documentName": "Bộ luật Hình sự 2015",
    "documentCode": null,
    "documentType": null,
    "totalArticles": 426,
    "status": "Còn hiệu lực",
    "createdAt": "2025-12-21T10:30:00"
  }
]
```

---

## ⚠️ Lưu Ý

### **File PDF Requirements:**

1. **Cấu trúc rõ ràng**: Phải có pattern "Điều X."
2. **Encoding**: UTF-8 hoặc Unicode
3. **Kích thước**: Max 10MB/file
4. **Format**: Không được scan (phải có text layer)

### **Nếu Parse Thất Bại:**

Kiểm tra:
- File PDF có text layer không? (không phải ảnh scan)
- Có pattern "Điều 1.", "Điều 2." không?
- Encoding có đúng không?

### **Performance:**

- 1 file 5MB (~300 trang) → Parse trong ~5-10 giây
- 30 files → ~3-5 phút total
- Database size: ~50-100MB cho 30 files

---

## 🎯 Next Phase: RAG Chatbot

Phase 2 sẽ implement:
1. Search engine (SQL Full-Text Search)
2. RAG service (retrieve + AI generation)
3. Chat API endpoint
4. Chat UI với citations

Đã sẵn sàng để implement khi bạn cần!

---

## 📁 File Structure

```
src/main/java/com/htai/exe201phapluatso/
├── legal/
│   ├── entity/
│   │   ├── LegalDocument.java
│   │   └── LegalArticle.java
│   ├── repo/
│   │   ├── LegalDocumentRepo.java
│   │   └── LegalArticleRepo.java
│   ├── service/
│   │   ├── LegalDocumentService.java
│   │   └── LegalDocumentParserService.java
│   ├── controller/
│   │   └── LegalDocumentController.java
│   └── dto/
│       ├── UploadLegalDocumentRequest.java
│       ├── UploadLegalDocumentResponse.java
│       └── LegalDocumentDTO.java

src/main/resources/
├── db/migration/
│   └── V7__legal_documents.sql
└── static/html/
    └── legal-upload.html

uploads/legal/  (created automatically)
```

---

## ✅ Checklist

- [x] Database schema
- [x] Entities & Repositories
- [x] Parser service (regex-based)
- [x] Upload service
- [x] API endpoints
- [x] Frontend upload page
- [x] File storage
- [x] Error handling
- [ ] Full-text search index (Phase 2)
- [ ] RAG chatbot (Phase 2)
