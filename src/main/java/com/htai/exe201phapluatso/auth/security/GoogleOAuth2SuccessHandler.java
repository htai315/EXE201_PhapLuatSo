package com.htai.exe201phapluatso.auth.security;

import com.htai.exe201phapluatso.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class GoogleOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    @Value("${app.frontend.redirect}")
    private String frontendRedirect;

    public GoogleOAuth2SuccessHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OidcUser oidc = (OidcUser) authentication.getPrincipal();

        String sub = oidc.getSubject();
        String email = oidc.getEmail();
        String name = oidc.getFullName();
        boolean verified = Boolean.TRUE.equals(oidc.getEmailVerified());

        var tokens = authService.loginWithGoogle(sub, email, name, verified);

        // DEV: redirect with tokens in URL fragment
        String url = frontendRedirect
                + "#access=" + URLEncoder.encode(tokens.accessToken(), StandardCharsets.UTF_8)
                + "&refresh=" + URLEncoder.encode(tokens.refreshToken(), StandardCharsets.UTF_8);

        response.sendRedirect(url);
    }
}
