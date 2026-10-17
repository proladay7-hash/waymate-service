package com.waymate.service.service;

import com.waymate.service.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for generating and validating JSON Web Tokens (JWT).
 * Configured with 1-day (24 hours) session expiration.
 */
@Service
@Slf4j
public class JwtService {

	@Value("${waymate.jwt.secret:WaymateSecretKeyForJwtAuthenticationAdventureRideHub2026!}")
	private String secretKeyString;

	// 1 day expiration in milliseconds: 24 * 60 * 60 * 1000 = 86,400,000 ms
	@Value("${waymate.jwt.expiration-ms:86400000}")
	private long jwtExpirationMs;

	private SecretKey getSigningKey() {
		byte[] keyBytes = secretKeyString.getBytes(StandardCharsets.UTF_8);
		return Keys.hmacShaKeyFor(keyBytes);
	}

	/**
	 * Generates a signed JWT token for the user with 1-day expiration.
	 */
	public String generateToken(User user) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

		return Jwts.builder()
			.subject(user.getEmail())
			.claim("id", user.getId())
			.claim("name", user.getName())
			.claim("role", user.getRole())
			.claim("avatar", user.getAvatar())
			.issuedAt(now)
			.expiration(expiryDate)
			.signWith(getSigningKey())
			.compact();
	}

	/**
	 * Extracts email (subject) from JWT token.
	 */
	public String extractEmail(String token) {
		return extractAllClaims(token).getSubject();
	}

	/**
	 * Extracts user ID from JWT token.
	 */
	public String extractUserId(String token) {
		Claims claims = extractAllClaims(token);
		return claims.get("id", String.class);
	}

	/**
	 * Validates JWT token signature and expiration.
	 */
	public boolean validateToken(String token) {
		try {
			Claims claims = extractAllClaims(token);
			return !claims.getExpiration().before(new Date());
		} catch (JwtException | IllegalArgumentException e) {
			log.warn("Invalid or expired JWT token: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Extracts all claims from a signed JWT token.
	 */
	public Claims extractAllClaims(String token) {
		return Jwts.parser()
			.verifyWith(getSigningKey())
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}
}