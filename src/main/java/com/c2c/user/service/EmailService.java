package com.c2c.user.service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 邮件发送服务
 * <p>基于 JavaMailSender 异步发送验证码邮件（HTML 模板）。
 * 未配置 spring.mail.username 时记录错误日志并安全跳过，避免影响主流程。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    /** 异步发送验证码邮件到指定邮箱 */
    public void sendVerificationCode(String to, String code) {
        CompletableFuture.runAsync(() -> {
            try {
                if (from == null || from.trim().isEmpty()) {
                    log.error("cannot send verification email to {}: spring.mail.username is not configured "
                            + "(set MAIL_USERNAME / spring.mail.username to a real QQ mailbox)", to);
                    return;
                }
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(from);
                helper.setTo(to);
                helper.setSubject("C2C 验证码");

                String html = "<div style=\"max-width:480px;margin:0 auto;padding:32px;"
                        + "background:#fff;border-radius:16px;font-family:Arial,'PingFang SC','Microsoft YaHei',sans-serif\">"
                        + "<h1 style=\"font-size:22px;color:#2d3436;margin:0 0 16px\">C2C 验证码</h1>"
                        + "<p style=\"font-size:14px;color:#636e72\">您的验证码为：</p>"
                        + "<div style=\"font-size:36px;font-weight:800;color:#ff6b35;letter-spacing:6px\">"
                        + code
                        + "</div>"
                        + "<p style=\"font-size:12px;color:#999;margin-top:20px\">验证码 5 分钟内有效，请勿泄露给他人。</p>"
                        + "</div>";

                helper.setText(html, true);
                mailSender.send(message);
                log.info("verification email sent: to={}, code={}", to, code);
            } catch (MessagingException e) {
                log.error("failed to send verification email: to={}", to, e);
            }
        });
    }
}
