package com.htai.exe201phapluatso.auth.dto;

import com.htai.exe201phapluatso.auth.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @Email(message = "Email không hợp lệ")
        @NotBlank(message = "Email không được để trống")
        String email,
        
        @NotBlank(message = "Mật khẩu không được để trống")
        @ValidPassword
        String password,
        
        String fullName
) {}
