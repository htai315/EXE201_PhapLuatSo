package com.htai.exe201phapluatso.auth.service;

import com.htai.exe201phapluatso.auth.entity.RefreshToken;
import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.repo.RefreshTokenRepo;
import com.htai.exe201phapluatso.common.HashUtil;
import com.htai.exe201phapluatso.common.exception.TokenReusedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    public record IssuedRefreshToken(String raw, LocalDateTime expiresAt) {}

    private final RefreshTokenRepo refreshRepo;
    private final SecurityAuditService securityAuditService;

    @Value("${app.jwt.refresh-days}")
    private long refreshDays;

    public TokenService(RefreshTokenRepo refreshRepo, SecurityAuditService securityAuditService) {
        this.refreshRepo = refreshRepo;
        this.securityAuditService = securityAuditService;
    }

    public IssuedRefreshToken issueRefreshToken(User user) {
        String raw = UUID.randomUUID() + "." + UUID.randomUUID();
        String hash = HashUtil.sha256Base64(raw);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime exp = now.plusDays(refreshDays);

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(hash);
        rt.setExpiresAt(exp);
        rt.setRevokedAt(null);
        refreshRepo.save(rt);

        return new IssuedRefreshToken(raw, exp);
    }

    /**
     * Validate refresh token, then ROTATE with reuse detection:
     * - If token was already used (usedAt != null), this is a SECURITY BREACH
     *   → Revoke ALL tokens for this user and throw TokenReusedException
     * - Otherwise, mark token as used and return user for new token pair
     */
    @Transactional
    public User validateAndRotate(String rawToken) {
        String hash = HashUtil.sha256Base64(rawToken.trim());

        RefreshToken token = refreshRepo.findByTokenHash(hash)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        // Check if token was already used (REUSE DETECTION)
        if (token.getUsedAt() != null) {
            log.warn("TOKEN REUSE DETECTED for user {} - revoking all tokens", token.getUser().getId());
            
            // Revoke ALL tokens for this user - security breach response
            revokeAllUserTokens(token.getUser().getId());
            
            // Log security event (ipAddress not available in this context, use "unknown")
            securityAuditService.logTokenReuse(token.getUser().getId(), "unknown", hash);
            
            throw new TokenReusedException(token.getUser().getId(), hash);
        }

        if (token.getRevokedAt() != null) {
            throw new RuntimeException("Refresh token revoked");
        }
        if (LocalDateTime.now().isAfter(token.getExpiresAt())) {
            throw new RuntimeException("Refresh token expired");
        }

        // Mark token as used (for reuse detection) and revoked
        LocalDateTime now = LocalDateTime.now();
        token.setUsedAt(now);
        token.setRevokedAt(now);
        refreshRepo.save(token);

        // Log token rotation (ipAddress not available in this context, use "unknown")
        securityAuditService.logTokenRotation(token.getUser().getId(), "unknown");

        return token.getUser();
    }

    /**
     * Issue new token and link it to the old token (for chain tracking)
     */
    @Transactional
    public IssuedRefreshToken rotateToken(User user, Long oldTokenId) {
        IssuedRefreshToken newToken = issueRefreshToken(user);
        
        // Update old token with reference to new token
        if (oldTokenId != null) {
            refreshRepo.findById(oldTokenId).ifPresent(oldToken -> {
                oldToken.setReplacedByTokenId(
                    refreshRepo.findByTokenHash(HashUtil.sha256Base64(newToken.raw()))
                        .map(RefreshToken::getId)
                        .orElse(null)
                );
                refreshRepo.save(oldToken);
            });
        }
        
        return newToken;
    }

    /**
     * Revoke all tokens for a user (used when token reuse is detected)
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        int revokedCount = refreshRepo.revokeAllUserTokens(userId, LocalDateTime.now());
        log.info("Revoked {} tokens for user {}", revokedCount, userId);
    }

    public void revoke(String rawToken) {
        String hash = HashUtil.sha256Base64(rawToken.trim());
        refreshRepo.findByTokenHash(hash).ifPresent(t -> {
            if (t.getRevokedAt() == null) {
                t.setRevokedAt(LocalDateTime.now());
                refreshRepo.save(t);
            }
        });
    }
}
