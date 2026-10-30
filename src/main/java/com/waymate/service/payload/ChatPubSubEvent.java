package com.waymate.service.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.waymate.service.model.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * Event payload distributed across all WayMate server instances via Redis Pub/Sub.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatPubSubEvent implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Event type: NEW_MESSAGE, USER_TYPING, MESSAGE_STATUS, AI_TYPING, SOS_ALERT
	 */
	private String type;

	/**
	 * Target chat room ID
	 */
	private String roomId;

	/**
	 * Session ID that originated this event (optional, for echo cancellation)
	 */
	private String senderSessionId;

	/**
	 * Message object if type is NEW_MESSAGE
	 */
	private Message message;

	/**
	 * Generic payload map for other WebSocket notifications
	 */
	private Map<String, Object> payload;
}
