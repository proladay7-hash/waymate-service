package com.waymate.service.controller;

import com.waymate.service.model.ChatRoom;
import com.waymate.service.model.Message;
import com.waymate.service.repository.UserRepository;
import com.waymate.service.service.AiAgentBridgeService;
import com.waymate.service.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for chat conversations, messages, group creation, and delivery statuses.
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat & Messaging", description = "Endpoints for 1-on-1 and Group chats, Message history & statuses")
public class ChatController {

	private final ChatService chatService;
	private final UserRepository userRepository;
	private final AiAgentBridgeService aiAgentBridgeService;
	private final com.waymate.service.config.ChatWebSocketHandler chatWebSocketHandler;

	// Request DTOs
	@Data @NoArgsConstructor @AllArgsConstructor @Builder
	public static class CreateDirectChatRequest {
		private UUID currentUserId;
		private UUID otherUserId;
	}

	@Data @NoArgsConstructor @AllArgsConstructor @Builder
	public static class CreateGroupChatRequest {
		private UUID creatorId;
		private String name;
		private String avatar;
		private List<UUID> memberIds;
	}

	@Data @NoArgsConstructor @AllArgsConstructor @Builder
	public static class AddMembersRequest {
		private List<UUID> memberIds;
	}

	@Data @NoArgsConstructor @AllArgsConstructor @Builder
	public static class SendMessageRestRequest {
		private UUID roomId;
		private UUID senderId;
		private String senderName;
		private String senderAvatar;
		private String content;
		private String type; // TEXT, IMAGE, VIDEO, LOCATION, TRIP
		private String mediaUrl;
		private String metadataJson;
	}

	@Data @NoArgsConstructor @AllArgsConstructor @Builder
	public static class UpdateStatusRequest {
		private UUID userId;
		private String status; // DELIVERED, READ
	}

	/**
	 * Get all chat rooms for a user.
	 */
	@GetMapping("/rooms")
	@Operation(summary = "Get user chat rooms", description = "Returns all direct and group conversations for the user.")
	public ResponseEntity<List<ChatRoom>> getUserRooms(@RequestParam UUID userId) {
		List<ChatRoom> rooms = chatService.getUserRooms(userId);
		return ResponseEntity.ok(rooms);
	}

	/**
	 * Get total unread chat messages count for a user across all rooms.
	 */
	@GetMapping("/unread-count")
	@Operation(summary = "Get total unread chat messages count for a user")
	public ResponseEntity<Map<String, Object>> getTotalUnreadCount(@RequestParam UUID userId) {
		long count = chatService.getTotalUnreadCount(userId);
		return ResponseEntity.ok(Map.of("unreadCount", count));
	}

	/**
	 * Get or create 1-on-1 direct chat room.
	 */
	@PostMapping("/rooms/direct")
	@Operation(summary = "Create or get direct chat", description = "Finds existing direct chat between two users or creates a new one.")
	public ResponseEntity<ChatRoom> getOrCreateDirectRoom(@RequestBody CreateDirectChatRequest request) {
		ChatRoom room = chatService.getOrCreateDirectRoom(request.getCurrentUserId(), request.getOtherUserId());
		return ResponseEntity.ok(room);
	}

	/**
	 * Create a multi-user group chat room.
	 */
	@PostMapping("/rooms/group")
	@Operation(summary = "Create group chat", description = "Creates a new multi-member group chat room like Messenger/Zalo.")
	public ResponseEntity<ChatRoom> createGroupRoom(@RequestBody CreateGroupChatRequest request) {
		ChatRoom room = chatService.createGroupRoom(
			request.getCreatorId(),
			request.getName(),
			request.getAvatar(),
			request.getMemberIds()
		);
		return ResponseEntity.status(HttpStatus.CREATED).body(room);
	}

	/**
	 * Add member(s) to a group.
	 */
	@PostMapping("/rooms/{roomId}/members")
	@Operation(summary = "Add members to group", description = "Adds new member(s) to an existing group chat room.")
	public ResponseEntity<ChatRoom> addMembers(@PathVariable UUID roomId, @RequestBody AddMembersRequest request) {
		ChatRoom room = chatService.addMembersToGroup(roomId, request.getMemberIds());
		return ResponseEntity.ok(room);
	}

	/**
	 * Get message history for a chat room.
	 */
	@GetMapping("/rooms/{roomId}/messages")
	@Operation(summary = "Get room messages", description = "Fetches message thread history for a chat room.")
	public ResponseEntity<List<Message>> getRoomMessages(@PathVariable UUID roomId) {
		List<Message> messages = chatService.getRoomMessages(roomId);
		return ResponseEntity.ok(messages);
	}

	/**
	 * Send a message via REST (fallback or initial dispatch).
	 */
	@PostMapping("/rooms/{roomId}/messages")
	@Operation(summary = "Send message (REST)", description = "Sends and persists a chat message; checks for @WayMate tag.")
	public ResponseEntity<Message> sendMessage(@PathVariable UUID roomId, @RequestBody SendMessageRestRequest request) {
		Message msg = Message.builder()
			.roomId(roomId)
			.senderId(request.getSenderId())
			.senderName(request.getSenderName() != null ? request.getSenderName() : "Rider")
			.senderAvatar(request.getSenderAvatar())
			.content(request.getContent())
			.type(request.getType() != null ? request.getType() : "TEXT")
			.mediaUrl(request.getMediaUrl())
			.metadataJson(request.getMetadataJson())
			.status("SENT")
			.createdAt(Instant.now())
			.updatedAt(Instant.now())
			.readByUserIds(new ArrayList<>(List.of(request.getSenderId().toString())))
			.build();

		Message saved = chatService.saveMessage(msg);
		if (chatWebSocketHandler != null) {
			chatWebSocketHandler.broadcastNewMessage(roomId, saved);
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	}

	/**
	 * Update message status (DELIVERED or READ).
	 */
	@PostMapping("/messages/{messageId}/status")
	@Operation(summary = "Update message status", description = "Updates message delivery state (DELIVERED or READ).")
	public ResponseEntity<Message> updateStatus(@PathVariable UUID messageId, @RequestBody UpdateStatusRequest request) {
		Message updated = chatService.updateMessageStatus(messageId, request.getStatus(), request.getUserId());
		return ResponseEntity.ok(updated);
	}
}
