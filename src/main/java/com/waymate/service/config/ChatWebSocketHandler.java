package com.waymate.service.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waymate.service.model.Message;
import com.waymate.service.model.Sos;
import com.waymate.service.service.AiAgentBridgeService;
import com.waymate.service.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance WebSocket handler for real-time chat messaging, delivery status updates,
 * typing indicators, and @WayMate AI Assistant mentions.
 */
@Component
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

	private final ChatService chatService;
	private final AiAgentBridgeService aiAgentBridgeService;
	private final ObjectMapper objectMapper;

	public ChatWebSocketHandler(
		ChatService chatService,
		AiAgentBridgeService aiAgentBridgeService,
		@org.springframework.beans.factory.annotation.Autowired(required = false) ObjectMapper objectMapper
	) {
		this.chatService = chatService;
		this.aiAgentBridgeService = aiAgentBridgeService;
		if (objectMapper != null) {
			this.objectMapper = objectMapper;
		} else {
			ObjectMapper mapper = new ObjectMapper();
			try {
				mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
				mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
			} catch (Throwable ignored) {}
			this.objectMapper = mapper;
		}
	}

	// Active sessions: sessionId -> WebSocketSession
	private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

	// User to sessions: userId -> Set<sessionId> (allows broadcasting across all user's devices/screens)
	private final Map<String, Set<String>> userToSessions = new ConcurrentHashMap<>();

	// Room subscriptions: roomId -> Set<sessionId> (actively viewing room conversation)
	private final Map<String, Set<String>> roomSubscribers = new ConcurrentHashMap<>();

	// Session metadata
	private final Map<String, String> sessionToUser = new ConcurrentHashMap<>();
	private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();

	private static final UUID AI_BOT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final String AI_BOT_NAME = "WayMate AI Assistant";
	private static final String AI_BOT_AVATAR = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=200&q=80";

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		String sessionId = session.getId();
		activeSessions.put(sessionId, session);
		log.info("WebSocket connected: session={}", sessionId);

		// Extract userId from connection query string if available (e.g. /ws/chat?userId=...)
		try {
			if (session.getUri() != null && session.getUri().getQuery() != null) {
				String query = session.getUri().getQuery();
				for (String param : query.split("&")) {
					String[] pair = param.split("=");
					if (pair.length == 2 && "userId".equalsIgnoreCase(pair[0])) {
						String userId = java.net.URLDecoder.decode(pair[1], java.nio.charset.StandardCharsets.UTF_8);
						registerUserSession(sessionId, userId);
						break;
					}
				}
			}
		} catch (Exception ignored) {}
	}

	private void registerUserSession(String sessionId, String userId) {
		if (userId == null || userId.isBlank()) return;
		sessionToUser.put(sessionId, userId);
		userToSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
		log.info("Session {} registered for user {}", sessionId, userId);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		String sessionId = session.getId();
		activeSessions.remove(sessionId);

		String userId = sessionToUser.remove(sessionId);
		if (userId != null) {
			Set<String> sessions = userToSessions.get(userId);
			if (sessions != null) {
				sessions.remove(sessionId);
				if (sessions.isEmpty()) {
					userToSessions.remove(userId);
				}
			}
		}

		String currentRoom = sessionToRoom.remove(sessionId);
		if (currentRoom != null) {
			Set<String> subscribers = roomSubscribers.get(currentRoom);
			if (subscribers != null) {
				subscribers.remove(sessionId);
				if (subscribers.isEmpty()) {
					roomSubscribers.remove(currentRoom);
				}
			}
		}
		log.info("WebSocket disconnected: session={}, status={}", sessionId, status);
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) {
		try {
			JsonNode json = objectMapper.readTree(message.getPayload());
			String type = json.has("type") ? json.get("type").asText() : "";

			switch (type) {
				case "IDENTIFY" -> handleIdentify(session, json);
				case "JOIN_ROOM" -> handleJoinRoom(session, json);
				case "LEAVE_ROOM" -> handleLeaveRoom(session, json);
				case "SEND_MESSAGE" -> handleSendMessage(session, json);
				case "MESSAGE_STATUS" -> handleMessageStatus(session, json);
				case "TYPING" -> handleTyping(session, json);
				case "PING" -> session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
				default -> log.debug("Unknown websocket packet type: {}", type);
			}
		} catch (Exception e) {
			log.error("Error processing websocket message from session {}: {}", session.getId(), e.getMessage());
		}
	}

	private void handleIdentify(WebSocketSession session, JsonNode json) {
		String userId = json.has("userId") ? json.get("userId").asText() : null;
		if (userId != null && !userId.isBlank()) {
			registerUserSession(session.getId(), userId);
		}
	}

	private void handleJoinRoom(WebSocketSession session, JsonNode json) throws IOException {
		String roomId = json.has("roomId") ? json.get("roomId").asText() : null;
		String userId = json.has("userId") ? json.get("userId").asText() : null;

		if (roomId == null || roomId.isBlank()) return;

		String sessionId = session.getId();
		if (userId != null) {
			registerUserSession(sessionId, userId);
		}

		// Remove from prior room if any
		String oldRoom = sessionToRoom.get(sessionId);
		if (oldRoom != null && !oldRoom.equals(roomId)) {
			Set<String> oldSubs = roomSubscribers.get(oldRoom);
			if (oldSubs != null) oldSubs.remove(sessionId);
		}

		sessionToRoom.put(sessionId, roomId);
		roomSubscribers.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

		log.info("Session {} (user {}) joined room {}", sessionId, userId, roomId);

		// Send ack
		Map<String, Object> ack = Map.of(
			"type", "ROOM_JOINED",
			"roomId", roomId,
			"timestamp", Instant.now().toString()
		);
		session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ack)));
	}

	private void handleLeaveRoom(WebSocketSession session, JsonNode json) {
		String roomId = json.has("roomId") ? json.get("roomId").asText() : sessionToRoom.get(session.getId());
		if (roomId != null) {
			Set<String> subscribers = roomSubscribers.get(roomId);
			if (subscribers != null) {
				subscribers.remove(session.getId());
			}
			sessionToRoom.remove(session.getId());
			log.info("Session {} left room {}", session.getId(), roomId);
		}
	}

	private void handleSendMessage(WebSocketSession session, JsonNode json) {
		try {
			String roomIdStr = json.get("roomId").asText();
			UUID roomId = UUID.fromString(roomIdStr);
			String senderIdStr = json.get("senderId").asText();
			UUID senderId = UUID.fromString(senderIdStr);
			String senderName = json.has("senderName") ? json.get("senderName").asText() : "Rider";
			String senderAvatar = json.has("senderAvatar") ? json.get("senderAvatar").asText() : null;
			String content = json.has("content") ? json.get("content").asText() : "";
			String msgType = json.has("msgType") ? json.get("msgType").asText() : "TEXT";
			String mediaUrl = json.has("mediaUrl") ? json.get("mediaUrl").asText() : null;
			String metadataJson = json.has("metadataJson") ? json.get("metadataJson").asText() : null;

			Message msg = Message.builder()
				.roomId(roomId)
				.senderId(senderId)
				.senderName(senderName)
				.senderAvatar(senderAvatar)
				.content(content)
				.type(msgType)
				.mediaUrl(mediaUrl)
				.metadataJson(metadataJson)
				.status("SENT")
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.readByUserIds(new ArrayList<>(List.of(senderIdStr)))
				.build();

			Message saved = chatService.saveMessage(msg);

			// Broadcast to room
			Map<String, Object> broadcastPacket = Map.of(
				"type", "NEW_MESSAGE",
				"message", saved
			);
			broadcastToRoom(roomIdStr, broadcastPacket);

			// Check for @WayMate AI Assistant mention!
			if (aiAgentBridgeService.containsAiMention(content)) {
				triggerAiAssistantReply(roomId, content, senderIdStr);
			}

		} catch (Exception e) {
			log.error("Failed to handle SEND_MESSAGE: {}", e.getMessage(), e);
		}
	}

	private void triggerAiAssistantReply(UUID roomId, String userText, String userId) {
		String roomIdStr = roomId.toString();

		// Broadcast AI typing indicator
		broadcastToRoom(roomIdStr, Map.of(
			"type", "AI_TYPING",
			"roomId", roomIdStr,
			"isTyping", true
		));

		CompletableFuture.runAsync(() -> {
			try {
				String cleanPrompt = aiAgentBridgeService.extractAiPrompt(userText);
				String aiReply = aiAgentBridgeService.queryAiAgent(cleanPrompt, roomIdStr, userId);

				Message aiMsg = Message.builder()
					.roomId(roomId)
					.senderId(AI_BOT_ID)
					.senderName(AI_BOT_NAME)
					.senderAvatar(AI_BOT_AVATAR)
					.content(aiReply)
					.type("AI_RESPONSE")
					.status("SENT")
					.createdAt(Instant.now())
					.updatedAt(Instant.now())
					.readByUserIds(new ArrayList<>())
					.build();

				Message savedAiMsg = chatService.saveMessage(aiMsg);

				// Broadcast AI Message to room
				broadcastToRoom(roomIdStr, Map.of(
					"type", "NEW_MESSAGE",
					"message", savedAiMsg
				));

			} catch (Exception e) {
				log.error("Error generating AI assistant response: {}", e.getMessage());
			} finally {
				// Turn off AI typing indicator
				broadcastToRoom(roomIdStr, Map.of(
					"type", "AI_TYPING",
					"roomId", roomIdStr,
					"isTyping", false
				));
			}
		});
	}

	private void handleMessageStatus(WebSocketSession session, JsonNode json) {
		try {
			String messageIdStr = json.get("messageId").asText();
			UUID messageId = UUID.fromString(messageIdStr);
			String status = json.get("status").asText(); // "DELIVERED" or "READ"
			String roomIdStr = json.has("roomId") ? json.get("roomId").asText() : null;
			String userIdStr = json.has("userId") ? json.get("userId").asText() : null;
			UUID userId = userIdStr != null ? UUID.fromString(userIdStr) : null;

			Message updated = chatService.updateMessageStatus(messageId, status, userId);

			Map<String, Object> statusPacket = Map.of(
				"type", "MESSAGE_STATUS_UPDATE",
				"messageId", messageIdStr,
				"status", status,
				"userId", userIdStr != null ? userIdStr : "",
				"roomId", roomIdStr != null ? roomIdStr : (updated.getRoomId() != null ? updated.getRoomId().toString() : "")
			);

			if (roomIdStr != null) {
				broadcastToRoom(roomIdStr, statusPacket);
			}
		} catch (Exception e) {
			log.error("Failed to handle MESSAGE_STATUS: {}", e.getMessage());
		}
	}

	private void handleTyping(WebSocketSession session, JsonNode json) {
		try {
			String roomIdStr = json.get("roomId").asText();
			String userId = json.has("userId") ? json.get("userId").asText() : "";
			String userName = json.has("userName") ? json.get("userName").asText() : "Rider";
			boolean isTyping = json.has("isTyping") && json.get("isTyping").asBoolean();

			Map<String, Object> typingPacket = Map.of(
				"type", "USER_TYPING",
				"roomId", roomIdStr,
				"userId", userId,
				"userName", userName,
				"isTyping", isTyping
			);

			broadcastToRoomExcept(roomIdStr, session.getId(), typingPacket);
		} catch (Exception e) {
			log.error("Failed to handle TYPING: {}", e.getMessage());
		}
	}

	public void broadcastToRoom(String roomId, Map<String, Object> payload) {
		broadcastToRoomExcept(roomId, null, payload);
	}

	public void broadcastToRoomExcept(String roomId, String excludedSessionId, Map<String, Object> payload) {
		Set<String> targetSessions = new HashSet<>();

		// 1. All active sessions viewing this conversation screen
		Set<String> subscribers = roomSubscribers.get(roomId);
		if (subscribers != null) {
			targetSessions.addAll(subscribers);
		}

		// 2. All active sessions belonging to any member of this chat room
		// This guarantees that all users receive the push notification and unread counter update
		// immediately, even when they are on Home, Garage, Social, Profile, or Chat List screens!
		try {
			UUID rId = UUID.fromString(roomId);
			List<String> memberIds = chatService.getRoomMemberIds(rId);
			for (String mid : memberIds) {
				Set<String> userSessions = userToSessions.get(mid);
				if (userSessions != null) {
					targetSessions.addAll(userSessions);
				}
			}
		} catch (Exception e) {
			log.debug("Could not resolve member sessions for room {}: {}", roomId, e.getMessage());
		}

		if (targetSessions.isEmpty()) return;

		try {
			String jsonString = objectMapper.writeValueAsString(payload);
			TextMessage textMsg = new TextMessage(jsonString);

			for (String sid : targetSessions) {
				if (excludedSessionId != null && excludedSessionId.equals(sid)) continue;
				WebSocketSession ws = activeSessions.get(sid);
				if (ws != null && ws.isOpen()) {
					synchronized (ws) {
						ws.sendMessage(textMsg);
					}
				}
			}
		} catch (Exception e) {
			log.error("Failed to broadcast to room {}: {}", roomId, e.getMessage());
		}
	}

	/**
	 * Public helper to broadcast a persisted new message to all room members.
	 */
	public void broadcastNewMessage(UUID roomId, Message message) {
		broadcastToRoom(roomId.toString(), Map.of(
			"type", "NEW_MESSAGE",
			"message", message
		));
	}

	/**
	 * Public helper to broadcast an urgent SOS emergency alert to all connected rider sessions.
	 */
	public void broadcastSosAlert(Sos sos, List<UUID> targetUserIds) {
		try {
			Map<String, Object> sosData = new HashMap<>();
			sosData.put("id", sos.getId().toString());
			sosData.put("name", sos.getName());
			sosData.put("location", sos.getLocation());
			sosData.put("latitude", sos.getLatitude());
			sosData.put("longitude", sos.getLongitude());
			sosData.put("senderId", sos.getUserId() != null ? sos.getUserId().toString() : null);
			sosData.put("senderName", sos.getUser() != null ? sos.getUser().getName() : "Nearby Rider");
			sosData.put("senderAvatar", sos.getUser() != null ? sos.getUser().getAvatar() : null);
			sosData.put("phoneNumber", sos.getPhoneNumber());
			sosData.put("bikeInfo", sos.getBikeInfo());
			sosData.put("description", sos.getDescription());
			sosData.put("createdAt", sos.getCreatedAt() != null ? sos.getCreatedAt().toString() : Instant.now().toString());

			Map<String, Object> packet = Map.of(
				"type", "SOS_ALERT",
				"sos", sosData
			);

			String jsonString = objectMapper.writeValueAsString(packet);
			TextMessage textMsg = new TextMessage(jsonString);

			Set<String> targetSessions = new HashSet<>();

			if (targetUserIds != null && !targetUserIds.isEmpty()) {
				for (UUID uid : targetUserIds) {
					Set<String> sessions = userToSessions.get(uid.toString());
					if (sessions != null) {
						targetSessions.addAll(sessions);
					}
				}
			} else {
				// Broadcast to all active sessions except the creator
				String creatorIdStr = sos.getUserId() != null ? sos.getUserId().toString() : null;
				for (Map.Entry<String, String> entry : sessionToUser.entrySet()) {
					if (creatorIdStr == null || !creatorIdStr.equals(entry.getValue())) {
						targetSessions.add(entry.getKey());
					}
				}
			}

			// If some connected sessions have not yet identified, also include them if targetUserIds is null
			if (targetUserIds == null || targetUserIds.isEmpty()) {
				targetSessions.addAll(activeSessions.keySet());
				if (sos.getUserId() != null) {
					Set<String> creatorSessions = userToSessions.get(sos.getUserId().toString());
					if (creatorSessions != null) {
						targetSessions.removeAll(creatorSessions);
					}
				}
			}

			for (String sid : targetSessions) {
				WebSocketSession ws = activeSessions.get(sid);
				if (ws != null && ws.isOpen()) {
					synchronized (ws) {
						ws.sendMessage(textMsg);
					}
				}
			}
			log.info("Broadcasted SOS_ALERT [id={}] to {} active sessions", sos.getId(), targetSessions.size());
		} catch (Exception e) {
			log.error("Failed to broadcast SOS_ALERT: {}", e.getMessage(), e);
		}
	}
}
