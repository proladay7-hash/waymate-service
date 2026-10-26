package com.waymate.service.service;

import com.waymate.service.model.Trip;
import com.waymate.service.repository.TripRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service managing adventure trips and scenic motorcycle routes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {

	private final TripRepository tripRepository;

	public List<Trip> getAllTrips() {
		return tripRepository.findAllActiveTrips();
	}

	public Optional<Trip> getTripById(String id) {
		return tripRepository.findById(id);
	}

	public Trip createTrip(Trip trip) {
		if (trip.getId() == null || trip.getId().isBlank()) {
			trip.setId("trip-" + UUID.randomUUID().toString().substring(0, 8));
		}
		if (trip.getCreatedAt() == null) {
			trip.setCreatedAt(Instant.now());
		}
		trip.setUpdatedAt(Instant.now());
		return tripRepository.save(trip);
	}

	public List<Trip> saveAll(List<Trip> trips) {
		trips.forEach(t -> {
			if (t.getId() == null || t.getId().isBlank()) {
				t.setId("trip-" + UUID.randomUUID().toString().substring(0, 8));
			}
			if (t.getCreatedAt() == null) {
				t.setCreatedAt(Instant.now());
			}
			t.setUpdatedAt(Instant.now());
		});
		return tripRepository.saveAll(trips);
	}
}
