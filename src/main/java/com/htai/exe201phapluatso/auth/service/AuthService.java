package com.htai.exe201phapluatso.auth.service;

import com.htai.exe201phapluatso.auth.dto.*;
import com.htai.exe201phapluatso.auth.entity.*;
import com.htai.exe201phapluatso.auth.repo.*;
import com.htai.exe201phapluatso.common.exception.BadRequestException;
import com.htai.exe201phapluatso.common.exception.ForbiddenException;
import com.htai.exe201phapluatso.common.exception.NotFoundException;
import com.htai.exe201phapluatso.common.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepo userRepo;
    private final RoleRepo roleRepo;
    private final PlanRepo planRepo;
    private final RefreshTokenRepo refreshTokenRepo;

    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final EmailVerificationService emailVerificationService;

    @Value("${app.jwt.access-minutes}")
    private long accessMinutes;

    public AuthService(
            UserRepo userRepo,
            RoleRepo roleRepo,
            PlanRepo planRepo,
            RefreshTokenRepo refreshTokenRepo,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenService tokenService,
            @Lazy EmailVerificationService emailVerificationService
    ) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.planRepo = planRepo;
        this.refreshTokenRepo = refreshTokenRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenService = tokenService;
        this.emailVerificationService = emailVerificationService;
    }

    // -------- LOCAL REGISTER --------
    public void register(RegisterRequest req) {
        String email = req.email().toLowerCase().trim();
        if (userRepo.existsByEmail(email)) {
            throw new BadRequestException("Email đã tồn tại");
        }

        Role userRole = roleRepo.findByName("USER")
                .orElseThrow(() -> new NotFoundException("Không tìm thấy role USER"));

        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setFullName(req.fullName());
        u.setProvider("LOCAL");
        u.setProviderId(null);
        u.setEmailVerified(false);
        u.setEnabled(true);
        u.getRoles().add(userRole);

        User saved = userRepo.save(u);

        // Gửi email verification
        emailVerificationService.createAndSendVerificationToken(saved);

        // Database trigger will automatically create FREE credits (10 chat credits)
    }

    // -------- LOCAL LOGIN --------
    public TokenResponse login(LoginRequest req) {
        long startTime = System.currentTimeMillis();
        String email = req.email().toLowerCase().trim();

        long dbStart = System.currentTimeMillis();
        User u = userRepo.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Email hoặc mật khẩu không đúng"));
        log.debug("LOGIN: DB query took {}ms", System.currentTimeMillis() - dbStart);

        if (!u.isEnabled()) {
            throw new ForbiddenException("Tài khoản đã bị khóa");
        }
        if (u.getPasswordHash() == null) {
            throw new BadRequestException("Tài khoản này đăng nhập bằng Google");
        }
        
        long bcryptStart = System.currentTimeMillis();
        boolean passwordMatch = passwordEncoder.matches(req.password(), u.getPasswordHash());
        log.debug("LOGIN: BCrypt verify took {}ms", System.currentTimeMillis() - bcryptStart);
        
        if (!passwordMatch) {
            throw new UnauthorizedException("Email hoặc mật khẩu không đúng");
        }
        
        // Kiểm tra email đã xác thực chưa (chỉ với LOCAL provider)
        if ("LOCAL".equals(u.getProvider()) && !u.isEmailVerified()) {
            throw new UnauthorizedException(
                "Vui lòng xác thực email trước khi đăng nhập. Kiểm tra hộp thư của bạn."
            );
        }

        long tokenStart = System.currentTimeMillis();
        TokenResponse response = issueTokens(u);
        log.debug("LOGIN: Token generation took {}ms", System.currentTimeMillis() - tokenStart);
        
        log.info("LOGIN: Total time {}ms for user {}", System.currentTimeMillis() - startTime, email);
        return response;
    }

    // -------- REFRESH (ROTATE) --------
    public TokenResponse refresh(RefreshRequest req) {
        User u = tokenService.validateAndRotate(req.refreshToken());
        return issueTokens(u);
    }

    public void logout(RefreshRequest req) {
        tokenService.revoke(req.refreshToken());
    }

    // -------- GOOGLE LOGIN (called by success handler) --------
    public TokenResponse loginWithGoogle(String googleSub, String email, String fullName, boolean emailVerified) {
        String normalized = email.toLowerCase().trim();

        User u = userRepo.findByProviderAndProviderId("GOOGLE", googleSub)
                .orElseGet(() -> userRepo.findByEmail(normalized).orElse(null));

        Role userRole = roleRepo.findByName("USER")
                .orElseThrow(() -> new NotFoundException("Không tìm thấy role USER"));

        if (u == null) {
            // create new user
            u = new User();
            u.setEmail(normalized);
            u.setFullName(fullName);
            u.setProvider("GOOGLE");
            u.setProviderId(googleSub);
            u.setEmailVerified(emailVerified);
            u.setEnabled(true);
            u.getRoles().add(userRole);

            userRepo.save(u);

            // Database trigger will automatically create FREE credits (10 chat credits)
        } else {
            // link google if not linked yet
            if (u.getProviderId() == null) {
                u.setProvider("GOOGLE");
                u.setProviderId(googleSub);
            }
            if (!u.getRoles().contains(userRole)) {
                u.getRoles().add(userRole);
            }
            u.setEmailVerified(u.isEmailVerified() || emailVerified);
            userRepo.save(u);
        }

        return issueTokens(u);
    }

    // -------- Issue Access + Refresh --------
    public TokenResponse issueTokens(User u) {
        var roles = u.getRoles().stream().map(Role::getName).toList();
        String access = jwtService.createAccessToken(u.getId(), u.getEmail(), roles);

        var refresh = tokenService.issueRefreshToken(u);

        long accessExpSeconds = accessMinutes * 60;
        return new TokenResponse(access, refresh.raw(), accessExpSeconds);
    }
}
