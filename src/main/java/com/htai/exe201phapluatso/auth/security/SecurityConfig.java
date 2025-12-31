package com.htai.exe201phapluatso.auth.security;

import com.htai.exe201phapluatso.auth.oauth2.CustomOAuth2UserService;
import com.htai.exe201phapluatso.auth.oauth2.OAuth2AuthenticationFailureHandler;
import com.htai.exe201phapluatso.auth.oauth2.OAuth2AuthenticationSuccessHandler;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import com.htai.exe201phapluatso.auth.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;

    @Autowired
    private OAuth2AuthenticationFailureHandler oauth2AuthenticationFailureHandler;

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtService jwtService,
            UserRepo userRepo
    ) throws Exception {

        JwtAuthFilter jwtFilter = new JwtAuthFilter(jwtService, userRepo);

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // public pages + assets
                        .requestMatchers(
                                "/", "/index.html", "/app.html",
                                "/css/**", "/img/**", "/scripts/**",
                                "/html/**", "/favicon.ico", "/uploads/**"
                        ).permitAll()

                        // payment result page (public - VNPay redirect without token)
                        .requestMatchers("/payment-result.html").permitAll()

                        // auth endpoints (public - không cần token)
                        .requestMatchers("/api/auth/register", "/api/auth/login", 
                                       "/api/auth/refresh", "/api/auth/logout",
                                       "/api/auth/password-reset/**",
                                       "/oauth2/**", "/login/**").permitAll()

                        // payment IPN callback (public - VNPay server-to-server)
                        .requestMatchers("/api/payment/vnpay-ipn").permitAll()

                        // auth endpoints (protected - cần token)
                        .requestMatchers("/api/auth/me", "/api/auth/test").authenticated()

                        // mọi thứ khác mới cần đăng nhập
                        .anyRequest().authenticated()
                )
                // OAuth2 Login Configuration
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(customOAuth2UserService)
                        )
                        .successHandler(oauth2AuthenticationSuccessHandler)
                        .failureHandler(oauth2AuthenticationFailureHandler)
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
