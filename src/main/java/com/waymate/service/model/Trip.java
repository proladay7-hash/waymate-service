package com.waymate.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

/**
 * Trip Neo4j Node entity.
 * Represents an adventure motorcycle or road-trip route with waypoints, lodging, pit stops, and telemetry.
 */
@Node("Trip")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Trip extends BaseModel {

	@Id
	@Property("id")
	private String id;

	@Property("title")
	private String title;

	@Property("title_vie")
	private String titleVie;

	@Property("rider_name")
	private String riderName;

	@Property("rider_avatar")
	private String riderAvatar;

	@Property("rider_badge")
	private String riderBadge;

	@Property("rider_badge_vie")
	private String riderBadgeVie;

	@Property("bike_model")
	private String bikeModel;

	@Property("distance_km")
	private Double distanceKm;

	@Property("avg_velocity_kmh")
	private Double avgVelocityKmh;

	@Property("max_velocity_kmh")
	private Double maxVelocityKmh;

	@Property("group_size")
	private String groupSize;

	@Property("duration")
	private String duration;

	@Property("duration_vie")
	private String durationVie;

	@Property("dates")
	private String dates;

	@Property("category")
	@Builder.Default
	private String category = "popular";

	@Property("cover_image")
	private String coverImage;

	@Property("likes_count")
	@Builder.Default
	private Integer likesCount = 0;

	@Property("comments_count")
	@Builder.Default
	private Integer commentsCount = 0;

	@Property("total_cost_vnd")
	private String totalCostVnd;

	@Property("total_cost_usd")
	private String totalCostUsd;

	@Property("description")
	private String description;

	@Property("description_vie")
	private String descriptionVie;

	@Property("google_maps_url")
	private String googleMapsUrl;

	@Property("destination_json")
	private String destinationJson;

	@Property("eat_drink_json")
	private String eatDrinkJson;

	@Property("landmarks_json")
	private String landmarksJson;

	@Property("lodging_json")
	private String lodgingJson;
}
