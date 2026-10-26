package com.waymate.service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health and status check controller for container orchestrators and clients.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Health & Status", description = "Endpoints for service health checks and readiness verification")
public class HealthController {

	@GetMapping("/health")
	@Operation(summary = "Health check", description = "Returns UP status if waymate-service is running.")
	public ResponseEntity<Map<String, Object>> health() {
		return ResponseEntity.ok(Map.of(
			"status", "UP",
			"service", "waymate-service",
			"timestamp", System.currentTimeMillis()
		));
	}

	@GetMapping("/hello")
	@Operation(summary = "Hello check", description = "Simple handshake endpoint for client verification.")
	public ResponseEntity<Map<String, String>> hello() {
		return ResponseEntity.ok(Map.of(
			"status", "ok",
			"message", "Hello from WayMate API Service!"
		));
	}
}
