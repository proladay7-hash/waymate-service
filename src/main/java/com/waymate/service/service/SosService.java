package com.waymate.service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waymate.service.model.Sos;
import com.waymate.service.model.User;
import com.waymate.service.model.Vehicle;
import com.waymate.service.payload.request.SosRequest;
import com.waymate.service.payload.response.SosResponse;
import com.waymate.service.repository.SosRepository;
import com.waymate.service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service managing emergency SOS alerts and dispatching nearby radius notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SosService {

	private final SosRepository sosRepository;
	private final UserRepository userRepository;
	private final NotificationService notificationService;
	private final com.waymate.service.config.ChatWebSocketHandler chatWebSocketHandler;
	private final ObjectMapper objectMapper = new ObjectMapper();

	private static final double SOS_RADIUS_KM = 10.0;

	/**
	 * Create and broadcast an emergency SOS request to riders within 10km.
	 */
	public SosResponse createSos(SosRequest request) {
		Optional<User> userOpt = request.getUserId() != null ? userRepository.findById(request.getUserId()) : Optional.empty();
		User user = userOpt.orElse(null);

		// Resolve bike info if not provided
		String bikeInfo = request.getBikeInfo();
		if ((bikeInfo == null || bikeInfo.isBlank()) && user != null && user.getVehicles() != null && !user.getVehicles().isEmpty()) {
			Vehicle v = user.getVehicles().get(0);
			bikeInfo = String.format("%s %s (%s)",
				v.getBrand() != null ? v.getBrand() : "",
				v.getName() != null ? v.getName() : "Motorcycle",
				v.getPlateNumber() != null ? v.getPlateNumber() : "No Plate"
			).trim();
		}

		String phone = request.getPhoneNumber();
		if ((phone == null || phone.isBlank()) && user != null) {
			phone = user.getPhoneNumber();
		}

		Sos sos = Sos.builder()
			.name(request.getName() != null ? request.getName() : "Emergency Roadside Assistance")
			.location(request.getLocation() != null ? request.getLocation() : "Current GPS Coordinates")
			.latitude(request.getLatitude() != null ? request.getLatitude() : 11.9404)
			.longitude(request.getLongitude() != null ? request.getLongitude() : 108.4583)
			.userId(request.getUserId())
			.description(request.getDescription())
			.phoneNumber(phone)
			.bikeInfo(bikeInfo)
			.status("PENDING")
			.user(user)
			.createdAt(Instant.now())
			.updatedAt(Instant.now())
			.build();

		Sos saved = sosRepository.save(sos);
		log.info("Saved new SOS [id={}, user={}, location={}]", saved.getId(), request.getUserId(), saved.getLocation());

		// Update user's latest location if user exists
		if (user != null && request.getLatitude() != null && request.getLongitude() != null) {
			user.setLatitude(request.getLatitude());
			user.setLongitude(request.getLongitude());
			userRepository.save(user);
		}

		// Notify all users within 10km radius & push real-time WebSocket alert
		List<UUID> notifiedRiderIds = notifyNearbyRiders(saved);
		log.info("SOS [id={}] notified {} nearby riders within 10km", saved.getId(), notifiedRiderIds.size());

		if (chatWebSocketHandler != null) {
			chatWebSocketHandler.broadcastSosAlert(saved, notifiedRiderIds);
		}

		return SosResponse.fromSos(saved);
	}

	/**
	 * Find nearby riders within 10km and produce Notification records in database right away.
	 */
	private List<UUID> notifyNearbyRiders(Sos sos) {
		List<User> allUsers = userRepository.findAll();
		List<UUID> notifiedRiderIds = new ArrayList<>();
		double sosLat = sos.getLatitude() != null ? sos.getLatitude() : 11.9404;
		double sosLng = sos.getLongitude() != null ? sos.getLongitude() : 108.4583;

		String senderName = sos.getUser() != null ? sos.getUser().getName() : "Nearby Rider";
		String senderAvatar = sos.getUser() != null ? sos.getUser().getAvatar() : null;

		for (User rider : allUsers) {
			// Do not notify self
			if (sos.getUserId() != null && sos.getUserId().equals(rider.getId())) {
				continue;
			}

			double distance = Double.MAX_VALUE;
			if (rider.getLatitude() != null && rider.getLongitude() != null) {
				distance = calculateDistanceKm(sosLat, sosLng, rider.getLatitude(), rider.getLongitude());
			} else {
				// If rider location is unknown, include for community emergency alert
				distance = 2.5; // default within 10km for demo community members
			}

			if (distance <= SOS_RADIUS_KM) {
				try {
					Map<String, Object> meta = new HashMap<>();
					meta.put("sosId", sos.getId().toString());
					meta.put("lat", sosLat);
					meta.put("lng", sosLng);
					meta.put("location", sos.getLocation());
					meta.put("distanceKm", Math.round(distance * 10.0) / 10.0);
					meta.put("phone", sos.getPhoneNumber());
					meta.put("bikeInfo", sos.getBikeInfo());

					String title = "🚨 SOS EMERGENCY: " + sos.getName();
					String content = String.format("%s needs bike help near %s (approx. %.1f km away).",
						senderName, sos.getLocation(), distance);

					notificationService.createNotification(
						rider.getId(),
						title,
						content,
						"SOS",
						sos.getUserId(),
						senderName,
						senderAvatar,
						sos.getId().toString(),
						objectMapper.writeValueAsString(meta)
					);
					notifiedRiderIds.add(rider.getId());
				} catch (Exception e) {
					log.error("Failed to produce SOS notification for user {}: {}", rider.getId(), e.getMessage());
				}
			}
		}
		return notifiedRiderIds;
	}

	/**
	 * Get all active SOS requests, with calculated distance relative to caller if coordinates provided.
	 */
	public List<SosResponse> getActiveSos(Double currentLat, Double currentLng) {
		List<Sos> list = sosRepository.findAllActiveSos();
		return list.stream().map(s -> {
			SosResponse resp = SosResponse.fromSos(s);
			if (currentLat != null && currentLng != null && s.getLatitude() != null && s.getLongitude() != null) {
				double dist = calculateDistanceKm(currentLat, currentLng, s.getLatitude(), s.getLongitude());
				resp.setDistanceKm(Math.round(dist * 10.0) / 10.0);
			}
			return resp;
		}).collect(Collectors.toList());
	}

	/**
	 * Get detailed SOS by ID.
	 */
	public SosResponse getSosById(UUID sosId) {
		Sos sos = sosRepository.findById(sosId)
			.orElseThrow(() -> new IllegalArgumentException("SOS request not found with id: " + sosId));
		return SosResponse.fromSos(sos);
	}

	/**
	 * Resolve or cancel an SOS request.
	 */
	public SosResponse updateStatus(UUID sosId, String status) {
		Sos sos = sosRepository.findById(sosId)
			.orElseThrow(() -> new IllegalArgumentException("SOS request not found with id: " + sosId));
		sos.setStatus(status != null ? status : "RESOLVED");
		sos.setUpdatedAt(Instant.now());
		return SosResponse.fromSos(sosRepository.save(sos));
	}

	/**
	 * Haversine formula to compute great-circle distance between two GPS points in kilometers.
	 */
	public static double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
		final int R = 6371; // Earth's mean radius in km
		double latDistance = Math.toRadians(lat2 - lat1);
		double lonDistance = Math.toRadians(lon2 - lon1);
		double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
				* Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return R * c;
	}
}
