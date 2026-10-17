package com.waymate.service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service for sending user verification emails and OTP codes.
 */
@Service
@Slf4j
public class EmailService {

	@Autowired(required = false)
	private JavaMailSender mailSender;

	/**
	 * Send 6-digit password reset OTP to user's email.
	 *
	 * @param toEmail recipient email address
	 * @param otpCode 6-digit random verification code
	 */
	public void sendPasswordResetOtp(String toEmail, String otpCode) {
		String subject = "Waymate - Password Reset Verification Code";
		String body = "Hello,\n\n"
				+ "You have requested to reset your password for your Waymate account.\n"
				+ "Your 6-digit verification code is: " + otpCode + "\n\n"
				+ "⚠️ This code will expire in 1 minute.\n"
				+ "If you did not request this, please ignore this email.\n\n"
				+ "Best regards,\n"
				+ "Waymate Team";

		log.info("=================================================================");
		log.info("📧 [WAYMATE EMAIL SERVICE] Password Reset Request");
		log.info("   Recipient : {}", toEmail);
		log.info("   OTP Code  : {} (Expires in 1 minute / 60 seconds)", otpCode);
		log.info("=================================================================");

		if (mailSender != null) {
			try {
				SimpleMailMessage message = new SimpleMailMessage();
				message.setTo(toEmail);
				message.setSubject(subject);
				message.setText(body);
				mailSender.send(message);
				log.info("Email successfully dispatched via SMTP to {}", toEmail);
			} catch (Exception e) {
				log.warn("SMTP email delivery failed ({}), but OTP was logged above for local development/testing.", e.getMessage());
			}
		} else {
			log.info("JavaMailSender not configured. Simulated email delivery to {}.", toEmail);
		}
	}
}