package com.htai.exe201phapluatso.auth.oauth2;

import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.service.JwtService;
import com.htai.exe201phapluatso.auth.service.TokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TokenService tokenService;

    @Value("${app.frontend.redirect:http://localhost:8080/html/oauth2-redirect.html}")
    private String frontendRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        if (response.isCommitted()) {
            logger.debug("Response has already been committed. Unable to redirect.");
            return;
        }

        CustomOidcUser oidcUser = (CustomOidcUser) authentication.getPrincipal();
        User user = oidcUser.getUser();
        
        // Generate access token
        String accessToken = jwtService.createAccessToken(
            user.getId(),
            user.getEmail(),
            user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toList())
        );
        
        // Generate refresh token
        TokenService.IssuedRefreshToken refreshToken = tokenService.issueRefreshToken(user);
        
        logger.info("OAuth2 login successful for user: {}", user.getEmail());
        
        // Redirect to frontend with both tokens
        String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken.raw())
                .build().toUriString();
        
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
