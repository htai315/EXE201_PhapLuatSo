# 🔐 Google OAuth2 Login - Hướng Dẫn Chi Tiết

## 📋 Mục Lục
1. [Tạo Google Cloud Project](#1-tạo-google-cloud-project)
2. [Cấu hình OAuth2 Credentials](#2-cấu-hình-oauth2-credentials)
3. [Thêm Dependencies](#3-thêm-dependencies)
4. [Cấu hình Application Properties](#4-cấu-hình-application-properties)
5. [Tạo Entity & Repository](#5-tạo-entity--repository)
6. [Implement OAuth2 Service](#6-implement-oauth2-service)
7. [Tạo Controller](#7-tạo-controller)
8. [Cập nhật Frontend](#8-cập-nhật-frontend)
9. [Testing](#9-testing)

---

## 1. Tạo Google Cloud Project

### Bước 1.1: Truy cập Google Cloud Console
1. Mở trình duyệt và truy cập: https://console.cloud.google.com/
2. Đăng nhập bằng tài khoản Google của bạn

### Bước 1.2: Tạo Project Mới
1. Click vào dropdown "Select a project" ở góc trên bên trái
2. Click "NEW PROJECT"
3. Điền thông tin:
   - **Project name**: `phap-luat-so` (hoặc tên bạn muốn)
   - **Organization**: Để trống nếu không có
   - **Location**: Để mặc định
4. Click "CREATE"
5. Đợi vài giây để project được tạo

### Bước 1.3: Enable Google+ API (Optional nhưng nên làm)
1. Trong project vừa tạo, vào menu bên trái
2. Chọn "APIs & Services" > "Library"
3. Tìm "Google+ API"
4. Click "ENABLE"

---

## 2. Cấu hình OAuth2 Credentials

### Bước 2.1: Cấu hình OAuth Consent Screen
1. Vào "APIs & Services" > "OAuth consent screen"
2. Chọn **User Type**:
   - **External**: Cho phép bất kỳ ai có Google account đăng nhập
   - **Internal**: Chỉ cho phép user trong organization (cần Google Workspace)
   - **Chọn "External"** rồi click "CREATE"

3. **Điền thông tin App**:
   ```
   App name: Pháp Luật Số
   User support email: [your-email@gmail.com]
   App logo: (Optional - upload logo nếu có)
   
   Application home page: http://localhost:8080
   Application privacy policy link: http://localhost:8080/privacy
   Application terms of service link: http://localhost:8080/terms
   
   Authorized domains: localhost (cho development)
   
   Developer contact information: [your-email@gmail.com]
   ```

4. Click "SAVE AND CONTINUE"

5. **Scopes** (Bước 2):
   - Click "ADD OR REMOVE SCOPES"
   - Chọn các scopes sau:
     - `userinfo.email`
     - `userinfo.profile`
     - `openid`
   - Click "UPDATE" rồi "SAVE AND CONTINUE"

6. **Test users** (Bước 3):
   - Click "ADD USERS"
   - Thêm email của bạn để test
   - Click "ADD" rồi "SAVE AND CONTINUE"

7. **Summary** (Bước 4):
   - Review lại thông tin
   - Click "BACK TO DASHBOARD"

### Bước 2.2: Tạo OAuth2 Credentials
1. Vào "APIs & Services" > "Credentials"
2. Click "CREATE CREDENTIALS" > "OAuth client ID"
3. Chọn **Application type**: "Web application"
4. Điền thông tin:
   ```
   Name: Pháp Luật Số Web Client
   
   Authorized JavaScript origins:
   - http://localhost:8080
   - http://127.0.0.1:8080
   
   Authorized redirect URIs:
   - http://localhost:8080/login/oauth2/code/google
   - http://localhost:8080/api/auth/oauth2/callback/google
   ```

5. Click "CREATE"

6. **LƯU LẠI THÔNG TIN QUAN TRỌNG**:
   ```
   Client ID: [Copy và lưu lại]
   Client Secret: [Copy và lưu lại]
   ```
   ⚠️ **QUAN TRỌNG**: Lưu 2 thông tin này vào file an toàn, không commit lên Git!

---

## 3. Thêm Dependencies

### Bước 3.1: Cập nhật pom.xml

Thêm dependency Spring Security OAuth2:

```xml
<!-- Spring Security OAuth2 Client -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>

<!-- Google API Client (Optional - nếu cần gọi thêm Google APIs) -->
<dependency>
    <groupId>com.google.api-client</groupId>
    <artifactId>google-api-client</artifactId>
    <version>2.2.0</version>
</dependency>
```

### Bước 3.2: Reload Maven
```bash
mvn clean install
```

---

## 4. Cấu hình Application Properties

### Bước 4.1: Tạo file application-oauth.properties (Optional)

Hoặc thêm trực tiếp vào `application.properties`:

```properties
# ===== GOOGLE OAUTH2 CONFIGURATION =====

# Google OAuth2 Client ID và Secret
spring.security.oauth2.client.registration.google.client-id=YOUR_CLIENT_ID_HERE
spring.security.oauth2.client.registration.google.client-secret=YOUR_CLIENT_SECRET_HERE

# Redirect URI
spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}

# Scopes
spring.security.oauth2.client.registration.google.scope=openid,profile,email

# Authorization Grant Type
spring.security.oauth2.client.registration.google.authorization-grant-type=authorization_code

# Client Name
spring.security.oauth2.client.registration.google.client-name=Google

# Provider Configuration
spring.security.oauth2.client.provider.google.authorization-uri=https://accounts.google.com/o/oauth2/v2/auth
spring.security.oauth2.client.provider.google.token-uri=https://oauth2.googleapis.com/token
spring.security.oauth2.client.provider.google.user-info-uri=https://www.googleapis.com/oauth2/v3/userinfo
spring.security.oauth2.client.provider.google.user-name-attribute=sub

# JWT Configuration (nếu dùng JWT)
jwt.secret=your-secret-key-here-make-it-long-and-secure
jwt.expiration=86400000
```

### Bước 4.2: Tạo file .env (Recommended)

Tạo file `.env` ở root project (KHÔNG commit file này):

```env
GOOGLE_CLIENT_ID=your_actual_client_id_here
GOOGLE_CLIENT_SECRET=your_actual_client_secret_here
JWT_SECRET=your_jwt_secret_key_here
```

Thêm vào `.gitignore`:
```
.env
application-oauth.properties
```

---

## 5. Tạo Entity & Repository

### Bước 5.1: Cập nhật User Entity

Thêm các field cho OAuth2:

```java
@Entity
@Table(name = "users")
public class User {
    // ... existing fields ...
    
    @Column(name = "provider")
    private String provider; // "local", "google", "facebook"
    
    @Column(name = "provider_id")
    private String providerId; // Google user ID
    
    @Column(name = "avatar_url")
    private String avatarUrl;
    
    @Column(name = "email_verified")
    private Boolean emailVerified = false;
    
    // Getters and Setters
}
```

### Bước 5.2: Tạo Migration

Tạo file `V2__add_oauth_fields.sql`:

```sql
-- Add OAuth2 fields to users table
ALTER TABLE users ADD COLUMN provider VARCHAR(20) DEFAULT 'local';
ALTER TABLE users ADD COLUMN provider_id VARCHAR(255);
ALTER TABLE users ADD COLUMN avatar_url VARCHAR(500);
ALTER TABLE users ADD COLUMN email_verified BOOLEAN DEFAULT false;

-- Add index for faster lookup
CREATE INDEX idx_users_provider_id ON users(provider, provider_id);
```

---

## 6. Implement OAuth2 Service

### Bước 6.1: Tạo OAuth2UserInfo Interface

```java
package com.htai.exe201phapluatso.auth.oauth2;

import java.util.Map;

public interface OAuth2UserInfo {
    String getId();
    String getName();
    String getEmail();
    String getImageUrl();
}
```

### Bước 6.2: Tạo GoogleOAuth2UserInfo

```java
package com.htai.exe201phapluatso.auth.oauth2;

import java.util.Map;

public class GoogleOAuth2UserInfo implements OAuth2UserInfo {
    private Map<String, Object> attributes;

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getImageUrl() {
        return (String) attributes.get("picture");
    }
}
```

### Bước 6.3: Tạo CustomOAuth2UserService

```java
package com.htai.exe201phapluatso.auth.oauth2;

import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepo userRepo;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(oauth2User.getAttributes());
        
        // Process user
        User user = processOAuth2User(registrationId, userInfo);
        
        return new CustomOAuth2User(oauth2User, user);
    }

    private User processOAuth2User(String provider, OAuth2UserInfo userInfo) {
        Optional<User> userOptional = userRepo.findByProviderAndProviderId(provider, userInfo.getId());
        
        User user;
        if (userOptional.isPresent()) {
            // Update existing user
            user = userOptional.get();
            user.setFullName(userInfo.getName());
            user.setAvatarUrl(userInfo.getImageUrl());
        } else {
            // Create new user
            user = new User();
            user.setEmail(userInfo.getEmail());
            user.setFullName(userInfo.getName());
            user.setProvider(provider);
            user.setProviderId(userInfo.getId());
            user.setAvatarUrl(userInfo.getImageUrl());
            user.setEmailVerified(true);
            user.setRole("USER");
            // No password for OAuth users
        }
        
        return userRepo.save(user);
    }
}
```

### Bước 6.4: Tạo CustomOAuth2User

```java
package com.htai.exe201phapluatso.auth.oauth2;

import com.htai.exe201phapluatso.auth.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {
    private OAuth2User oauth2User;
    private User user;

    public CustomOAuth2User(OAuth2User oauth2User, User user) {
        this.oauth2User = oauth2User;
        this.user = user;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oauth2User.getAuthorities();
    }

    @Override
    public String getName() {
        return oauth2User.getName();
    }

    public User getUser() {
        return user;
    }
}
```

---

## 7. Tạo Controller

### Bước 7.1: Tạo OAuth2Controller

```java
package com.htai.exe201phapluatso.auth.controller;

import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.oauth2.CustomOAuth2User;
import com.htai.exe201phapluatso.auth.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/oauth2")
public class OAuth2Controller {

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/success")
    public RedirectView oauth2Success(@AuthenticationPrincipal CustomOAuth2User oauth2User) {
        User user = oauth2User.getUser();
        
        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail());
        
        // Redirect to frontend with token
        return new RedirectView("/html/oauth2-redirect.html?token=" + token);
    }

    @GetMapping("/callback/google")
    public Map<String, Object> googleCallback(@AuthenticationPrincipal CustomOAuth2User oauth2User) {
        User user = oauth2User.getUser();
        String token = jwtUtil.generateToken(user.getEmail());
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", user);
        
        return response;
    }
}
```

---

## 8. Cập nhật Frontend

### Bước 8.1: Tạo oauth2-redirect.html

```html
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Đang đăng nhập...</title>
</head>
<body>
    <div style="text-align: center; padding: 50px;">
        <h2>Đang xử lý đăng nhập...</h2>
        <p>Vui lòng đợi...</p>
    </div>
    
    <script>
        // Get token from URL
        const urlParams = new URLSearchParams(window.location.search);
        const token = urlParams.get('token');
        
        if (token) {
            // Save token
            localStorage.setItem('accessToken', token);
            
            // Redirect to home
            window.location.href = '/index.html';
        } else {
            alert('Đăng nhập thất bại');
            window.location.href = '/html/login.html';
        }
    </script>
</body>
</html>
```

### Bước 8.2: Cập nhật login.html

Thêm nút "Đăng nhập bằng Google":

```html
<!-- Existing login form -->

<!-- Divider -->
<div class="text-center my-3">
    <span class="divider-text">HOẶC</span>
</div>

<!-- Google Login Button -->
<a href="/oauth2/authorization/google" class="btn btn-outline-dark w-100 mb-3">
    <img src="https://www.google.com/favicon.ico" alt="Google" style="width: 20px; margin-right: 10px;">
    Đăng nhập bằng Google
</a>
```

---

## 9. Testing

### Bước 9.1: Test Flow
1. Start application
2. Truy cập http://localhost:8080/html/login.html
3. Click "Đăng nhập bằng Google"
4. Chọn tài khoản Google
5. Cho phép quyền truy cập
6. Kiểm tra redirect về trang chủ với token

### Bước 9.2: Debug Checklist
- [ ] Google Cloud Project đã tạo
- [ ] OAuth Consent Screen đã cấu hình
- [ ] Client ID và Secret đã đúng
- [ ] Redirect URI đã match
- [ ] Dependencies đã thêm
- [ ] Database migration đã chạy
- [ ] Service đã implement đúng

---

## 🎯 Tóm Tắt Các Bước

1. ✅ Tạo Google Cloud Project
2. ✅ Cấu hình OAuth2 Credentials
3. ✅ Thêm Dependencies vào pom.xml
4. ✅ Cấu hình application.properties
5. ✅ Cập nhật User Entity
6. ✅ Tạo OAuth2 Services
7. ✅ Tạo Controller
8. ✅ Cập nhật Frontend
9. ✅ Testing

---

## 📞 Troubleshooting

### Lỗi "redirect_uri_mismatch"
- Kiểm tra lại Authorized redirect URIs trong Google Console
- Đảm bảo URL match chính xác (http vs https, port number)

### Lỗi "invalid_client"
- Client ID hoặc Secret sai
- Kiểm tra lại application.properties

### User không được tạo
- Kiểm tra database migration
- Xem logs để debug

---

**Ngày tạo**: 29/12/2024
**Version**: 1.0
