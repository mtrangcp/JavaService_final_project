package com.btvn.serviceprojectfinal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async("emailTaskExecutor")
    public void sendOtpEmail(String toEmail, String fullName, String otpCode,
                             long expirationMinutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[Job Portal] Mã OTP đặt lại mật khẩu");
            helper.setText(buildOtpEmailHtml(fullName, otpCode, expirationMinutes), true);

            mailSender.send(message);
            log.info("[EMAIL] Gửi OTP thành công đến: {}", toEmail);

        } catch (Exception e) {
            log.error("[EMAIL] Gửi OTP thất bại đến: {} | Lỗi: {}", toEmail, e.getMessage());
        }
    }

    private String buildOtpEmailHtml(String fullName, String otpCode,
                                     long expirationMinutes) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2 style="color: #2c3e50;">Đặt lại mật khẩu — Job Portal</h2>
                    <p>Xin chào <strong>%s</strong>,</p>
                    <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>
                    <p>Mã OTP của bạn là:</p>
                    <div style="text-align: center; margin: 24px 0;">
                        <span style="font-size: 36px; font-weight: bold; letter-spacing: 8px;
                                     color: #e74c3c; background: #fdf2f2; padding: 12px 24px;
                                     border-radius: 8px;">%s</span>
                    </div>
                    <p>Mã OTP có hiệu lực trong <strong>%d phút</strong>.</p>
                    <p style="color: #7f8c8d;">Nếu bạn không yêu cầu, hãy bỏ qua email này.</p>
                    <hr/>
                    <p style="font-size: 12px; color: #bdc3c7;">Job Portal System — No Reply</p>
                </div>
                """.formatted(fullName, otpCode, expirationMinutes);
    }
}