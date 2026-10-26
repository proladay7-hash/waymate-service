package com.waymate.service.payload.response;

import com.waymate.service.model.Sos;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SosResponse {
	private UUID id;
	private String name;
	private String location;
	private Double latitude;
	private Double longitude;
	private UUID userId;
	private String userName;
	private String userAvatar;
	private String phoneNumber;
	private String description;
	private String bikeInfo;
	private String status;
	private Double distanceKm; // Distance relative to requesting user
	private Instant createdAt;
	private Instant updatedAt;

	public static SosResponse fromSos(Sos sos) {
		if (sos == null) return null;
		return SosResponse.builder()
			.id(sos.getId())
			.name(sos.getName())
			.location(sos.getLocation())
			.latitude(sos.getLatitude())
			.longitude(sos.getLongitude())
			.userId(sos.getUserId())
			.userName(sos.getUser() != null ? sos.getUser().getName() : null)
			.userAvatar(sos.getUser() != null ? sos.getUser().getAvatar() : null)
			.phoneNumber(sos.getPhoneNumber() != null ? sos.getPhoneNumber() : (sos.getUser() != null ? sos.getUser().getPhoneNumber() : null))
			.description(sos.getDescription())
			.bikeInfo(sos.getBikeInfo())
			.status(sos.getStatus())
			.createdAt(sos.getCreatedAt())
			.updatedAt(sos.getUpdatedAt())
			.build();
	}
}
