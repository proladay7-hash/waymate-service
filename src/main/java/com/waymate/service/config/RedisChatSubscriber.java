package com.waymate.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waymate.service.payload.ChatPubSubEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Subscriber that listens to Redis Pub/Sub events and broadcasts them down to
 * local WebSocket sessions connected to this server instance.
 */
@Component
@Slf4j
public class RedisChatSubscriber implements MessageListener {

	private final ChatWebSocketHandler chatWebSocketHandler;
	private final ObjectMapper objectMapper;

	public RedisChatSubscriber(@Lazy ChatWebSocketHandler chatWebSocketHandler, ObjectMapper objectMapper) {
		this.chatWebSocketHandler = chatWebSocketHandler;
		this.objectMapper = objectMapper;
	}

	@Override
	public void onMessage(Message message, byte[] pattern) {
		try {
			String body = new String(message.getBody(), StandardCharsets.UTF_8);
			ChatPubSubEvent event = objectMapper.readValue(body, ChatPubSubEvent.class);

			if (event == null || event.getType() == null) {
				return;
			}

			String type = event.getType();
			String roomId = event.getRoomId();
			String senderSessionId = event.getSenderSessionId();

			switch (type) {
				case "NEW_MESSAGE" -> {
					if (roomId != null && event.getMessage() != null) {
						Map<String, Object> packet = Map.of(
							"type", "NEW_MESSAGE",
							"message", event.getMessage()
						);
						chatWebSocketHandler.broadcastToLocalRoom(roomId, null, packet);
					}
				}
				case "USER_TYPING", "AI_TYPING", "MESSAGE_STATUS" -> {
					if (roomId != null && event.getPayload() != null) {
						chatWebSocketHandler.broadcastToLocalRoom(roomId, senderSessionId, event.getPayload());
					}
				}
				case "SOS_ALERT" -> {
					if (event.getPayload() != null) {
						chatWebSocketHandler.broadcastToLocalSos(event.getPayload());
					}
				}
				default -> {
					if (roomId != null && event.getPayload() != null) {
						chatWebSocketHandler.broadcastToLocalRoom(roomId, senderSessionId, event.getPayload());
					}
				}
			}
		} catch (Exception e) {
			log.error("Failed to process Redis Pub/Sub chat message: {}", e.getMessage(), e);
		}
	}
}
