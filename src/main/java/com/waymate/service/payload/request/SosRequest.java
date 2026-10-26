package com.waymate.service.payload.request;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SosRequest {
	private UUID userId;
	private String name; // Issue title: e.g. "Puncture / Flat Tyre", "Engine Breakdown"
	private String location; // Address / landmark description
	private Double latitude;
	private Double longitude;
	private String description; // Additional details
	private String phoneNumber; // Contact phone
	private String bikeInfo; // Bike make/model/plate
}
