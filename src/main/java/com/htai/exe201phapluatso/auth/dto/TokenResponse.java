package com.htai.exe201phapluatso.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long accessExpiresInSeconds
) {}
