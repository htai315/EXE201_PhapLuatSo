# Hướng Dẫn Chi Tiết: Setup EnvFile trong IntelliJ IDEA

## Bước 1: Cài đặt EnvFile Plugin

### 1.1. Mở Settings
- **Cách 1**: Menu `File` → `Settings` (Windows/Linux)
- **Cách 2**: Menu `IntelliJ IDEA` → `Preferences` (Mac)
- **Cách 3**: Phím tắt `Ctrl + Alt + S` (Windows/Linux) hoặc `Cmd + ,` (Mac)

### 1.2. Tìm và cài Plugin
1. Trong cửa sổ Settings, click vào **Plugins** ở menu bên trái
2. Ở tab **Marketplace** (phía trên), gõ "**EnvFile**" vào ô tìm kiếm
3. Tìm plugin tên "**EnvFile**" (tác giả: Borys Pierov)
4. Click nút **Install**
5. Sau khi cài xong, click **Restart IDE** để khởi động lại IntelliJ

---

## Bước 2: Mở Run/Debug Configurations

### 2.1. Mở Configuration Editor
Có 3 cách để mở:

**Cách 1: Từ Menu**
- Menu `Run` → `Edit Configurations...`

**Cách 2: Từ Toolbar**
- Nhìn lên góc phải màn hình, bạn sẽ thấy:
  ```
  [Tên Configuration ▼] [▶ Run] [🐛 Debug]
  ```
- Click vào dropdown `[Tên Configuration ▼]`
- Chọn **Edit Configurations...**

**Cách 3: Phím tắt**
- `Alt + Shift + F10` (Windows/Linux)
- `Ctrl + Alt + R` (Mac)
- Sau đó nhấn `0` để chọn "Edit Configurations"

---

## Bước 3: Chọn Spring Boot Application

### 3.1. Trong cửa sổ Run/Debug Configurations:
1. Bên trái, bạn sẽ thấy danh sách các configurations
2. Tìm mục **Spring Boot** (có thể đã mở sẵn)
3. Click vào tên application của bạn, ví dụ:
   - `Exe201PhapLuatSoApplication`
   - Hoặc tên class main của bạn

### 3.2. Nếu chưa có Configuration:
1. Click nút **+** (Add New Configuration) ở góc trên bên trái
2. Chọn **Spring Boot**
3. Điền thông tin:
   - **Name**: `Exe201PhapLuatSoApplication`
   - **Main class**: Click `...` và chọn class có `@SpringBootApplication`
   - **Module**: Chọn module của project

---

## Bước 4: Configure EnvFile Tab

### 4.1. Tìm tab EnvFile
Sau khi chọn Spring Boot configuration, bạn sẽ thấy nhiều tabs ở phía trên:
```
Configuration | Logs | Code Coverage | EnvFile | ...
```

Click vào tab **EnvFile**

### 4.2. Enable EnvFile
1. Tìm checkbox **"Enable EnvFile"** ở đầu tab
2. ✅ **Tick vào checkbox này** để bật tính năng

### 4.3. Add .env file
1. Trong tab EnvFile, bạn sẽ thấy một bảng trống với các cột:
   ```
   | ✓ | Path | Type |
   ```

2. Click nút **+** (Add) ở góc dưới bên trái của bảng

3. Một menu sẽ hiện ra, chọn **".env file"**

4. Một cửa sổ file browser sẽ mở ra:
   - Navigate đến thư mục root của project
   - Chọn file **`.env`** (file bạn vừa tạo)
   - Click **OK**

5. File `.env` sẽ xuất hiện trong bảng:
   ```
   | ✓ | C:\...\EXE201_PhapLuatSo\.env | .env file |
   ```

6. Đảm bảo checkbox ở cột đầu tiên (✓) được tick

### 4.4. Configure Options (Optional)
Dưới bảng file list, bạn có thể thấy các options:
- ✅ **"Substitute Env Vars"** - Nên tick
- ✅ **"Ignore missing files"** - Nên tick (để không lỗi nếu file không tồn tại)

---

## Bước 5: Apply và Save

1. Click nút **Apply** ở góc dưới bên phải
2. Click nút **OK** để đóng cửa sổ

---

## Bước 6: Verify Setup

### 6.1. Kiểm tra nhanh
1. Mở file `.env` và đảm bảo có nội dung:
   ```properties
   JWT_SECRET=your_secret_here
   OPENAI_API_KEY=sk-proj-...
   GOOGLE_CLIENT_ID=...
   ```

2. Trong class main của bạn, thêm đoạn code test (tạm thời):
   ```java
   @SpringBootApplication
   public class Exe201PhapLuatSoApplication {
       public static void main(String[] args) {
           // TEST: Print environment variables
           System.out.println("=== ENVIRONMENT VARIABLES CHECK ===");
           System.out.println("JWT_SECRET: " + (System.getenv("JWT_SECRET") != null ? "✓ Loaded" : "✗ Missing"));
           System.out.println("OPENAI_API_KEY: " + (System.getenv("OPENAI_API_KEY") != null ? "✓ Loaded" : "✗ Missing"));
           System.out.println("GOOGLE_CLIENT_ID: " + (System.getenv("GOOGLE_CLIENT_ID") != null ? "✓ Loaded" : "✗ Missing"));
           System.out.println("===================================");
           
           SpringApplication.run(Exe201PhapLuatSoApplication.class, args);
       }
   }
   ```

