package com.waymate.service.controller;

import com.waymate.service.payload.request.SosRequest;
import com.waymate.service.payload.response.SosResponse;
import com.waymate.service.service.SosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for emergency SOS roadside assistance alerts.
 */
@RestController
@RequestMapping("/api/v1/sos")
@RequiredArgsConstructor
@Tag(name = "Emergency SOS", description = "Endpoints for broadcasting and managing emergency SOS alerts to nearby riders within 10km")
public class SosController {

	private final SosService sosService;

	/**
	 * Send an emergency SOS alert.
	 * Persists in SOS table and notifies all users within 10km radius.
	 */
	@PostMapping
	@Operation(summary = "Send SOS alert", description = "Dispatches emergency request, persists in SOS table and notifies nearby riders within 10km.")
	public ResponseEntity<SosResponse> createSos(@RequestBody SosRequest request) {
		SosResponse response = sosService.createSos(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * Get all active SOS requests, optionally filtered/sorted by distance to caller GPS.
	 */
	@GetMapping({"", "/active"})
	@Operation(summary = "Get active SOS alerts", description = "Returns active SOS alerts in the community with relative distance.")
	public ResponseEntity<List<SosResponse>> getActiveSos(
		@RequestParam(required = false) Double lat,
		@RequestParam(required = false) Double lng
	) {
		return ResponseEntity.ok(sosService.getActiveSos(lat, lng));
	}

	/**
	 * Get SOS details by ID.
	 */
	@GetMapping("/{id}")
	@Operation(summary = "Get SOS details by ID", description = "Retrieves full emergency information for an SOS request.")
	public ResponseEntity<SosResponse> getSosById(@PathVariable UUID id) {
		return ResponseEntity.ok(sosService.getSosById(id));
	}

	/**
	 * Update SOS status (RESOLVED, HELPING, CANCELLED).
	 */
	@RequestMapping(value = "/{id}/status", method = {RequestMethod.POST, RequestMethod.PUT})
	@Operation(summary = "Update SOS status", description = "Updates status of an emergency SOS request.")
	public ResponseEntity<SosResponse> updateStatus(
		@PathVariable UUID id,
		@RequestParam String status
	) {
		return ResponseEntity.ok(sosService.updateStatus(id, status));
	}
}
