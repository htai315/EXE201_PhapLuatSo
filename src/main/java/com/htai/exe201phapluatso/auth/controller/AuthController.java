package com.htai.exe201phapluatso.auth.controller;

import com.htai.exe201phapluatso.auth.dto.*;
import com.htai.exe201phapluatso.auth.security.AuthUserPrincipal;
import com.htai.exe201phapluatso.auth.service.AuthService;
import com.htai.exe201phapluatso.auth.service.EmailVerificationService;
import com.htai.exe201phapluatso.auth.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final UserService userService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(
            AuthService auth, 
            UserService userService, 
            EmailVerificationService emailVerificationService) {
        this.auth = auth;
        this.userService = userService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        auth.register(req);
        return ResponseEntity.ok(new MessageResponse(
            "Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản."
        ));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return auth.refresh(req);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody RefreshRequest req) {
        auth.logout(req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok(new MessageResponse(
            "Xác thực email thành công! Bạn có thể đăng nhập ngay."
        ));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@Valid @RequestBody ResendVerificationRequest req) {
        emailVerificationService.resendVerificationEmail(req.email());
        return ResponseEntity.ok(new MessageResponse(
            "Email xác thực đã được gửi lại. Vui lòng kiểm tra hộp thư."
        ));
    }

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok("Auth controller is working");
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        try {
            // Cast principal to AuthUserPrincipal to get email
            AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
            String email = principal.email();
            
            UserProfileResponse profile = userService.getUserProfile(email);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
