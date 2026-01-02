package com.htai.exe201phapluatso.auth.security;

import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import com.htai.exe201phapluatso.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

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

            // Check user status directly from DB (no cache - instant ban/unban effect)
            User user = userRepo.findById(uid).orElse(null);
            
            if (user == null || !user.isEnabled()) {
                // User deleted or disabled
                chain.doFilter(request, response);
                return;
            }
            
            if (!user.isActive()) {
                // User is banned - return 403 with ban message
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                String message = user.getBanReason() != null 
                    ? "Tài khoản của bạn đã bị khóa. Lý do: " + user.getBanReason()
                    : "Tài khoản của bạn đã bị khóa.";
                response.getWriter().write("{\"error\":\"ACCOUNT_BANNED\",\"message\":\"" + message + "\"}");
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

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.debug("JWT token expired");
        } catch (io.jsonwebtoken.JwtException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Error processing JWT token", e);
        }

        chain.doFilter(request, response);
    }
}
