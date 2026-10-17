package com.waymate.service.controller;

import com.waymate.service.payload.request.ForgotPasswordRequest;
import com.waymate.service.payload.request.GoogleLoginRequest;
import com.waymate.service.payload.request.ResetPasswordRequest;
import com.waymate.service.payload.request.SigninRequest;
import com.waymate.service.payload.request.SignupRequest;
import com.waymate.service.payload.request.UpdatePasswordRequest;
import com.waymate.service.payload.response.ApiResponse;
import com.waymate.service.payload.response.AuthResponse;
import com.waymate.service.payload.response.UserResponse;
import com.waymate.service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for User operations, Authentication, and Account Management.
 */
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name = "User & Authentication", description = "Endpoints for Signup, Signin, Google OAuth, Password update & recovery")
public class UserController {

	private final UserService userService;

	/**
	 * Traditional Signup - creates a new User node in the Neo4j database.
	 */
	@PostMapping("/signup")
	@Operation(summary = "Traditional Signup", description = "Registers a new user and persists their account in Neo4j.")
	public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest request) {
		AuthResponse response = userService.signup(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * Traditional Signin - validates email and BCrypt password against Neo4j DB.
	 */
	@PostMapping("/signin")
	@Operation(summary = "Traditional Signin", description = "Authenticates email and password against Neo4j.")
	public ResponseEntity<AuthResponse> signin(@RequestBody SigninRequest request) {
		AuthResponse response = userService.signin(request);
		return ResponseEntity.ok(response);
	}

	/**
	 * Google OAuth Login.
	 * If the Google email already exists in the DB, uses the existing account.
	 * Does not create duplicate nodes.
	 */
	@PostMapping("/google-login")
	@Operation(summary = "Google Login", description = "Authenticates with Google account. If email exists in DB, uses existing account; otherwise registers new account.")
	public ResponseEntity<AuthResponse> googleLogin(@RequestBody GoogleLoginRequest request) {
		AuthResponse response = userService.googleLogin(request);
		return ResponseEntity.ok(response);
	}

	/**
	 * Update User Password.
	 */
	@PostMapping("/update-password")
	@Operation(summary = "Update Password", description = "Updates password after verifying the old password.")
	public ResponseEntity<ApiResponse> updatePassword(@RequestBody UpdatePasswordRequest request) {
		ApiResponse response = userService.updatePassword(request);
		return ResponseEntity.ok(response);
	}

	/**
	 * Forgot Password - sends a 6-digit verification code expiring in 1 minute via email.
	 */
	@PostMapping("/forgot-password")
	@Operation(summary = "Forgot Password (Send OTP)", description = "Generates and sends a 6-digit OTP code expiring in 1 minute to the user's email.")
	public ResponseEntity<ApiResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
		ApiResponse response = userService.sendForgotPasswordOtp(request);
		return ResponseEntity.ok(response);
	}

	/**
	 * Reset Password - verifies 6-digit OTP code (within 1 min) and resets password.
	 */
	@PostMapping("/reset-password")
	@Operation(summary = "Reset Password with OTP", description = "Verifies the 6-digit OTP before 1-minute expiration and updates password.")
	public ResponseEntity<ApiResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
		ApiResponse response = userService.resetPassword(request);
		return ResponseEntity.ok(response);
	}

	/**
	 * Get User profile by ID.
	 */
	@GetMapping("/{id}")
	@Operation(summary = "Get User by ID", description = "Fetches user profile by node ID.")
	public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
		UserResponse response = userService.getUserById(id);
		return ResponseEntity.ok(response);
	}

	/**
	 * Get User profile by Email.
	 */
	@GetMapping("/email/{email}")
	@Operation(summary = "Get User by Email", description = "Fetches user profile by email address.")
	public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
		UserResponse response = userService.getUserByEmail(email);
		return ResponseEntity.ok(response);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse> handleIllegalArgument(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body(
			ApiResponse.builder().success(false).message(ex.getMessage()).build()
		);
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiResponse> handleIllegalState(IllegalStateException ex) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
			ApiResponse.builder().success(false).message(ex.getMessage()).build()
		);
	}
}