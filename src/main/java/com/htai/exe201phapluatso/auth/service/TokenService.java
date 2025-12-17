package com.htai.exe201phapluatso.auth.service;

import com.htai.exe201phapluatso.auth.entity.RefreshToken;
import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.repo.RefreshTokenRepo;
import com.htai.exe201phapluatso.common.HashUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TokenService {

    public record IssuedRefreshToken(String raw, LocalDateTime expiresAt) {}

    private final RefreshTokenRepo refreshRepo;

    @Value("${app.jwt.refresh-days}")
    private long refreshDays;

    public TokenService(RefreshTokenRepo refreshRepo) {
        this.refreshRepo = refreshRepo;
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
     * Validate refresh token, then ROTATE:
     * - mark old token revoked
     * - return the user to issue a new token pair
     */
    public User validateAndRotate(String rawToken) {
        String hash = HashUtil.sha256Base64(rawToken.trim());

        RefreshToken token = refreshRepo.findByTokenHash(hash)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (token.getRevokedAt() != null) throw new RuntimeException("Refresh token revoked");
        if (LocalDateTime.now().isAfter(token.getExpiresAt())) throw new RuntimeException("Refresh token expired");

        token.setRevokedAt(LocalDateTime.now());
        refreshRepo.save(token);

        return token.getUser();
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
