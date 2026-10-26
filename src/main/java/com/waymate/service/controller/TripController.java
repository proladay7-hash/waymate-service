package com.waymate.service.controller;

import com.waymate.service.model.Trip;
import com.waymate.service.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for curated adventure trips, scenic pass guides, and community routes.
 */
@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
@Tag(name = "Adventure Trips", description = "Endpoints for discovering, viewing, and posting curated motorcycle trips and routes")
public class TripController {

	private final TripService tripService;

	@GetMapping
	@Operation(summary = "Get all trips", description = "Returns active adventure trips stored in Neo4j.")
	public ResponseEntity<List<Trip>> getAllTrips() {
		return ResponseEntity.ok(tripService.getAllTrips());
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get trip by ID", description = "Retrieves full waypoint and route details for a specific trip.")
	public ResponseEntity<Trip> getTripById(@PathVariable String id) {
		return tripService.getTripById(id)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	@PostMapping
	@Operation(summary = "Create trip", description = "Publishes a new adventure trip route to Neo4j.")
	public ResponseEntity<Trip> createTrip(@RequestBody Trip trip) {
		Trip created = tripService.createTrip(trip);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}
}
