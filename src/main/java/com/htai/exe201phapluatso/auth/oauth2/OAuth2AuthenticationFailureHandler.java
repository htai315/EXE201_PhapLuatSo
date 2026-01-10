package com.htai.exe201phapluatso.auth.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        
        logger.error("OAuth2 authentication failed: {}", exception.getMessage());
        
        String message = exception.getLocalizedMessage();
        if (message == null || message.isEmpty()) {
            message = "Đăng nhập Google thất bại";
        }
        
        String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
        String targetUrl = "/html/login.html?error=oauth2_failed&message=" + encodedMessage;
        
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
