package com.htai.exe201phapluatso.auth.service;

import com.htai.exe201phapluatso.auth.entity.EmailVerificationToken;
import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.repo.EmailVerificationTokenRepo;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import com.htai.exe201phapluatso.common.exception.BadRequestException;
import com.htai.exe201phapluatso.common.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final int TOKEN_EXPIRY_HOURS = 24;

    private final EmailVerificationTokenRepo tokenRepo;
    private final UserRepo userRepo;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@phapluatso.com}")
    private String fromEmail;

    @Value("${spring.mail.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.frontend.base-url:http://localhost:8080}")
    private String frontendBaseUrl;

    public EmailVerificationService(
            EmailVerificationTokenRepo tokenRepo,
            UserRepo userRepo,
            JavaMailSender mailSender) {
        this.tokenRepo = tokenRepo;
        this.userRepo = userRepo;
        this.mailSender = mailSender;
    }

    /**
     * Tạo token và gửi email verification
     */
    @Transactional
    public void createAndSendVerificationToken(User user) {
        // Xóa token cũ nếu có
        tokenRepo.deleteByUser(user);

        // Tạo token mới
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS);

        EmailVerificationToken verificationToken = new EmailVerificationToken(token, user, expiresAt);
        tokenRepo.save(verificationToken);

        // Gửi email
        sendVerificationEmail(user.getEmail(), user.getFullName(), token);

        log.info("Verification email sent to: {}", user.getEmail());
    }

    /**
     * Xác thực email bằng token
     */
    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenRepo.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Link xác thực không hợp lệ"));

        if (verificationToken.isVerified()) {
            throw new BadRequestException("Email đã được xác thực trước đó");
        }

        if (verificationToken.isExpired()) {
            throw new BadRequestException("Link xác thực đã hết hạn. Vui lòng yêu cầu gửi lại email.");
        }

        // Cập nhật token
        verificationToken.setVerifiedAt(LocalDateTime.now());
        tokenRepo.save(verificationToken);

        // Cập nhật user
        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepo.save(user);

        log.info("Email verified successfully: {}", user.getEmail());
    }

    /**
     * Gửi lại email verification
     */
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepo.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new NotFoundException("Email không tồn tại trong hệ thống"));

        if (user.isEmailVerified()) {
            throw new BadRequestException("Email đã được xác thực");
        }

        if (!"LOCAL".equals(user.getProvider())) {
            throw new BadRequestException("Tài khoản đăng nhập bằng " + user.getProvider() + " không cần xác thực email");
        }

        createAndSendVerificationToken(user);
    }

    /**
     * Gửi email verification
     */
    private void sendVerificationEmail(String toEmail, String fullName, String token) {
        String verifyUrl = frontendBaseUrl + "/html/verify-email.html?token=" + token;
        String emailContent = buildVerificationEmailHtml(fullName, verifyUrl);

        if (!emailEnabled || "your-email@gmail.com".equals(fromEmail)) {
            log.warn("⚠️ Email chưa được cấu hình. Verification link cho {}: {}", toEmail, verifyUrl);
            log.info("📧 Nội dung email:\n{}", emailContent);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Xác thực email - Pháp Luật Số");
            helper.setText(emailContent, true);

            mailSender.send(message);
            log.info("✅ Đã gửi email xác thực đến: {}", toEmail);
        } catch (MessagingException e) {
            log.error("❌ Lỗi khi gửi email: {}", e.getMessage());
            throw new RuntimeException("Không thể gửi email. Vui lòng thử lại sau.", e);
        }
    }

    /**
     * Tạo nội dung email HTML
     */
    private String buildVerificationEmailHtml(String fullName, String verifyUrl) {
        String displayName = (fullName != null && !fullName.isBlank()) ? fullName : "bạn";
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f4;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
                        <h1 style="color: white; margin: 0; font-size: 28px;">⚖️ Pháp Luật Số</h1>
                        <p style="color: rgba(255,255,255,0.9); margin: 10px 0 0 0;">AI Hỗ trợ Pháp lý</p>
                    </div>
                    
                    <div style="background: white; padding: 40px 30px; border-radius: 0 0 10px 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                        <h2 style="color: #333; margin-top: 0;">Xin chào %s,</h2>
                        
                        <p style="color: #666; line-height: 1.6;">
                            Cảm ơn bạn đã đăng ký tài khoản tại <strong>Pháp Luật Số</strong>!
                        </p>
                        
                        <p style="color: #666; line-height: 1.6;">
                            Vui lòng click vào nút bên dưới để xác thực email của bạn:
                        </p>
                        
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s" style="display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; text-decoration: none; padding: 15px 40px; border-radius: 25px; font-weight: bold; font-size: 16px;">
                                ✉️ Xác thực Email
                            </a>
                        </div>
                        
                        <p style="color: #999; font-size: 14px; line-height: 1.6;">
                            Hoặc copy link sau vào trình duyệt:<br>
                            <a href="%s" style="color: #667eea; word-break: break-all;">%s</a>
                        </p>
                        
                        <div style="background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; border-radius: 4px;">
                            <p style="color: #856404; margin: 0; font-size: 14px;">
                                ⏰ <strong>Lưu ý:</strong> Link có hiệu lực trong 24 giờ.
                            </p>
                        </div>
                        
                        <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                        
                        <p style="color: #999; font-size: 12px; text-align: center;">
                            Nếu bạn không đăng ký tài khoản này, vui lòng bỏ qua email này.
                        </p>
                    </div>
                    
                    <div style="text-align: center; padding: 20px; color: #999; font-size: 12px;">
                        <p>© 2024 Pháp Luật Số. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(displayName, verifyUrl, verifyUrl, verifyUrl);
    }

    /**
     * Tự động xóa các token đã hết hạn (chạy lúc 2 AM mỗi ngày)
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepo.deleteExpiredTokens(LocalDateTime.now());
        log.info("🧹 Đã xóa các email verification token hết hạn");
    }
}
