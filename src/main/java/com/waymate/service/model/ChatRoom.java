package com.waymate.service.model;

import java.time.Instant;
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
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

/**
 * ChatRoom Neo4j Node entity.
 * Supports both 1-on-1 direct conversations and multi-user group chats.
 */
@Node("ChatRoom")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ChatRoom extends BaseModel {

	@Id
	@GeneratedValue(GeneratedValue.UUIDGenerator.class)
	@Property("id")
	private UUID id;

	@Property("name")
	private String name;

	@Property("type")
	@Builder.Default
	private String type = "DIRECT"; // "DIRECT" or "GROUP"

	@Property("avatar")
	private String avatar;

	@Property("created_by")
	private UUID createdById;

	@Property("last_message_content")
	private String lastMessageContent;

	@Property("last_message_sender")
	private String lastMessageSender;

	@Property("last_message_time")
	private Instant lastMessageTime;

	@Property("member_ids")
	@Builder.Default
	private List<String> memberIds = new ArrayList<>();

	@Transient
	@Builder.Default
	private Integer unreadCount = 0;
}
