package com.htai.exe201phapluatso.auth.service;

import com.htai.exe201phapluatso.auth.dto.ChangePasswordRequest;
import com.htai.exe201phapluatso.auth.dto.UserProfileResponse;
import com.htai.exe201phapluatso.auth.entity.Role;
import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import com.htai.exe201phapluatso.common.exception.BadRequestException;
import com.htai.exe201phapluatso.common.exception.NotFoundException;
import com.htai.exe201phapluatso.common.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final com.htai.exe201phapluatso.common.service.CloudinaryService cloudinaryService;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    public UserService(UserRepo userRepo, PasswordEncoder passwordEncoder, 
                       com.htai.exe201phapluatso.common.service.CloudinaryService cloudinaryService) {
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

        // Validate file
        validateAvatarFile(file);

        try {
            // Xóa avatar cũ trên Cloudinary nếu có
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                deleteOldAvatar(user.getAvatarUrl());
            }

            // Upload lên Cloudinary
            String avatarUrl = cloudinaryService.uploadFile(file, "avatars");

            // Update user avatar URL
            user.setAvatarUrl(avatarUrl);
            userRepo.save(user);
            
            log.info("Avatar uploaded to Cloudinary for user: {}", email);

            return avatarUrl;
        } catch (Exception e) {
            log.error("Không thể lưu file avatar", e);
            throw new BadRequestException("Không thể lưu file. Vui lòng thử lại.");
        }
    }
    
    /**
     * Validate avatar file
     */
    private void validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File không được để trống");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("Kích thước file không được vượt quá 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("File phải là ảnh (JPG, PNG, GIF, WEBP)");
        }
    }

    /**
     * Xóa file avatar cũ khỏi Cloudinary
     */
    private void deleteOldAvatar(String avatarUrl) {
        try {
            cloudinaryService.deleteFile(avatarUrl);
        } catch (Exception e) {
            log.warn("Không thể xóa avatar cũ trên Cloudinary: {}", e.getMessage());
        }
    }
}
