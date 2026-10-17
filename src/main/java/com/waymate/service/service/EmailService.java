package com.waymate.service.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Service for sending user verification emails and OTP codes via
 * JavaMailSender.
 */
@Service
@Slf4j
public class EmailService {

	@Autowired(required = false)
	private JavaMailSender mailSender;

	@Value("${spring.mail.username}")
	private String fromEmail;

	@Value("${spring.mail.password}")
	private String mailPassword;

	/**
	 * Send 6-digit password reset OTP to user's email.
	 *
	 * @param toEmail recipient email address
	 * @param otpCode 6-digit random verification code
	 */
	public void sendPasswordResetOtp(String toEmail, String otpCode) {
		String subject = "Waymate - Password Reset Verification Code";

		String plainBody = "Hello,\n\n"
				+ "You have requested to reset your password for your Waymate account.\n"
				+ "Your 6-digit verification code is: " + otpCode + "\n\n"
				+ "⚠️ This code will expire in 1 minute (60 seconds).\n"
				+ "If you did not request this, please ignore this email.\n\n"
				+ "Best regards,\n"
				+ "Waymate Team";

		String htmlBody = "<!DOCTYPE html>"
				+ "<html>"
				+ "<head><meta charset='UTF-8'></head>"
				+ "<body style='margin:0;padding:24px;background-color:#080B11;font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,sans-serif;color:#FFFFFF;'>"
				+ "<div style='max-width:480px;margin:0 auto;background-color:#0E131F;border:1px solid #1E293B;border-radius:16px;overflow:hidden;padding:28px;'>"
				+ "  <div style='text-align:center;margin-bottom:20px;'>"
				+ "    <h1 style='color:#00A3E0;font-size:22px;letter-spacing:2px;margin:0;font-weight:900;'>WAYMATE</h1>"
				+ "    <p style='color:#94A3B8;font-size:11px;margin:4px 0 0;letter-spacing:1px;font-weight:700;'>ADVENTURE PASSION HUB</p>"
				+ "  </div>"
				+ "  <h2 style='font-size:16px;color:#F8FAFC;margin:0 0 10px;'>Password Reset Request</h2>"
				+ "  <p style='color:#94A3B8;font-size:13px;line-height:1.5;margin:0 0 18px;'>"
				+ "    We received a request to reset your password. Use the verification code below to proceed:"
				+ "  </p>"
				+ "  <div style='background-color:#080B11;border:2px solid #00A3E0;border-radius:12px;padding:18px;text-align:center;margin:18px 0;'>"
				+ "    <span style='font-size:32px;font-weight:900;letter-spacing:8px;color:#00A3E0;font-family:monospace;'>"
				+ otpCode + "</span>"
				+ "  </div>"
				+ "  <p style='color:#E20613;font-size:12px;font-weight:700;margin:14px 0;text-align:center;'>"
				+ "    ⏱️ This code expires in 1 minute (60 seconds)"
				+ "  </p>"
				+ "  <p style='color:#64748B;font-size:11px;line-height:1.4;margin:18px 0 0;border-top:1px solid #1E293B;padding-top:14px;'>"
				+ "    If you did not request this code, no action is needed and your account remains safe.<br>"
				+ "    Sent from <strong>" + fromEmail + "</strong>"
				+ "  </p>"
				+ "</div>"
				+ "</body></html>";

		log.info("=================================================================");
		log.info("📧 [WAYMATE EMAIL SERVICE] Password Reset Request");
		log.info("   From      : {}", fromEmail);
		log.info("   Recipient : {}", toEmail);
		log.info("   OTP Code  : {} (Expires in 1 minute / 60 seconds)", otpCode);
		log.info("=================================================================");

		if (mailSender != null && mailPassword != null && !mailPassword.trim().isEmpty()) {
			try {
				MimeMessage mimeMessage = mailSender.createMimeMessage();
				MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
				helper.setFrom(fromEmail, "WayMate Support");
				helper.setTo(toEmail);
				helper.setSubject(subject);
				helper.setText(plainBody, htmlBody);

				mailSender.send(mimeMessage);
				log.info("✅ Email successfully dispatched via Gmail SMTP to {}", toEmail);
			} catch (Exception e) {
				log.warn("⚠️ SMTP delivery via {} failed ({}), but OTP was logged above for local testing.", fromEmail,
						e.getMessage());
			}
		} else {
			log.info(
					"ℹ️ Gmail SMTP is configured for {}, but MAIL_PASSWORD (Google App Password) is not yet set. Simulated email delivery to {}.",
					fromEmail, toEmail);
		}
	}
}