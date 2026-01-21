package com.htai.exe201phapluatso.auth.oauth2;

import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.service.TokenService;
import com.htai.exe201phapluatso.auth.util.CookieUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth2 success handler - Pure Option B implementation.
 * 
 * Security model:
 * - Sets refresh token as HttpOnly cookie
 * - Redirects to frontend WITHOUT any tokens in URL
 * - Frontend obtains access token by calling /api/auth/refresh
 */
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final TokenService tokenService;
    private final CookieUtils cookieUtils;

    @Value("${app.frontend.redirect-url:http://localhost:8080/html/oauth2-redirect.html}")
    private String frontendRedirectUrl;

    public OAuth2AuthenticationSuccessHandler(
            TokenService tokenService,
            CookieUtils cookieUtils) {
        this.tokenService = tokenService;
        this.cookieUtils = cookieUtils;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        if (response.isCommitted()) {
            logger.debug("Response has already been committed. Unable to redirect.");
            return;
        }

        CustomOidcUser oidcUser = (CustomOidcUser) authentication.getPrincipal();
        User user = oidcUser.getUser();

        // Generate refresh token and set as HttpOnly cookie
        // (Access token will be obtained by frontend calling /api/auth/refresh)
        TokenService.IssuedRefreshToken refreshToken = tokenService.issueRefreshToken(user);
        cookieUtils.addRefreshTokenCookie(response, refreshToken.raw());

        logger.info("OAuth2 login successful for user: {}. Refresh cookie set.", user.getEmail());

        // Redirect to frontend WITHOUT any tokens in URL (Pure Option B)
        getRedirectStrategy().sendRedirect(request, response, frontendRedirectUrl);
    }
}
