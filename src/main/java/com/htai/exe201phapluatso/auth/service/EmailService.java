package com.htai.exe201phapluatso.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@phapluatso.com}")
    private String fromEmail;

    @Value("${spring.mail.enabled:false}")
    private boolean emailEnabled;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Gửi OTP qua email
     */
    public void sendPasswordResetOtp(String toEmail, String otp) {
        // Nếu chưa cấu hình email, chỉ log ra console (để test)
        if (!emailEnabled || "your-email@gmail.com".equals(fromEmail)) {
            System.out.println("⚠️ Email chưa được cấu hình. OTP cho " + toEmail + " là: " + otp);
            System.out.println("📧 Nội dung email:");
            System.out.println(buildOtpEmailContent(otp));
            System.out.println("---");
            // Không throw exception để có thể test được
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Mã OTP đặt lại mật khẩu - Pháp Luật Số");
            message.setText(buildOtpEmailContent(otp));
            
            mailSender.send(message);
            System.out.println("✅ Đã gửi OTP đến email: " + toEmail);
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi gửi email: " + e.getMessage());
            throw new RuntimeException("Không thể gửi email. Vui lòng thử lại sau.", e);
        }
    }

    /**
     * Tạo nội dung email OTP
     */
    private String buildOtpEmailContent(String otp) {
        return """
                Xin chào,
                
                Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản Pháp Luật Số.
                
                Mã OTP của bạn là: %s
                
                Mã này có hiệu lực trong 15 phút.
                
                Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.
                
                Trân trọng,
                Đội ngũ Pháp Luật Số
                """.formatted(otp);
    }
}
