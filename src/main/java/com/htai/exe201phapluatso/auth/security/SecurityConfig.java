package com.htai.exe201phapluatso.auth.security;

import com.htai.exe201phapluatso.auth.oauth2.CustomOAuth2UserService;
import com.htai.exe201phapluatso.auth.oauth2.OAuth2AuthenticationFailureHandler;
import com.htai.exe201phapluatso.auth.oauth2.OAuth2AuthenticationSuccessHandler;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import com.htai.exe201phapluatso.auth.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

        private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

        private final CustomOAuth2UserService customOAuth2UserService;
        private final OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;
        private final OAuth2AuthenticationFailureHandler oauth2AuthenticationFailureHandler;
        private final OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient;

        // Constructor injection (best practice)
        public SecurityConfig(
                        CustomOAuth2UserService customOAuth2UserService,
                        OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler,
                        OAuth2AuthenticationFailureHandler oauth2AuthenticationFailureHandler,
                        OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.oauth2AuthenticationSuccessHandler = oauth2AuthenticationSuccessHandler;
        this.oauth2AuthenticationFailureHandler = oauth2AuthenticationFailureHandler;
        this.accessTokenResponseClient = accessTokenResponseClient;
    }

    // CORS Configuration Properties
    @Value("${app.security.cors.allowed-origins:http://localhost:8080,http://127.0.0.1:8080}")
    private String allowedOrigins;

    @Value("${app.security.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${app.security.cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS}")
    private String allowedMethods;

    @Value("${app.security.cors.allowed-headers:Authorization,Content-Type,X-Requested-With}")
    private String allowedHeaders;

    @Value("${app.security.cors.exposed-headers:Authorization}")
    private String exposedHeaders;

    // CSRF Configuration
    @Value("${app.security.csrf.enabled:false}")
    private boolean csrfEnabled;

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                // Split and trim origins to avoid whitespace issues
                if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
                        String[] origins = allowedOrigins.split(",");
                        for (String origin : origins) {
                                configuration.addAllowedOrigin(origin.trim());
                        }
                }

                configuration.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
                
                String[] headers = allowedHeaders.split(",");
                for (String header : headers) {
                        configuration.addAllowedHeader(header.trim());
                }
                
                configuration.setAllowCredentials(allowCredentials);
                
                String[] exposed = exposedHeaders.split(",");
                for (String header : exposed) {
                        configuration.addExposedHeader(header.trim());
                }

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/api/**", configuration);
                return source;
        }

        @Bean
        public SecurityFilterChain filterChain(
                        HttpSecurity http,
                        JwtService jwtService,
                        UserRepo userRepo) throws Exception {

                JwtAuthFilter jwtFilter = new JwtAuthFilter(jwtService, userRepo);

                http
                                // CORS configuration
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                // CSRF configuration based on environment
                                .csrf(csrf -> {
                                        if (!csrfEnabled) {
                                                csrf.disable();
                                        }
                                })
                                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // public pages + assets
                                                .requestMatchers(
                                                                "/", "/index.html", "/app.html",
                                                                "/css/**", "/img/**", "/scripts/**",
                                                                "/html/**", "/favicon.ico", "/uploads/**")
                                                .permitAll()

                                                // payment result page (public - VNPay redirect without token)
                                                .requestMatchers("/payment-result.html").permitAll()

                                                // auth endpoints (public - không cần token)
                                                .requestMatchers("/api/auth/register", "/api/auth/login",
                                                                "/api/auth/refresh", "/api/auth/logout",
                                                                "/api/auth/password-reset/**",
                                                                "/api/auth/verify-email",
                                                                "/api/auth/resend-verification",
                                                                "/oauth2/**", "/login/**")
                                                .permitAll()

                                                // payment IPN callback (public - VNPay server-to-server)
                                                .requestMatchers("/api/payment/vnpay-ipn").permitAll()

                                                // PayOS webhook callback (public - server-to-server)
                                                .requestMatchers("/api/payment/webhook").permitAll()

                                                // Payment status check (public - for redirect page)
                                                .requestMatchers("/api/payment/status/**").permitAll()

                                                // admin endpoints (protected - ADMIN role required)
                                                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                                                // auth endpoints (protected - cần token)
                                                .requestMatchers("/api/auth/me", "/api/auth/test").authenticated()

                                                // mọi thứ khác mới cần đăng nhập
                                                .anyRequest().authenticated())
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        String path = request.getRequestURI();
                                                        if (path.startsWith("/api/")) {
                                                                // API endpoints: return 401 JSON response
                                                                response.setStatus(401);
                                                                response.setContentType("application/json");
                                                                response.getWriter().write(
                                                                                "{\"error\":\"Unauthorized\",\"message\":\""
                                                                                                + authException.getMessage()
                                                                                                + "\"}");
                                                        } else {
                                                                // HTML pages: redirect to login
                                                                response.sendRedirect("/html/login.html");
                                                        }
                                                })
                                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                                        logger.error("Access Denied (403): {} - Reason: {}", 
                                                                request.getRequestURI(), accessDeniedException.getMessage());
                                                        response.setStatus(403);
                                                        response.setContentType("application/json");
                                                        response.getWriter().write(
                                                                "{\"error\":\"Forbidden\",\"message\":\"Access Denied: " 
                                                                + accessDeniedException.getMessage() + "\"}");
                                                }))
                                // OAuth2 Login Configuration
                                .oauth2Login(oauth2 -> oauth2
                                                .tokenEndpoint(token -> token
                                                                .accessTokenResponseClient(accessTokenResponseClient))
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .oidcUserService(customOAuth2UserService))
                                                .successHandler(oauth2AuthenticationSuccessHandler)
                                                .failureHandler(oauth2AuthenticationFailureHandler))
                                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
