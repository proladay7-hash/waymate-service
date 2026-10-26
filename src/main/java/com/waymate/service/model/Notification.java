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

/**
 * Notification Neo4j Node entity.
 * Represents alerts and notifications for chat messages, post reactions (like/comment/share), and emergency SOS.
 */
@Node("Notification")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Notification extends BaseModel {

	@Id
	@GeneratedValue(GeneratedValue.UUIDGenerator.class)
	@Property("id")
	private UUID id;

	@Property("user_id")
	private UUID userId;

	@Property("title")
	private String title;

	@Property("content")
	private String content;

	@Property("type")
	private String type; // SOS, CHAT_MESSAGE, POST_LIKE, POST_COMMENT, POST_SHARE, SYSTEM

	@Property("sender_id")
	private UUID senderId;

	@Property("sender_name")
	private String senderName;

	@Property("sender_avatar")
	private String senderAvatar;

	@Property("reference_id")
	private String referenceId; // e.g. postId, roomId, sosId

	@Property("metadata_json")
	private String metadataJson;

	@Property("is_read")
	@Builder.Default
	private Boolean isRead = false;

	public boolean isRead() {
		return Boolean.TRUE.equals(isRead);
	}
}
