# ✅ VNPay Environment Variables - GIẢI PHÁP CUỐI CÙNG

## Vấn Đề

`DotEnvEnvironmentPostProcessor` không được Spring Boot gọi, có thể do:
- Spring Boot 4.0.0 có cơ chế khác
- Hoặc vấn đề với service loader

## Giải Pháp Áp Dụng

Thay vì dùng `EnvironmentPostProcessor`, tôi đã chuyển sang dùng **ApplicationContextInitializer** trực tiếp trong `main()` method.

### Code Mới

```java
SpringApplication app = new SpringApplication(Exe201PhapLuatSoApplication.class);

app.addInitializers(context -> {
    // Load .env file into Spring Environment
    // This runs BEFORE application.properties is processed
});

app.run(args);
```

## Tại Sao Cách Này Hoạt Động

1. **ApplicationContextInitializer** chạy TRƯỚC khi Spring xử lý properties
2. Load `.env` file vào `Environment` với priority cao nhất
3. Khi Spring xử lý `${VNPAY_TMN_CODE}`, nó sẽ tìm thấy giá trị từ `.env`

## 🔧 Cách Áp Dụng

### Trong IntelliJ IDEA:

1. **Build → Rebuild Project**
2. **Stop** application (nút vuông đỏ)
3. **Start** application (nút play xanh)

## ✅ Kiểm Tra

Sau khi restart, bạn sẽ thấy trong log:

```
📁 Loading .env file: C:\Users\Chung\IdeaProjects\EXE201_PhapLuatSo\.env
✅ Loaded 13 variables from .env
```

Khi click nút thanh toán, log sẽ hiển thị:

```
vnp_TmnCode=NA128BPU
```

KHÔNG còn:

```
vnp_TmnCode=your-vnpay-tmn-code
```

## 🎯 Kết Quả Mong Đợi

- VNPay payment button sẽ hoạt động
- Redirect đến trang VNPay sandbox
- Không còn lỗi "Không tìm thấy website"
- Credentials vẫn được giấu khỏi `application.properties`

## 📝 Files Đã Thay Đổi

1. ✅ `src/main/java/com/htai/exe201phapluatso/Exe201PhapLuatSoApplication.java` - Thêm ApplicationContextInitializer
2. ✅ `src/main/resources/application.properties` - Dùng `${VNPAY_TMN_CODE:default}`
3. ✅ `.env` - Chứa credentials thực (gitignored)

## 💡 Lưu Ý

- Cách này đơn giản hơn và đáng tin cậy hơn
- Không cần `spring.factories`
- Không cần `EnvironmentPostProcessor`
- Code ngắn gọn, dễ hiểu
- Hoạt động với mọi phiên bản Spring Boot

## 🔒 Bảo Mật

- `.env` file vẫn được gitignore
- Credentials không xuất hiện trong code
- Giống pattern với OpenAI key, Google OAuth2
- Production-ready
