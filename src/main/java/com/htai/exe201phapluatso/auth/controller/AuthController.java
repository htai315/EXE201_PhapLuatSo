package com.htai.exe201phapluatso.auth.controller;

import com.htai.exe201phapluatso.auth.dto.*;
import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import com.htai.exe201phapluatso.auth.security.AuthUserPrincipal;
import com.htai.exe201phapluatso.auth.service.AuthService;
import com.htai.exe201phapluatso.auth.service.EmailVerificationService;
import com.htai.exe201phapluatso.auth.service.UserService;
import com.htai.exe201phapluatso.auth.util.CookieUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.util.Map;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller with HttpOnly cookie-based refresh tokens.
 * 
 * Security model:
 * - Access token: returned in response body, stored in-memory by frontend
 * - Refresh token: set as HttpOnly cookie, never exposed to JavaScript
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService auth;
    private final UserService userService;
    private final UserRepo userRepo;
    private final EmailVerificationService emailVerificationService;
    private final CookieUtils cookieUtils;

    public AuthController(
            AuthService auth,
            UserService userService,
            UserRepo userRepo,
            EmailVerificationService emailVerificationService,
            CookieUtils cookieUtils) {
        this.auth = auth;
        this.userService = userService;
        this.userRepo = userRepo;
        this.emailVerificationService = emailVerificationService;
        this.cookieUtils = cookieUtils;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        auth.register(req);
        return ResponseEntity.ok(new MessageResponse(
                "Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản."));
    }

    /**
     * Login endpoint - returns access token in body, sets refresh token as HttpOnly
     * cookie.
     */
    @PostMapping("/login")
    public AccessTokenResponse login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest request,
            HttpServletResponse response) {
        String ipAddress = getClientIP(request);
        String userAgent = request.getHeader("User-Agent");

        TokenResponse tokens = auth.login(req, ipAddress, userAgent);

        // Set refresh token as HttpOnly cookie
        cookieUtils.addRefreshTokenCookie(response, tokens.refreshToken());

        // Return only access token in response body
        return new AccessTokenResponse(tokens.accessToken(), tokens.accessExpiresInSeconds());
    }

    /**
     * Refresh endpoint - reads refresh token from cookie, returns new access token.
     * Keeps the same refresh token (no rotation).
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        // Debug: log all received cookies
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            logger.warn("[Refresh] No cookies received in request");
        } else {
            logger.debug("[Refresh] Received {} cookies", cookies.length);
        }

        // Extract refresh token from cookie
        String refreshToken = cookieUtils.extractRefreshToken(cookies);

        if (refreshToken == null || refreshToken.isBlank()) {
            logger.warn("[Refresh] No refresh token found in cookies");
            return ResponseEntity.status(401).body(new MessageResponse("No refresh token provided"));
        }

        try {
            TokenResponse tokens = auth.refresh(new RefreshRequest(refreshToken));

            // No need to set new cookie - keeping the same refresh token
            // Return only access token in response body
            return ResponseEntity.ok(new AccessTokenResponse(tokens.accessToken(), tokens.accessExpiresInSeconds()));
        } catch (Exception e) {
            logger.warn("[Refresh] Token validation failed: {}", e.getMessage());
            // Clear cookie on any error (invalid, expired)
            cookieUtils.clearRefreshTokenCookie(response);
            return ResponseEntity.status(401).body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * Logout endpoint - revokes refresh token and clears cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieUtils.extractRefreshToken(request.getCookies());

        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                auth.logout(new RefreshRequest(refreshToken));
            } catch (Exception e) {
                // Ignore errors during logout - just clear cookie
            }
        }

        // Always clear cookie
        cookieUtils.clearRefreshTokenCookie(response);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok(new MessageResponse(
                "Xác thực email thành công! Bạn có thể đăng nhập ngay."));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@Valid @RequestBody ResendVerificationRequest req) {
        emailVerificationService.resendVerificationEmail(req.email());
        return ResponseEntity.ok(new MessageResponse(
                "Email xác thực đã được gửi lại. Vui lòng kiểm tra hộp thư."));
    }

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok("Auth controller is working");
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
            String email = principal.email();

            UserProfileResponse profile = userService.getUserProfile(email);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // Temporarily public for debugging - REMOVE IN PRODUCTION
    // debug endpoints removed

    /**
     * Get client IP address, handling proxy headers
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        return request.getRemoteAddr();
    }
}
