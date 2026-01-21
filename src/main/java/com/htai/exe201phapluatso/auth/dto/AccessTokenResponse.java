package com.htai.exe201phapluatso.auth.dto;

/**
 * Response DTO for access token only.
 * Refresh token is now sent via HttpOnly cookie, not in response body.
 */
public record AccessTokenResponse(
        String accessToken,
        long expiresIn) {
}
