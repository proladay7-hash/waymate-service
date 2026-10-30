package com.waymate.service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waymate.service.model.Message;
import com.waymate.service.payload.ChatPubSubEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service that publishes chat and WebSocket events to Redis Pub/Sub,
 * allowing instant cross-server routing to any connected server instance.
 */
@Service
@Slf4j
public class RedisChatPublisher {

	public static final String CHAT_CHANNEL = "waymate:chat:events";

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;
	private final ChannelTopic chatChannelTopic;

	public RedisChatPublisher(
		StringRedisTemplate stringRedisTemplate,
		ObjectMapper objectMapper,
		ChannelTopic chatChannelTopic
	) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
		this.chatChannelTopic = chatChannelTopic;
	}

	/**
	 * Publish a newly persisted message to all servers.
	 */
	public boolean publishNewMessage(String roomId, String senderSessionId, Message message) {
		ChatPubSubEvent event = ChatPubSubEvent.builder()
			.type("NEW_MESSAGE")
			.roomId(roomId)
			.senderSessionId(senderSessionId)
			.message(message)
			.build();

		return publish(event);
	}

	/**
	 * Publish a room event (typing indicator, status update, etc.) to all servers.
	 */
	public boolean publishRoomEvent(String roomId, String senderSessionId, String type, Map<String, Object> payload) {
		ChatPubSubEvent event = ChatPubSubEvent.builder()
			.type(type)
			.roomId(roomId)
			.senderSessionId(senderSessionId)
			.payload(payload)
			.build();

		return publish(event);
	}

	/**
	 * Publish an SOS emergency alert to all servers.
	 */
	public boolean publishSosAlert(Map<String, Object> sosPayload) {
		ChatPubSubEvent event = ChatPubSubEvent.builder()
			.type("SOS_ALERT")
			.payload(sosPayload)
			.build();

		return publish(event);
	}

	private boolean publish(ChatPubSubEvent event) {
		try {
			String json = objectMapper.writeValueAsString(event);
			stringRedisTemplate.convertAndSend(chatChannelTopic.getTopic(), json);
			return true;
		} catch (Exception e) {
			log.error("Failed to publish chat event to Redis: {}", e.getMessage(), e);
			return false;
		}
	}
}
