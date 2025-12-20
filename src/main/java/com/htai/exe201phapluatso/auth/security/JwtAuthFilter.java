package com.htai.exe201phapluatso.auth.security;

import com.htai.exe201phapluatso.auth.repo.UserRepo;
import com.htai.exe201phapluatso.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepo userRepo;

    public JwtAuthFilter(JwtService jwtService, UserRepo userRepo) {
        this.jwtService = jwtService;
        this.userRepo = userRepo;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();

        try {
            Claims claims = jwtService.parse(token);

            Long uid = claims.get("uid", Long.class);
            String email = claims.getSubject();

            if (uid == null || email == null) {
                chain.doFilter(request, response);
                return;
            }

            var user = userRepo.findById(uid).orElse(null);
            if (user == null || !user.isEnabled()) {
                chain.doFilter(request, response);
                return;
            }

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.get("roles", List.class);

            var authorities = roles.stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .toList();

            var principal = new AuthUserPrincipal(uid, email);

            var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (Exception ignored) {
        }

        chain.doFilter(request, response);
    }
}
