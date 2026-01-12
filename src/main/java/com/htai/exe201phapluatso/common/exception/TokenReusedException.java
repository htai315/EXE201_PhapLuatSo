package com.htai.exe201phapluatso.common.exception;

/**
 * Exception thrown when a refresh token reuse is detected.
 * This indicates a potential security breach - someone may have stolen the token.
 * When this happens, ALL tokens for the user should be revoked.
 */
public class TokenReusedException extends RuntimeException {
    
    private final Long userId;
    private final String tokenId;

    public TokenReusedException(Long userId, String tokenId) {
        super("Phát hiện sử dụng lại token. Tất cả phiên đăng nhập đã bị hủy vì lý do bảo mật.");
        this.userId = userId;
        this.tokenId = tokenId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTokenId() {
        return tokenId;
    }
}
