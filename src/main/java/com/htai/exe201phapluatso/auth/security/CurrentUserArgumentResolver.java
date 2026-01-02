package com.htai.exe201phapluatso.auth.security;

import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolver for @CurrentUser annotation
 * Injects the authenticated user from SecurityContext
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserRepo userRepo;

    public CurrentUserArgumentResolver(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) 
            && parameter.getParameterType().equals(User.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        
        // If principal is AuthUserPrincipal, get user by ID
        if (principal instanceof AuthUserPrincipal) {
            AuthUserPrincipal authPrincipal = (AuthUserPrincipal) principal;
            return userRepo.findById(authPrincipal.userId()).orElse(null);
        }
        
        // Fallback: try to get by email (for OAuth2)
        String email = authentication.getName();
        return userRepo.findByEmail(email).orElse(null);
    }
}
