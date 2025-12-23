# 🎓 Hướng Dẫn Sử Dụng Tính Năng Tạo Đề Bằng AI

## 📋 Tổng Quan

Tính năng **Tạo Đề Bằng AI** cho phép bạn tự động tạo bộ câu hỏi trắc nghiệm từ tài liệu học tập chỉ trong vài giây.

## ✨ Tính Năng

- ✅ Upload tài liệu PDF, DOCX, TXT
- ✅ AI tự động phân tích và tạo câu hỏi
- ✅ Mỗi câu hỏi có 4 đáp án
- ✅ Có giải thích chi tiết
- ✅ Tự động lưu vào bộ đề
- ✅ Chọn số lượng câu hỏi (10-25 câu)

## 🚀 Cách Sử Dụng

### Bước 1: Truy cập trang Tạo Đề Bằng AI
1. Đăng nhập vào hệ thống
2. Vào **Bộ đề** → Click nút **"Tạo Bằng AI"** (màu xanh lá)
3. Hoặc truy cập trực tiếp: `/html/quiz-generate-ai.html`

### Bước 2: Upload tài liệu
1. **Kéo thả** file vào vùng upload
2. Hoặc **click** vào vùng upload để chọn file
3. File hỗ trợ:
   - PDF (`.pdf`)
   - Word (`.docx`)
   - Text (`.txt`)
4. Kích thước tối đa: **10MB**

### Bước 3: Điền thông tin
1. **Tên bộ đề** (bắt buộc)
   - VD: "Bộ đề Luật Dân sự 2015"
2. **Số câu hỏi** (chọn từ dropdown)
   - 10, 15, 20, hoặc 25 câu
   - Mặc định: 15 câu
3. **Mô tả** (tùy chọn)
   - Mô tả ngắn về bộ đề

### Bước 4: Tạo câu hỏi
1. Click nút **"Tạo Câu Hỏi Bằng AI"**
2. Đợi 10-30 giây (AI đang xử lý)
3. Xem kết quả

### Bước 5: Xem và sử dụng
1. Xem preview tất cả câu hỏi
2. Đáp án đúng được highlight màu xanh
3. Đọc giải thích cho mỗi câu
4. Click **"Xem Danh Sách Bộ Đề"** để quản lý
5. Hoặc **"Tạo Bộ Đề Khác"** để tiếp tục

## 💡 Tips Để Có Kết Quả Tốt Nhất

### 1. Chọn tài liệu phù hợp
- ✅ Tài liệu có cấu trúc rõ ràng
- ✅ Nội dung về pháp luật Việt Nam
- ✅ Văn bản pháp luật, giáo trình, tài liệu học tập
- ❌ Tránh tài liệu quá ngắn (< 500 từ)
- ❌ Tránh tài liệu không liên quan đến pháp luật

### 2. Số lượng câu hỏi
- **10 câu**: Tài liệu ngắn (1-2 trang)
- **15 câu**: Tài liệu trung bình (3-5 trang) ⭐ Recommended
- **20-25 câu**: Tài liệu dài (5+ trang)

### 3. Kiểm tra kết quả
- Đọc lại câu hỏi để đảm bảo chính xác
- Kiểm tra đáp án đúng
- Đọc giải thích
- Có thể chỉnh sửa sau nếu cần

## 📊 Ví Dụ Thực Tế

### Input:
```
File: Bộ luật Dân sự 2015 - Chương 2.pdf
Tên: Bộ đề Năng lực pháp luật dân sự
Số câu: 15
```

### Output:
```
✅ Đã tạo 15 câu hỏi
✅ Bộ đề ID: 123
✅ Thời gian: 18 giây
```

### Câu hỏi mẫu:
```
Câu 1: Theo Bộ luật Dân sự 2015, độ tuổi thành niên ở Việt Nam là?
A. 16 tuổi
B. 17 tuổi
C. 18 tuổi ✓
D. 21 tuổi

Giải thích: Theo Điều 21 Bộ luật Dân sự 2015, công dân đủ 18 tuổi 
trở lên là người thành niên.
```

## ⚠️ Lưu Ý

### Giới hạn
- File tối đa: **10MB**
- Số câu hỏi: **5-50 câu**
- Thời gian xử lý: **10-30 giây**
- Độ dài văn bản: **Tối đa 50,000 ký tự**

### Lỗi thường gặp
1. **"File không được để trống"**
   - Chưa chọn file → Chọn file trước khi submit

2. **"Chỉ hỗ trợ file PDF, DOCX, TXT"**
   - File sai định dạng → Chuyển đổi sang định dạng hỗ trợ

3. **"File không được vượt quá 10MB"**
   - File quá lớn → Nén file hoặc chia nhỏ tài liệu

4. **"File không có nội dung văn bản"**
   - File PDF là ảnh scan → Dùng OCR hoặc file khác

5. **"AI không tạo được câu hỏi nào"**
   - Tài liệu không phù hợp → Thử tài liệu khác

## 🎯 Sau Khi Tạo Xong

### Bạn có thể:
1. **Xem bộ đề** trong danh sách
2. **Quản lý câu hỏi** (sửa, xóa)
3. **Làm bài thi** ngay lập tức
4. **Chia sẻ** với người khác (tính năng sắp có)

### Chỉnh sửa câu hỏi:
1. Vào **Bộ đề** → Chọn bộ đề vừa tạo
2. Click **"Quản lý câu hỏi"**
3. Sửa/xóa câu hỏi nếu cần

## 🔮 Tính Năng Sắp Có

- [ ] Chọn độ khó (Dễ, Trung bình, Khó)
- [ ] Chọn loại câu hỏi (Lý thuyết, Tình huống)
- [ ] Preview trước khi lưu
- [ ] Chỉnh sửa câu hỏi trước khi lưu
- [ ] Export ra file Word/PDF
- [ ] Tạo từ nhiều file cùng lúc

## 📞 Hỗ Trợ

Nếu gặp vấn đề:
1. Kiểm tra lại file và thông tin
2. Thử lại với file khác
3. Liên hệ support nếu vẫn lỗi

---

**Chúc bạn tạo đề thành công! 🎉**
