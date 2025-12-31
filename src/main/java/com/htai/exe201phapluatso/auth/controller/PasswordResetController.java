package com.htai.exe201phapluatso.auth.controller;

import com.htai.exe201phapluatso.auth.dto.ResetPasswordRequest;
import com.htai.exe201phapluatso.auth.dto.SendOtpRequest;
import com.htai.exe201phapluatso.auth.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/password-reset")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    /**
     * Bước 1: Gửi OTP đến email
     * POST /api/auth/password-reset/send-otp
     */
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        passwordResetService.sendOtp(request.email());
        return ResponseEntity.ok(Map.of(
                "message", "Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư."
        ));
    }

    /**
     * Bước 2: Xác thực OTP và đặt lại mật khẩu
     * POST /api/auth/password-reset/reset
     */
    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.email(), request.otp(), request.newPassword());
        return ResponseEntity.ok(Map.of(
                "message", "Đặt lại mật khẩu thành công. Bạn có thể đăng nhập với mật khẩu mới."
        ));
    }
}
