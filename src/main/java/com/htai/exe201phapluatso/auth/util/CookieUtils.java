package com.htai.exe201phapluatso.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Utility class for managing HttpOnly refresh token cookies.
 * Provides consistent cookie creation and clearing across auth endpoints.
 */
@Component
public class CookieUtils {

    private static final Logger log = LoggerFactory.getLogger(CookieUtils.class);

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    // Use "/" to ensure cookie is sent for all paths including OAuth redirect
    public static final String COOKIE_PATH = "/";

    @Value("${app.jwt.refresh-days:7}")
    private long refreshDays;

    @Value("${app.security.cookie.same-site:Lax}")
    private String sameSite;

    @Value("${app.security.cookie.secure:false}")
    private boolean secureCookie;

    @Value("${app.security.cookie.domain:}")
    private String cookieDomain;

    /**
     * Create and add refresh token cookie to response.
     * 
     * @param response     HTTP response to add cookie to
     * @param refreshToken The raw refresh token value
     */
    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        // Use addHeader to properly set HttpOnly cookie with SameSite
        // (Jakarta Cookie API doesn't support SameSite attribute directly)
        StringBuilder cookieHeader = new StringBuilder();
        cookieHeader.append(String.format(
                "%s=%s; Path=%s; Max-Age=%d; HttpOnly; SameSite=%s",
                REFRESH_TOKEN_COOKIE_NAME,
                refreshToken,
                COOKIE_PATH,
                (int) (refreshDays * 24 * 60 * 60),
                sameSite));

        if (secureCookie) {
            cookieHeader.append("; Secure");
        }

        if (!cookieDomain.isEmpty()) {
            cookieHeader.append("; Domain=").append(cookieDomain);
        }

        String cookie = cookieHeader.toString();
        log.info("[CookieUtils] Setting refresh token cookie - Secure={}, SameSite={}, Domain={}, Path={}", 
                secureCookie, sameSite, cookieDomain.isEmpty() ? "(none)" : cookieDomain, COOKIE_PATH);
        log.debug("[CookieUtils] Full cookie header: {}", cookie.replaceAll("=.*?;", "=***;"));
        
        response.addHeader("Set-Cookie", cookie);
    }

    /**
     * Clear refresh token cookie from response.
     * Used for logout and error cases.
     * 
     * @param response HTTP response to clear cookie from
     */
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        // Use addHeader to properly clear HttpOnly cookie with SameSite
        StringBuilder cookieHeader = new StringBuilder();
        cookieHeader.append(String.format(
                "%s=; Path=%s; Max-Age=0; HttpOnly; SameSite=%s",
                REFRESH_TOKEN_COOKIE_NAME,
                COOKIE_PATH,
                sameSite));

        if (secureCookie) {
            cookieHeader.append("; Secure");
        }

        if (!cookieDomain.isEmpty()) {
            cookieHeader.append("; Domain=").append(cookieDomain);
        }

        response.addHeader("Set-Cookie", cookieHeader.toString());
    }

    /**
     * Extract refresh token from cookies array.
     * 
     * @param cookies Array of cookies from request
     * @return Refresh token value or null if not found
     */
    public String extractRefreshToken(Cookie[] cookies) {
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
