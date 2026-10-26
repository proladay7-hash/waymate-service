package com.waymate.service.model;

import java.util.ArrayList;
import java.util.List;
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

/**
 * Message Neo4j Node entity.
 * Represents a chat message in a direct or group conversation.
 * Supports rich media (photo, video), location sharing, trip sharing, and AI assistant answers.
 */
@Node("Message")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Message extends BaseModel {

	@Id
	@GeneratedValue(GeneratedValue.UUIDGenerator.class)
	@Property("id")
	private UUID id;

	@Property("room_id")
	private UUID roomId;

	@Property("sender_id")
	private UUID senderId;

	@Property("sender_name")
	private String senderName;

	@Property("sender_avatar")
	private String senderAvatar;

	@Property("content")
	private String content;

	/**
	 * Message types:
	 * - TEXT: Plain text
	 * - IMAGE: Picture attachment
	 * - VIDEO: Video clip attachment
	 * - LOCATION: GPS location card
	 * - TRIP: Route / Trip card
	 * - AI_RESPONSE: Answer from @WayMate AI Assistant
	 */
	@Property("type")
	@Builder.Default
	private String type = "TEXT";

	@Property("media_url")
	private String mediaUrl;

	/**
	 * Extra metadata for rich items in JSON format:
	 * e.g. for LOCATION: {"lat": 11.94, "lng": 108.45, "address": "Da Lat"}
	 * e.g. for TRIP: {"tripId": "trip-1", "title": "Da Lat Loop", "distanceKm": 310}
	 */
	@Property("metadata_json")
	private String metadataJson;

	/**
	 * Message status:
	 * - SENT: Dispatched to server
	 * - DELIVERED: Reached recipient device(s)
	 * - READ: Viewed by recipient(s)
	 */
	@Property("status")
	@Builder.Default
	private String status = "SENT";

	@Property("read_by_user_ids")
	@Builder.Default
	private List<String> readByUserIds = new ArrayList<>();
}
