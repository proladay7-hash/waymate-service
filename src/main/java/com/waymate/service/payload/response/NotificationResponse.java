package com.waymate.service.payload.response;

import com.waymate.service.model.Notification;
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
public class NotificationResponse {
	private UUID id;
	private UUID userId;
	private String title;
	private String content;
	private String type; // SOS, CHAT_MESSAGE, POST_LIKE, POST_COMMENT, POST_SHARE, SYSTEM
	private UUID senderId;
	private String senderName;
	private String senderAvatar;
	private String referenceId;
	private String metadataJson;
	private Boolean isRead;
	private Instant createdAt;
	private Instant updatedAt;

	public static NotificationResponse fromNotification(Notification n) {
		if (n == null) return null;
		return NotificationResponse.builder()
			.id(n.getId())
			.userId(n.getUserId())
			.title(n.getTitle())
			.content(n.getContent())
			.type(n.getType())
			.senderId(n.getSenderId())
			.senderName(n.getSenderName())
			.senderAvatar(n.getSenderAvatar())
			.referenceId(n.getReferenceId())
			.metadataJson(n.getMetadataJson())
			.isRead(n.getIsRead())
			.createdAt(n.getCreatedAt())
			.updatedAt(n.getUpdatedAt())
			.build();
	}
}
