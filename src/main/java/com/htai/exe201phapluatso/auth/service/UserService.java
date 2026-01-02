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

    // Thư mục lưu avatar
    private static final String UPLOAD_DIR = "uploads/avatars/";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    public UserService(UserRepo userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        
        // Tạo thư mục upload nếu chưa có
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            log.error("Không thể tạo thư mục upload", e);
        }
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
            // Xóa avatar cũ nếu có
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                deleteOldAvatar(user.getAvatarUrl());
            }

            // Generate unique filename (ignore original filename for security)
            String extension = getFileExtension(file.getContentType());
            String filename = UUID.randomUUID().toString() + extension;

            // Save file
            Path filePath = Paths.get(UPLOAD_DIR + filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Update user avatar URL
            String avatarUrl = "/uploads/avatars/" + filename;
            user.setAvatarUrl(avatarUrl);
            userRepo.save(user);
            
            log.info("Avatar uploaded for user: {}", email);

            return avatarUrl;
        } catch (IOException e) {
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
        
        // Validate filename to prevent path traversal
        String filename = file.getOriginalFilename();
        if (filename != null && (filename.contains("..") || filename.contains("/") || filename.contains("\\"))) {
            throw new BadRequestException("Tên file không hợp lệ");
        }
    }
    
    /**
     * Get file extension from content type
     */
    private String getFileExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    /**
     * Xóa file avatar cũ khỏi hệ thống
     */
    private void deleteOldAvatar(String avatarUrl) {
        try {
            // Extract filename from URL (e.g., "/uploads/avatars/abc.jpg" -> "abc.jpg")
            String filename = avatarUrl.substring(avatarUrl.lastIndexOf("/") + 1);
            
            // Validate filename to prevent path traversal
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                log.warn("Invalid avatar filename: {}", filename);
                return;
            }
            
            Path filePath = Paths.get(UPLOAD_DIR + filename);
            
            // Xóa file nếu tồn tại
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.debug("Đã xóa avatar cũ: {}", filename);
            }
        } catch (IOException e) {
            log.warn("Không thể xóa avatar cũ: {}", e.getMessage());
        }
    }
}
