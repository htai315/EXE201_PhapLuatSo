package com.htai.exe201phapluatso.auth.service;

import com.htai.exe201phapluatso.auth.dto.ChangePasswordRequest;
import com.htai.exe201phapluatso.auth.dto.UserProfileResponse;
import com.htai.exe201phapluatso.auth.entity.Role;
import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import com.htai.exe201phapluatso.common.exception.BadRequestException;
import com.htai.exe201phapluatso.common.exception.NotFoundException;
import com.htai.exe201phapluatso.common.exception.UnauthorizedException;
import com.htai.exe201phapluatso.common.service.CloudinaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;

    // Cloudinary folder for avatars
    private static final String AVATAR_FOLDER = "avatars";

    public UserService(UserRepo userRepo, PasswordEncoder passwordEncoder, CloudinaryService cloudinaryService) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.cloudinaryService = cloudinaryService;
    }

    public UserProfileResponse getUserProfile(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        // Ưu tiên role ADMIN nếu user có nhiều role
        String role = user.getRoles().stream()
                .map(Role::getName)
                .filter(r -> "ADMIN".equals(r))
                .findFirst()
                .orElse(user.getRoles().stream()
                        .map(Role::getName)
                        .findFirst()
                        .orElse("USER"));

        return new UserProfileResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            role,
            user.getAvatarUrl()
        );
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        // Kiểm tra nếu user đăng nhập bằng Google
        if (!"LOCAL".equals(user.getProvider())) {
            throw new BadRequestException("Tài khoản đăng nhập bằng " + user.getProvider() + " không thể đổi mật khẩu");
        }

        // Kiểm tra mật khẩu hiện tại
        if (user.getPasswordHash() == null || 
            !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Mật khẩu hiện tại không đúng");
        }

        // Cập nhật mật khẩu mới
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepo.save(user);
        
        log.info("Password changed for user: {}", email);
    }

    @Transactional
    public String uploadAvatar(String email, MultipartFile file) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        // Delete old avatar from Cloudinary if exists
        if (user.getAvatarUrl() != null && user.getAvatarUrl().contains("cloudinary.com")) {
            String publicId = cloudinaryService.extractPublicId(user.getAvatarUrl());
            if (publicId != null) {
                cloudinaryService.deleteFile(publicId);
            }
        }

        // Upload new avatar to Cloudinary
        String avatarUrl = cloudinaryService.uploadImage(file, AVATAR_FOLDER);

        // Update user avatar URL
        user.setAvatarUrl(avatarUrl);
        userRepo.save(user);
        
        log.info("Avatar uploaded for user: {} -> {}", email, avatarUrl);

        return avatarUrl;
    }
}
