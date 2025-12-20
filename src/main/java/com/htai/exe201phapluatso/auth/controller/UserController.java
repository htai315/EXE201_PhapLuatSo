package com.htai.exe201phapluatso.auth.controller;

import com.htai.exe201phapluatso.auth.dto.ChangePasswordRequest;
import com.htai.exe201phapluatso.auth.security.AuthUserPrincipal;
import com.htai.exe201phapluatso.auth.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
        String email = principal.email();
        userService.changePassword(email, request);
        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
    }

    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(
            @RequestParam("avatar") MultipartFile file,
            Authentication authentication
    ) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
        String email = principal.email();
        String avatarUrl = userService.uploadAvatar(email, file);
        return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
    }
}
