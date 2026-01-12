package com.htai.exe201phapluatso.auth.dto;

import com.htai.exe201phapluatso.auth.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
    @NotBlank(message = "Mật khẩu hiện tại không được để trống")
    String currentPassword,
    
    @NotBlank(message = "Mật khẩu mới không được để trống")
    @ValidPassword
    String newPassword
) {}
