package com.htai.exe201phapluatso.payment.service;

import com.htai.exe201phapluatso.auth.entity.Plan;
import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.payment.entity.Payment;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Service gửi email thông báo thanh toán thành công.
 * Sử dụng @Async để không block webhook processing.
 */
@Service
public class PaymentEmailService {

    private static final Logger log = LoggerFactory.getLogger(PaymentEmailService.class);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getInstance(new Locale("vi", "VN"));

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@phapluatso.com}")
    private String fromEmail;

    @Value("${spring.mail.enabled:false}")
    private boolean emailEnabled;

    public PaymentEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Gửi email xác nhận thanh toán thành công.
     * Method này chạy async để không block webhook processing.
     * 
     * @param payment Payment entity với đầy đủ thông tin (user, plan đã được load)
     */
    @Async
    public void sendPaymentSuccessEmail(Payment payment) {
        if (payment == null) {
            log.warn("Cannot send payment email: payment is null");
            return;
        }

        User user = payment.getUser();
        Plan plan = payment.getPlan();

        if (user == null || plan == null) {
            log.warn("Cannot send payment email: user or plan is null for orderCode={}", 
                    payment.getOrderCode());
            return;
        }

        String toEmail = user.getEmail();
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Cannot send payment email: user email is empty for orderCode={}", 
                    payment.getOrderCode());
            return;
        }

        String subject = "Xác nhận thanh toán thành công - Pháp Luật Số";
        String htmlContent = buildPaymentSuccessEmailHtml(payment, user, plan);

        // Nếu email chưa được cấu hình, log ra console
        if (!emailEnabled || "your-email@gmail.com".equals(fromEmail)) {
            logEmailToConsole(toEmail, subject, payment, plan);
            return;
        }

        try {
            sendHtmlEmail(toEmail, subject, htmlContent);
            log.info("✅ Đã gửi email xác nhận thanh toán đến: {} (orderCode={})", 
                    toEmail, payment.getOrderCode());
        } catch (Exception e) {
            // Log error nhưng KHÔNG throw exception
            // Payment vẫn SUCCESS dù email gửi thất bại
            log.error("❌ Lỗi khi gửi email thanh toán đến {}: {} (orderCode={})", 
                    toEmail, e.getMessage(), payment.getOrderCode());
        }
    }

    /**
     * Gửi HTML email.
     */
    private void sendHtmlEmail(String toEmail, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true = HTML
        
        mailSender.send(message);
    }

    /**
     * Log email content ra console khi email disabled (để test).
     */
    private void logEmailToConsole(String toEmail, String subject, Payment payment, Plan plan) {
        log.info("========== EMAIL NOTIFICATION (Console Mode) ==========");
        log.info("To: {}", toEmail);
        log.info("Subject: {}", subject);
        log.info("Order Code: {}", payment.getOrderCode());
        log.info("Plan: {} ({})", plan.getName(), plan.getCode());
        log.info("Amount: {} VNĐ", CURRENCY_FORMATTER.format(payment.getAmount()));
        log.info("Chat Credits: {}", plan.getChatCredits());
        log.info("Quiz Credits: {}", plan.getQuizGenCredits());
        log.info("Paid At: {}", payment.getPaidAt() != null ? 
                payment.getPaidAt().format(DATE_FORMATTER) : "N/A");
        log.info("========================================================");
    }

    /**
     * Build HTML email template cho thanh toán thành công.
     */
    private String buildPaymentSuccessEmailHtml(Payment payment, User user, Plan plan) {
        String orderCode = String.valueOf(payment.getOrderCode());
        String planName = plan.getName();
        String amount = CURRENCY_FORMATTER.format(payment.getAmount()) + " VNĐ";
        String chatCredits = String.valueOf(plan.getChatCredits());
        String quizCredits = String.valueOf(plan.getQuizGenCredits());
        String paidAt = payment.getPaidAt() != null ? 
                payment.getPaidAt().format(DATE_FORMATTER) : "N/A";
        String userName = user.getFullName() != null ? user.getFullName() : user.getEmail();

        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Xác nhận thanh toán</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f5f5f5;">
                <table role="presentation" style="width: 100%%; max-width: 600px; margin: 0 auto; background-color: #ffffff;">
                    <!-- Header -->
                    <tr>
                        <td style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 30px; text-align: center;">
                            <h1 style="color: #ffffff; margin: 0; font-size: 24px;">Pháp Luật Số</h1>
                            <p style="color: #e0e0e0; margin: 10px 0 0 0; font-size: 14px;">Trợ lý pháp lý thông minh</p>
                        </td>
                    </tr>
                    
                    <!-- Success Icon -->
                    <tr>
                        <td style="padding: 30px 30px 20px 30px; text-align: center;">
                            <div style="width: 80px; height: 80px; background-color: #4CAF50; border-radius: 50%%; margin: 0 auto; display: flex; align-items: center; justify-content: center;">
                                <span style="color: white; font-size: 40px; line-height: 80px;">✓</span>
                            </div>
                            <h2 style="color: #333333; margin: 20px 0 10px 0; font-size: 22px;">Thanh toán thành công!</h2>
                            <p style="color: #666666; margin: 0; font-size: 14px;">Cảm ơn bạn đã tin tưởng sử dụng dịch vụ của chúng tôi</p>
                        </td>
                    </tr>
                    
                    <!-- Greeting -->
                    <tr>
                        <td style="padding: 0 30px 20px 30px;">
                            <p style="color: #333333; margin: 0; font-size: 16px;">Xin chào <strong>%s</strong>,</p>
                            <p style="color: #666666; margin: 10px 0 0 0; font-size: 14px;">
                                Giao dịch của bạn đã được xử lý thành công. Dưới đây là chi tiết đơn hàng:
                            </p>
                        </td>
                    </tr>
                    
                    <!-- Order Details -->
                    <tr>
                        <td style="padding: 0 30px 30px 30px;">
                            <table role="presentation" style="width: 100%%; background-color: #f8f9fa; border-radius: 8px; border: 1px solid #e9ecef;">
                                <tr>
                                    <td style="padding: 20px;">
                                        <table role="presentation" style="width: 100%%;">
                                            <tr>
                                                <td style="padding: 8px 0; color: #666666; font-size: 14px;">Mã đơn hàng:</td>
                                                <td style="padding: 8px 0; color: #333333; font-size: 14px; text-align: right; font-weight: bold;">#%s</td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 8px 0; color: #666666; font-size: 14px;">Gói dịch vụ:</td>
                                                <td style="padding: 8px 0; color: #333333; font-size: 14px; text-align: right; font-weight: bold;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 8px 0; color: #666666; font-size: 14px;">Số tiền:</td>
                                                <td style="padding: 8px 0; color: #4CAF50; font-size: 16px; text-align: right; font-weight: bold;">%s</td>
                                            </tr>
                                            <tr>
                                                <td colspan="2" style="padding: 15px 0 8px 0; border-top: 1px dashed #dee2e6;"></td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 8px 0; color: #666666; font-size: 14px;">Credits Chat AI:</td>
                                                <td style="padding: 8px 0; color: #667eea; font-size: 14px; text-align: right; font-weight: bold;">+%s lượt</td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 8px 0; color: #666666; font-size: 14px;">Credits Tạo Quiz:</td>
                                                <td style="padding: 8px 0; color: #764ba2; font-size: 14px; text-align: right; font-weight: bold;">+%s lượt</td>
                                            </tr>
                                            <tr>
                                                <td colspan="2" style="padding: 15px 0 8px 0; border-top: 1px dashed #dee2e6;"></td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 8px 0; color: #666666; font-size: 14px;">Thời gian thanh toán:</td>
                                                <td style="padding: 8px 0; color: #333333; font-size: 14px; text-align: right;">%s</td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    
                    <!-- CTA Button -->
                    <tr>
                        <td style="padding: 0 30px 30px 30px; text-align: center;">
                            <a href="https://phapluatso.com" style="display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: #ffffff; text-decoration: none; padding: 14px 30px; border-radius: 6px; font-size: 14px; font-weight: bold;">
                                Bắt đầu sử dụng ngay
                            </a>
                        </td>
                    </tr>
                    
                    <!-- Footer -->
                    <tr>
                        <td style="background-color: #f8f9fa; padding: 20px 30px; text-align: center; border-top: 1px solid #e9ecef;">
                            <p style="color: #999999; margin: 0 0 10px 0; font-size: 12px;">
                                Nếu bạn có bất kỳ câu hỏi nào, vui lòng liên hệ với chúng tôi.
                            </p>
                            <p style="color: #999999; margin: 0; font-size: 12px;">
                                © 2024 Pháp Luật Số. All rights reserved.
                            </p>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(userName, orderCode, planName, amount, chatCredits, quizCredits, paidAt);
    }
}
