package com.htai.exe201phapluatso.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.htai.exe201phapluatso.auth.dto.RateLimitInfo;
import com.htai.exe201phapluatso.auth.service.RateLimitService;
import com.htai.exe201phapluatso.auth.service.SecurityAuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Filter to enforce rate limiting on sensitive endpoints.
 * Runs before JwtAuthFilter to block excessive requests early.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimitService rateLimitService;
    private final SecurityAuditService securityAuditService;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(
            RateLimitService rateLimitService,
            SecurityAuditService securityAuditService,
            ObjectMapper objectMapper
    ) {
        this.rateLimitService = rateLimitService;
        this.securityAuditService = securityAuditService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        String endpoint = request.getRequestURI();
        
        // Only rate limit specific endpoints
        if (!rateLimitService.isRateLimitedEndpoint(endpoint)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = getClientIP(request);
        
        if (!rateLimitService.isAllowed(ip, endpoint)) {
            RateLimitInfo info = rateLimitService.getRateLimitInfo(ip, endpoint);
            
            log.warn("Rate limit exceeded for IP {} on endpoint {}", ip, endpoint);
            securityAuditService.logRateLimitExceeded(ip, endpoint);
            
            sendRateLimitResponse(response, info);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendRateLimitResponse(HttpServletResponse response, RateLimitInfo info) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        // Add Retry-After header
        response.setHeader("Retry-After", String.valueOf(info.retryAfterSeconds()));
        response.setHeader("X-RateLimit-Limit", String.valueOf(info.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(info.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(info.resetTimestamp()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Quá nhiều yêu cầu. Vui lòng thử lại sau " + info.retryAfterSeconds() + " giây.");
        body.put("code", "RATE_LIMIT_EXCEEDED");
        body.put("retryAfter", info.retryAfterSeconds());
        body.put("limit", info.limit());
        body.put("remaining", info.remaining());

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    /**
     * Get client IP address, considering proxy headers.
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP in the chain (original client)
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }
}
