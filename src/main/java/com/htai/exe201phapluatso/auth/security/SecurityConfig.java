package com.htai.exe201phapluatso.auth.security;

import com.htai.exe201phapluatso.auth.repo.UserRepo;
import com.htai.exe201phapluatso.auth.service.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

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

                        // auth endpoints (public - không cần token)
                        .requestMatchers("/api/auth/register", "/api/auth/login", 
                                       "/api/auth/refresh", "/api/auth/logout",
                                       "/oauth2/**", "/login/**").permitAll()

                        // auth endpoints (protected - cần token)
                        .requestMatchers("/api/auth/me", "/api/auth/test").authenticated()

                        // mọi thứ khác mới cần đăng nhập
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
