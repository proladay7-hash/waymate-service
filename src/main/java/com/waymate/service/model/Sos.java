package com.waymate.service.model;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * SOS Neo4j Node entity.
 * Represents emergency assistance requests from riders with bike issues.
 */
@Node("SOS")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Sos extends BaseModel {

	@Id
	@GeneratedValue(GeneratedValue.UUIDGenerator.class)
	@Property("id")
	private UUID id;

	@Property("name")
	private String name; // Issue name / title (e.g., "Flat Tyre / Thủng lốp", "Engine Breakdown / Chết máy")

	@Property("location")
	private String location; // Address / landmark text description

	@Property("latitude")
	private Double latitude;

	@Property("longitude")
	private Double longitude;

	@Property("user_id")
	private UUID userId;

	@Property("description")
	private String description;

	@Property("phone_number")
	private String phoneNumber;

	@Property("bike_info")
	private String bikeInfo; // e.g. "Honda CB500X • 59-X1 123.45"

	@Property("status")
	@Builder.Default
	private String status = "PENDING"; // PENDING, HELPING, RESOLVED, CANCELLED

	@Relationship(type = "REQUESTED_BY", direction = Relationship.Direction.OUTGOING)
	private User user;
}