3. Run application (click nút ▶ Run)

4. Xem console output, bạn sẽ thấy:
   ```
   === ENVIRONMENT VARIABLES CHECK ===
   JWT_SECRET: ✓ Loaded
   OPENAI_API_KEY: ✓ Loaded
   GOOGLE_CLIENT_ID: ✓ Loaded
   ===================================
   ```

5. Nếu thấy "✓ Loaded" → **Thành công!** 🎉
6. Nếu thấy "✗ Missing" → Xem phần Troubleshooting bên dưới

---

## Troubleshooting (Xử lý lỗi)

### Lỗi 1: Không thấy tab EnvFile
**Nguyên nhân**: Plugin chưa được cài hoặc chưa restart IDE

**Giải pháp**:
1. Vào `File` → `Settings` → `Plugins`
2. Kiểm tra "EnvFile" đã được cài chưa
3. Nếu chưa, cài và restart IDE
4. Nếu đã cài, thử restart IDE lại

### Lỗi 2: Environment variables không load
**Nguyên nhân**: Checkbox "Enable EnvFile" chưa được tick

**Giải pháp**:
1. Mở `Run` → `Edit Configurations`
2. Chọn Spring Boot configuration
3. Tab EnvFile
4. ✅ Tick vào "Enable EnvFile"
5. ✅ Đảm bảo file .env có checkbox được tick
6. Apply và OK

### Lỗi 3: File .env không tìm thấy
**Nguyên nhân**: Đường dẫn file sai hoặc file chưa tạo

**Giải pháp**:
1. Kiểm tra file `.env` có tồn tại ở root project không
2. Trong tab EnvFile, xóa file cũ (click dấu -)
3. Add lại file .env với đường dẫn đúng
4. Hoặc tick vào "Ignore missing files"

### Lỗi 4: Giá trị environment variable bị sai
**Nguyên nhân**: File .env có syntax sai

**Giải pháp**:
1. Mở file `.env`
2. Đảm bảo format đúng:
   ```properties
   KEY=value
   # Không có dấu cách trước/sau =
   # Không cần dấu ngoặc kép (trừ khi value có space)
   ```
3. Ví dụ đúng:
   ```properties
   JWT_SECRET=my_secret_key_here
   OPENAI_API_KEY=sk-proj-abc123
   ```
4. Ví dụ SAI:
   ```properties
   JWT_SECRET = "my_secret_key_here"  # ✗ Có space và quotes không cần thiết
   ```

---

## Alternative: Không dùng EnvFile Plugin

Nếu không muốn cài plugin, bạn có thể set environment variables thủ công:

### Cách 1: Trong Run Configuration
1. `Run` → `Edit Configurations`
2. Chọn Spring Boot app
3. Tìm field **"Environment variables"**
4. Click icon 📁 (folder) bên phải
5. Click **+** để thêm từng biến:
   ```
   Name: JWT_SECRET
   Value: your_secret_here
   ```
6. Lặp lại cho tất cả các biến
7. Apply và OK

### Cách 2: Set trong System (Windows)
```cmd
# Mở Command Prompt as Administrator
setx JWT_SECRET "your_secret_here"
setx OPENAI_API_KEY "sk-proj-..."
setx GOOGLE_CLIENT_ID "your-client-id"
# ... các biến khác

# Restart IntelliJ để load biến mới
```

### Cách 3: Set trong System (Mac/Linux)
```bash
# Thêm vào ~/.bashrc hoặc ~/.zshrc
export JWT_SECRET="your_secret_here"
export OPENAI_API_KEY="sk-proj-..."
export GOOGLE_CLIENT_ID="your-client-id"

# Reload
source ~/.bashrc

# Restart IntelliJ
```

---

## Summary Checklist

- [ ] Cài EnvFile plugin
- [ ] Restart IntelliJ
- [ ] Tạo file `.env` từ `.env.example`
- [ ] Điền giá trị thực vào `.env`
- [ ] Mở Run/Debug Configurations
- [ ] Chọn Spring Boot application
- [ ] Vào tab EnvFile
- [ ] ✅ Enable EnvFile
- [ ] Add file `.env`
- [ ] ✅ Tick checkbox cho file
- [ ] Apply và OK
- [ ] Test bằng cách print env vars
- [ ] Xóa code test sau khi verify

---

## Lưu ý quan trọng

⚠️ **KHÔNG BAO GIỜ commit file `.env` lên Git!**

File `.env` đã được thêm vào `.gitignore`, nhưng hãy luôn kiểm tra trước khi commit:

```bash
# Kiểm tra xem .env có trong staged files không
git status

# Nếu thấy .env, ĐỪNG commit! Thêm vào .gitignore:
echo ".env" >> .gitignore
git add .gitignore
git commit -m "Add .env to gitignore"
```

🎉 **Xong! Bây giờ application của bạn đã load environment variables an toàn!**
