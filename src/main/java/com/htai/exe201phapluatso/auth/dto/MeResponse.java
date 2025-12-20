package com.htai.exe201phapluatso.auth.dto;

import java.util.Collection;

/**
 * Thông tin tài khoản trả về từ /api/auth/me
 */
public record MeResponse(
        String email,
        Collection<String> roles
) {
}


