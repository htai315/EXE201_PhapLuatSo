package com.htai.exe201phapluatso.auth.service;

import com.htai.exe201phapluatso.auth.entity.PasswordResetOtp;
import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.repo.PasswordResetOtpRepo;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import com.htai.exe201phapluatso.common.exception.BadRequestException;
import com.htai.exe201phapluatso.common.exception.NotFoundException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class PasswordResetService {

    private final UserRepo userRepo;
    private final PasswordResetOtpRepo otpRepo;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(
            UserRepo userRepo,
            PasswordResetOtpRepo otpRepo,
            EmailService emailService,
            PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.otpRepo = otpRepo;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Bước 1: Gửi OTP đến email
     */
    @Transactional
    public void sendOtp(String email) {
        // Kiểm tra email có tồn tại không
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Email không tồn tại trong hệ thống"));

        // Kiểm tra nếu user đăng nhập bằng Google/OAuth
        if (!"LOCAL".equals(user.getProvider())) {
            throw new BadRequestException("Tài khoản này đăng nhập bằng Google. Vui lòng sử dụng nút 'Đăng nhập bằng Google' để truy cập.");
        }

        // Xóa các OTP cũ của email này
        otpRepo.deleteByEmail(email);

        // Tạo OTP 6 số
        String otp = generateOtp();

        // Lưu OTP vào database (hết hạn sau 15 phút)
        PasswordResetOtp resetOtp = new PasswordResetOtp(
                email,
                otp,
                LocalDateTime.now().plusMinutes(15)
        );
        otpRepo.save(resetOtp);

        // Gửi OTP qua email
        emailService.sendPasswordResetOtp(email, otp);
    }

    /**
     * Bước 2: Xác thực OTP và đặt lại mật khẩu
     */
    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        // Tìm OTP
        PasswordResetOtp resetOtp = otpRepo.findByEmailAndOtpAndUsedFalse(email, otp)
                .orElseThrow(() -> new BadRequestException("OTP không hợp lệ hoặc đã được sử dụng"));

        // Kiểm tra OTP có hết hạn không
        if (resetOtp.isExpired()) {
            throw new BadRequestException("OTP đã hết hạn. Vui lòng yêu cầu OTP mới.");
        }

        // Tìm user
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        // Validate mật khẩu mới
        if (newPassword == null || newPassword.length() < 6) {
            throw new BadRequestException("Mật khẩu phải có ít nhất 6 ký tự");
        }

        // Cập nhật mật khẩu mới
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        // Đánh dấu OTP đã sử dụng
        resetOtp.setUsed(true);
        otpRepo.save(resetOtp);
    }

    /**
     * Tạo OTP 6 số ngẫu nhiên
     */
    private String generateOtp() {
        int otp = 100000 + random.nextInt(900000); // 100000 - 999999
        return String.valueOf(otp);
    }

    /**
     * Tự động xóa các OTP đã hết hạn (chạy mỗi giờ)
     */
    @Scheduled(cron = "0 0 * * * *") // Chạy vào đầu mỗi giờ
    @Transactional
    public void cleanupExpiredOtps() {
        otpRepo.deleteExpiredOtps(LocalDateTime.now());
        System.out.println("🧹 Đã xóa các OTP hết hạn");
    }
}
