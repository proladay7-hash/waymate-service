package com.waymate.service.payload.request;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for dispatching/persisting a notification to a user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {

	private UUID userId;
	private String title;
	private String content;
	private String type; // SOS, CHAT_MESSAGE, POST_LIKE, POST_COMMENT, POST_SHARE, SYSTEM
	private UUID senderId;
	private String senderName;
	private String senderAvatar;
	private String referenceId;
	private String metadataJson;
}
