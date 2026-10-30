package com.waymate.service.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * Global exception handler providing clean REST responses for rate limit and general errors.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(RateLimitException.class)
	public ResponseEntity<Map<String, Object>> handleRateLimitException(RateLimitException ex) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));

		Map<String, Object> body = Map.of(
			"status", HttpStatus.TOO_MANY_REQUESTS.value(),
			"error", "Too Many Requests",
			"message", ex.getMessage(),
			"retryAfterSeconds", ex.getRetryAfterSeconds(),
			"timestamp", Instant.now().toString()
		);

		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
			.headers(headers)
			.body(body);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
			"status", HttpStatus.BAD_REQUEST.value(),
			"error", "Bad Request",
			"message", ex.getMessage(),
			"timestamp", Instant.now().toString()
		));
	}
}
