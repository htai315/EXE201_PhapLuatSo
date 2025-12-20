package com.htai.exe201phapluatso.auth.service;

import com.htai.exe201phapluatso.auth.dto.ChangePasswordRequest;
import com.htai.exe201phapluatso.auth.dto.UserProfileResponse;
import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import com.htai.exe201phapluatso.common.exception.BadRequestException;
import com.htai.exe201phapluatso.common.exception.NotFoundException;
import com.htai.exe201phapluatso.common.exception.UnauthorizedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    // Thư mục lưu avatar (có thể config trong application.properties)
    private static final String UPLOAD_DIR = "uploads/avatars/";

    public UserService(UserRepo userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        
        // Tạo thư mục upload nếu chưa có
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo thư mục upload", e);
        }
    }

    public UserProfileResponse getUserProfile(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        String role = user.getRoles().isEmpty() ? "USER" : 
                      user.getRoles().iterator().next().getName();

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
    }

    @Transactional
    public String uploadAvatar(String email, MultipartFile file) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        // Validate file
        if (file.isEmpty()) {
            throw new BadRequestException("File không được để trống");
        }

        // Validate file size (5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BadRequestException("Kích thước file không được vượt quá 5MB");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("File phải là ảnh (JPG, PNG, GIF)");
        }

        try {
            // Xóa avatar cũ nếu có
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                deleteOldAvatar(user.getAvatarUrl());
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
            String filename = UUID.randomUUID().toString() + extension;

            // Save file
            Path filePath = Paths.get(UPLOAD_DIR + filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Update user avatar URL
            String avatarUrl = "/uploads/avatars/" + filename;
            user.setAvatarUrl(avatarUrl);
            userRepo.save(user);

            return avatarUrl;
        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu file", e);
        }
    }

    /**
     * Xóa file avatar cũ khỏi hệ thống
     */
    private void deleteOldAvatar(String avatarUrl) {
        try {
            // Extract filename from URL (e.g., "/uploads/avatars/abc.jpg" -> "abc.jpg")
            String filename = avatarUrl.substring(avatarUrl.lastIndexOf("/") + 1);
            Path filePath = Paths.get(UPLOAD_DIR + filename);
            
            // Xóa file nếu tồn tại
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                System.out.println("Đã xóa avatar cũ: " + filename);
            }
        } catch (IOException e) {
            // Log error nhưng không throw exception để không ảnh hưởng đến việc upload avatar mới
            System.err.println("Không thể xóa avatar cũ: " + e.getMessage());
        }
    }
}
