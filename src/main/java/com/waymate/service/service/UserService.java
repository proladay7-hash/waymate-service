package com.waymate.service.service;

import com.waymate.service.constant.Role;
import com.waymate.service.constant.UserStatus;
import com.waymate.service.model.User;
import com.waymate.service.payload.request.ForgotPasswordRequest;
import com.waymate.service.payload.request.GoogleLoginRequest;
import com.waymate.service.payload.request.ResetPasswordRequest;
import com.waymate.service.payload.request.SigninRequest;
import com.waymate.service.payload.request.SignupRequest;
import com.waymate.service.payload.request.UpdatePasswordRequest;
import com.waymate.service.payload.request.VerifyOtpRequest;
import com.waymate.service.payload.response.ApiResponse;
import com.waymate.service.payload.response.AuthResponse;
import com.waymate.service.payload.response.UserResponse;
import com.waymate.service.repository.UserRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Core business service for User account operations and authentication in Waymate.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final EmailService emailService;
	private final JwtService jwtService;

	@Value("${waymate.oauth2.google.client-id}")
	private String googleClientId;

	@Value("${waymate.oauth2.google.client-secret}")
	private String googleClientSecret;

	private final RestClient restClient = RestClient.create();

	// In-memory thread-safe store for 6-digit OTP verification codes (1-minute expiration)
	private final Map<String, OtpEntry> otpCache = new ConcurrentHashMap<>();

	private record OtpEntry(String code, Instant expiresAt) {
		public boolean isExpired() {
			return Instant.now().isAfter(expiresAt);
		}
	}

	/**
	 * Traditional User Signup.
	 * Registers a new user node in the Neo4j database and issues a signed JWT token valid for 1 day.
	 */
	public AuthResponse signup(SignupRequest request) {
		String normalizedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
		if (normalizedEmail.isEmpty()) {
			throw new IllegalArgumentException("Email address is required");
		}
		if (userRepository.existsByEmail(normalizedEmail)) {
			throw new IllegalArgumentException("An account with this email already exists");
		}

		User user = User.builder()
			.name(request.getName() != null && !request.getName().isBlank() ? request.getName().trim() : "Rider")
			.email(normalizedEmail)
			.password(passwordEncoder.encode(request.getPassword()))
			.avatar(request.getAvatar())
			.phoneNumber(request.getPhoneNumber())
			.bio(request.getBio())
			.dateOfBirth(request.getDateOfBirth())
			.role(Role.USER.name())
			.status(UserStatus.ACTIVE.name())
			.point(0L)
			.lastLogin(Instant.now())
			.build();

		User savedUser = userRepository.save(user);
		String token = jwtService.generateToken(savedUser);
		log.info("Registered new user node in Neo4j with ID: {} and email: {}. JWT issued.", savedUser.getId(), savedUser.getEmail());

		return AuthResponse.builder()
			.success(true)
			.message("Registration successful")
			.user(UserResponse.fromUser(savedUser))
			.token(token)
			.build();
	}

	/**
	 * Traditional User Signin.
	 * Authenticates email and BCrypt password against Neo4j DB and issues a 1-day JWT token.
	 */
	public AuthResponse signin(SigninRequest request) {
		String normalizedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
		User user = userRepository.findByEmail(normalizedEmail)
			.orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

		if (Boolean.TRUE.equals(user.getIsDeleted())) {
			throw new IllegalStateException("Account has been deactivated. Please contact support.");
		}
		if (UserStatus.BANNED.name().equalsIgnoreCase(user.getStatus())) {
			throw new IllegalStateException("Account is suspended.");
		}
		if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			throw new IllegalArgumentException("Invalid email or password");
		}

		user.setLastLogin(Instant.now());
		User updatedUser = userRepository.save(user);
		String token = jwtService.generateToken(updatedUser);
		log.info("User logged in successfully: {}. JWT issued with 1-day expiration.", updatedUser.getEmail());

		return AuthResponse.builder()
			.success(true)
			.message("Login successful")
			.user(UserResponse.fromUser(updatedUser))
			.token(token)
			.build();
	}

	/**
	 * Google OAuth Login.
	 * If code is provided, exchanges code with Google token endpoint using client_secret.
	 * If idToken or accessToken is provided, verifies it with Google.
	 * If the email exists in Neo4j DB, uses the existing account.
	 * Returns a 1-day JWT session token.
	 */
	public AuthResponse googleLogin(GoogleLoginRequest request) {
		boolean verified = false;

		// 1. If Google authorization code is provided, exchange it for tokens using client_id and client_secret
		if (request.getCode() != null && !request.getCode().isBlank()) {
			try {
				var typeRef = new ParameterizedTypeReference<Map<String, Object>>() {};
				MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
				form.add("code", request.getCode().trim());
				form.add("client_id", googleClientId);
				form.add("client_secret", googleClientSecret);
				form.add("redirect_uri", request.getRedirectUri() != null ? request.getRedirectUri() : "");
				form.add("grant_type", "authorization_code");

				Map<String, Object> tokenRes = restClient.post()
					.uri("https://oauth2.googleapis.com/token")
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(form)
					.retrieve()
					.body(typeRef);

				if (tokenRes != null) {
					if (tokenRes.get("id_token") != null) {
						request.setIdToken((String) tokenRes.get("id_token"));
					}
					if (tokenRes.get("access_token") != null) {
						request.setAccessToken((String) tokenRes.get("access_token"));
					}
					log.info("Successfully exchanged Google authorization code for tokens");
				}
			} catch (Exception e) {
				log.warn("Could not exchange Google authorization code with Google token endpoint: {}", e.getMessage());
			}
		}

		// 2. Verify with Google Tokeninfo API if idToken is supplied
		if (request.getIdToken() != null && !request.getIdToken().isBlank()) {
			try {
				var typeRef = new ParameterizedTypeReference<Map<String, Object>>() {};
				Map<String, Object> tokenInfo = restClient.get()
					.uri("https://oauth2.googleapis.com/tokeninfo?id_token={idToken}", request.getIdToken().trim())
					.retrieve()
					.body(typeRef);

				if (tokenInfo != null) {
					String verifiedEmail = (String) tokenInfo.get("email");
					if (verifiedEmail != null && !verifiedEmail.isBlank()) {
						request.setEmail(verifiedEmail);
						verified = true;
					}
					if (tokenInfo.get("name") != null) {
						request.setName((String) tokenInfo.get("name"));
					}
					if (tokenInfo.get("picture") != null) {
						request.setAvatar((String) tokenInfo.get("picture"));
					}
					if (tokenInfo.get("sub") != null) {
						request.setGoogleId((String) tokenInfo.get("sub"));
					}
					log.info("Verified Google ID Token successfully for email: {}", request.getEmail());
				}
			} catch (Exception e) {
				log.warn("Could not verify Google ID token with Google tokeninfo: {}", e.getMessage());
			}
		}

		// 3. Verify with Google UserInfo endpoint if accessToken is supplied and idToken was not verified
		if (!verified && request.getAccessToken() != null && !request.getAccessToken().isBlank()) {
			try {
				var typeRef = new ParameterizedTypeReference<Map<String, Object>>() {};
				Map<String, Object> userInfo = restClient.get()
					.uri("https://www.googleapis.com/oauth2/v3/userinfo")
					.header("Authorization", "Bearer " + request.getAccessToken().trim())
					.retrieve()
					.body(typeRef);

				if (userInfo != null) {
					String verifiedEmail = (String) userInfo.get("email");
					if (verifiedEmail != null && !verifiedEmail.isBlank()) {
						request.setEmail(verifiedEmail);
						verified = true;
					}
					if (userInfo.get("name") != null) {
						request.setName((String) userInfo.get("name"));
					}
					if (userInfo.get("picture") != null) {
						request.setAvatar((String) userInfo.get("picture"));
					}
					if (userInfo.get("sub") != null) {
						request.setGoogleId((String) userInfo.get("sub"));
					}
					log.info("Verified Google Access Token successfully for email: {}", request.getEmail());
				}
			} catch (Exception e) {
				log.warn("Could not fetch userinfo with Google access token: {}", e.getMessage());
			}
		}

		if (!verified) {
			log.error("Google login rejected: No valid Google verification token provided or token verification failed for email: {}", request.getEmail());
			throw new IllegalArgumentException("Google authentication failed: Email could not be verified by Google. A valid Google account token is required.");
		}

		String normalizedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
		if (normalizedEmail.isEmpty()) {
			throw new IllegalArgumentException("Google account email is required");
		}

		var existingUserOpt = userRepository.findByEmail(normalizedEmail);
		User user;

		if (existingUserOpt.isPresent()) {
			// USE EXISTING ACCOUNT IN DB - DO NOT CREATE NEW
			user = existingUserOpt.get();
			user.setLastLogin(Instant.now());

			// Sync avatar if existing user does not have one
			if ((user.getAvatar() == null || user.getAvatar().isBlank()) && request.getAvatar() != null) {
				user.setAvatar(request.getAvatar());
			}
			user = userRepository.save(user);
			log.info("Google login matched existing user in Neo4j DB: {}", user.getEmail());
		} else {
			// Create new user for first-time Google signin
			String fallbackName = request.getName() != null && !request.getName().isBlank()
				? request.getName().trim()
				: normalizedEmail.split("@")[0];

			user = User.builder()
				.name(fallbackName)
				.email(normalizedEmail)
				.avatar(request.getAvatar())
				.role(Role.USER.name())
				.status(UserStatus.ACTIVE.name())
				.point(0L)
				.lastLogin(Instant.now())
				.build();

			user = userRepository.save(user);
			log.info("Created new user node via Google login in Neo4j DB: {}", user.getEmail());
		}

		String token = jwtService.generateToken(user);

		return AuthResponse.builder()
			.success(true)
			.message("Google authentication successful")
			.user(UserResponse.fromUser(user))
			.token(token)
			.build();
	}

	/**
	 * Extracts user information from Bearer JWT token.
	 */
	public UserResponse getCurrentUserFromToken(String authHeader) {
		if (authHeader == null || authHeader.isBlank()) {
			throw new IllegalArgumentException("Authorization header is required");
		}
		String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7).trim() : authHeader.trim();
		if (!jwtService.validateToken(token)) {
			throw new IllegalStateException("Session token is invalid or has expired (1 day session). Please log in again.");
		}
		String email = jwtService.extractEmail(token);
		return getUserByEmail(email);
	}

	/**
	 * Update User Password.
	 * Validates current password and updates with new BCrypt hash.
	 */
	public ApiResponse updatePassword(UpdatePasswordRequest request) {
		String normalizedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
		User user = userRepository.findByEmail(normalizedEmail)
			.orElseThrow(() -> new IllegalArgumentException("User not found with email: " + normalizedEmail));

		if (user.getPassword() == null || !passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
			throw new IllegalArgumentException("Incorrect current password");
		}
		if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
			throw new IllegalArgumentException("New password must be at least 6 characters long");
		}

		user.setPassword(passwordEncoder.encode(request.getNewPassword()));
		user.setUpdatedAt(Instant.now());
		userRepository.save(user);
		log.info("Password updated successfully for user: {}", user.getEmail());

		return ApiResponse.builder()
			.success(true)
			.message("Password updated successfully")
			.build();
	}

	/**
	 * Forgot Password - Generates and sends a 6-digit random code expiring in 1 minute via email.
	 */
	public ApiResponse sendForgotPasswordOtp(ForgotPasswordRequest request) {
		String normalizedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
		if (!userRepository.existsByEmail(normalizedEmail)) {
			throw new IllegalArgumentException("No registered account found with email: " + normalizedEmail);
		}

		// Generate 6-digit random code (100000 - 999999)
		String code = String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 1000000));
		// Set expiration strictly to 1 minute (60 seconds)
		Instant expiresAt = Instant.now().plusSeconds(60);

		otpCache.put(normalizedEmail, new OtpEntry(code, expiresAt));

		// Send email via EmailService asynchronously to provide instantaneous HTTP response
		CompletableFuture.runAsync(() -> {
			try {
				emailService.sendPasswordResetOtp(normalizedEmail, code);
			} catch (Exception e) {
				log.error("Failed to dispatch password reset OTP email to {}: {}", normalizedEmail, e.getMessage());
			}
		});

		return ApiResponse.builder()
			.success(true)
			.message("A 6-digit verification code has been sent to your email. The code expires in 1 minute.")
			.build();
	}

	/**
	 * Verify 6-digit OTP code before 1-minute expiration.
	 */
	public ApiResponse verifyOtp(VerifyOtpRequest request) {
		String normalizedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
		String code = request.getCode() != null ? request.getCode().trim() : "";

		OtpEntry entry = otpCache.get(normalizedEmail);
		if (entry == null) {
			throw new IllegalArgumentException("No verification code requested for this email, or code was already used. Please request a new code.");
		}

		if (entry.isExpired()) {
			otpCache.remove(normalizedEmail);
			throw new IllegalArgumentException("Verification code has expired (valid for 1 minute). Please request a new code.");
		}

		if (!entry.code().equals(code)) {
			throw new IllegalArgumentException("Invalid 6-digit verification code. Please check your email and try again.");
		}

		log.info("OTP verified successfully for email: {}", normalizedEmail);

		return ApiResponse.builder()
			.success(true)
			.message("Verification code verified successfully.")
			.build();
	}

	/**
	 * Reset Password using the 6-digit OTP code before it expires in 1 minute.
	 */
	public ApiResponse resetPassword(ResetPasswordRequest request) {
		String normalizedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
		OtpEntry entry = otpCache.get(normalizedEmail);

		if (entry == null) {
			throw new IllegalArgumentException("No verification code requested for this email, or code was already used.");
		}

		if (entry.isExpired()) {
			otpCache.remove(normalizedEmail);
			throw new IllegalArgumentException("Verification code has expired (valid for 1 minute). Please request a new code.");
		}

		if (!entry.code().equals(request.getCode().trim())) {
			throw new IllegalArgumentException("Invalid 6-digit verification code.");
		}

		if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
			throw new IllegalArgumentException("New password must be at least 6 characters long.");
		}

		// Code is valid! Invalidate the OTP to prevent replay attacks
		otpCache.remove(normalizedEmail);

		User user = userRepository.findByEmail(normalizedEmail)
			.orElseThrow(() -> new IllegalArgumentException("User not found with email: " + normalizedEmail));

		user.setPassword(passwordEncoder.encode(request.getNewPassword()));
		user.setUpdatedAt(Instant.now());
		userRepository.save(user);

		log.info("Password reset successfully using OTP for user: {}", user.getEmail());

		return ApiResponse.builder()
			.success(true)
			.message("Password has been reset successfully. You can now log in with your new password.")
			.build();
	}

	/**
	 * Retrieve User profile by ID.
	 */
	public UserResponse getUserById(UUID id) {
		User user = userRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
		return UserResponse.fromUser(user);
	}

	/**
	 * Retrieve User profile by Email.
	 */
	public UserResponse getUserByEmail(String email) {
		String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
		User user = userRepository.findByEmail(normalizedEmail)
			.orElseThrow(() -> new IllegalArgumentException("User not found with email: " + normalizedEmail));
		return UserResponse.fromUser(user);
	}
}