package com.waymate.service.service;

import com.waymate.service.model.ChatRoom;
import com.waymate.service.model.Message;
import com.waymate.service.model.User;
import com.waymate.service.repository.ChatRoomRepository;
import com.waymate.service.repository.MessageRepository;
import com.waymate.service.repository.UserRepository;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;

/**
 * Service managing chat rooms, messages, group conversations, and delivery statuses.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

	private final ChatRoomRepository chatRoomRepository;
	private final MessageRepository messageRepository;
	private final UserRepository userRepository;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	/**
	 * Get all chat rooms (direct & group) that a user is a member of.
	 */
	public List<ChatRoom> getUserRooms(UUID userId) {
		String uidStr = userId.toString();
		List<ChatRoom> rooms = chatRoomRepository.findRoomsByUserId(uidStr);

		// Populate names, avatars, and accurate unread counts for rooms
		for (ChatRoom room : rooms) {
			if ("DIRECT".equalsIgnoreCase(room.getType())) {
				resolveDirectRoomMetadata(room, userId);
			}
			try {
				long unread = messageRepository.countUnreadMessagesByRoom(room.getId(), userId);
				room.setUnreadCount((int) unread);
			} catch (Exception e) {
				room.setUnreadCount(0);
			}
		}
		return rooms;
	}

	/**
	 * Get total unread messages count for a user across all joined conversations.
	 */
	public long getTotalUnreadCount(UUID userId) {
		try {
			return messageRepository.countTotalUnreadForUser(userId, userId.toString());
		} catch (Exception e) {
			return 0L;
		}
	}

	/**
	 * Get or create a 1-on-1 direct chat room between two users.
	 */
	public ChatRoom getOrCreateDirectRoom(UUID user1Id, UUID user2Id) {
		String u1 = user1Id.toString();
		String u2 = user2Id.toString();

		Optional<ChatRoom> existing = chatRoomRepository.findDirectRoomBetween(u1, u2);
		if (existing.isPresent()) {
			ChatRoom room = existing.get();
			resolveDirectRoomMetadata(room, user1Id);
			return room;
		}

		// Look up user2's profile for room display name and avatar
		Optional<User> otherUserOpt = userRepository.findById(user2Id);
		String otherName = otherUserOpt.map(User::getName).orElse("Rider");
		String otherAvatar = otherUserOpt.map(User::getAvatar).orElse(null);

		ChatRoom newRoom = ChatRoom.builder()
			.name(otherName)
			.type("DIRECT")
			.avatar(otherAvatar)
			.createdById(user1Id)
			.memberIds(new ArrayList<>(List.of(u1, u2)))
			.createdAt(Instant.now())
			.updatedAt(Instant.now())
			.build();

		ChatRoom saved = chatRoomRepository.save(newRoom);
		resolveDirectRoomMetadata(saved, user1Id);
		return saved;
	}

	/**
	 * Create a new multi-member group chat room.
	 */
	public ChatRoom createGroupRoom(UUID creatorId, String groupName, String avatar, List<UUID> memberIds) {
		Set<String> allMembers = new HashSet<>();
		allMembers.add(creatorId.toString());
		if (memberIds != null) {
			for (UUID mid : memberIds) {
				allMembers.add(mid.toString());
			}
		}

		ChatRoom newGroup = ChatRoom.builder()
			.name((groupName != null && !groupName.isBlank()) ? groupName : "Riders Adventure Group")
			.type("GROUP")
			.avatar((avatar != null && !avatar.isBlank()) ? avatar : "https://images.unsplash.com/photo-1558981403-c5f9899a28bc?auto=format&fit=crop&w=300&q=80")
			.createdById(creatorId)
			.memberIds(new ArrayList<>(allMembers))
			.createdAt(Instant.now())
			.updatedAt(Instant.now())
			.build();

		return chatRoomRepository.save(newGroup);
	}

	/**
	 * Add member(s) to an existing group room.
	 */
	public ChatRoom addMembersToGroup(UUID roomId, List<UUID> newMemberIds) {
		ChatRoom room = chatRoomRepository.findById(roomId)
			.orElseThrow(() -> new IllegalArgumentException("Chat room not found with id: " + roomId));

		Set<String> memberSet = new HashSet<>(room.getMemberIds() != null ? room.getMemberIds() : List.of());
		for (UUID mid : newMemberIds) {
			memberSet.add(mid.toString());
		}

		room.setMemberIds(new ArrayList<>(memberSet));
		room.setUpdatedAt(Instant.now());
		return chatRoomRepository.save(room);
	}

	/**
	 * Get message history for a specific room.
	 * Checks Redis List (recent 50 messages) first; if miss, loads from Neo4j and hydrates Redis.
	 */
	public List<Message> getRoomMessages(UUID roomId) {
		String cacheKey = "chat:room:" + roomId + ":recent";
		try {
			List<String> cachedJsonList = redisTemplate.opsForList().range(cacheKey, 0, -1);
			if (cachedJsonList != null && !cachedJsonList.isEmpty()) {
				List<Message> cachedMessages = new ArrayList<>();
				for (String json : cachedJsonList) {
					cachedMessages.add(objectMapper.readValue(json, Message.class));
				}
				log.debug("Loaded {} messages from Redis cache for room {}", cachedMessages.size(), roomId);
				return cachedMessages;
			}
		} catch (Exception e) {
			log.warn("Redis chat history cache read error for room {}: {}", roomId, e.getMessage());
		}

		// Cache miss: Load from Neo4j
		List<Message> dbMessages = messageRepository.findMessagesByRoomId(roomId);
		if (dbMessages != null && !dbMessages.isEmpty()) {
			try {
				int start = Math.max(0, dbMessages.size() - 50);
				for (Message m : dbMessages.subList(start, dbMessages.size())) {
					redisTemplate.opsForList().rightPush(cacheKey, objectMapper.writeValueAsString(m));
				}
				redisTemplate.expire(cacheKey, Duration.ofDays(14));
			} catch (Exception e) {
				log.warn("Failed to hydrate Redis chat list for room {}: {}", roomId, e.getMessage());
			}
		}
		return dbMessages != null ? dbMessages : Collections.emptyList();
	}

	/**
	 * Get list of member user IDs for a room.
	 */
	public List<String> getRoomMemberIds(UUID roomId) {
		return chatRoomRepository.findById(roomId)
			.map(r -> r.getMemberIds() != null ? r.getMemberIds() : Collections.<String>emptyList())
			.orElse(Collections.emptyList());
	}

	/**
	 * Persist a new message, update chat room last activity, and push to Redis List recent cache.
	 */
	public Message saveMessage(Message message) {
		if (message.getCreatedAt() == null) {
			message.setCreatedAt(Instant.now());
		}
		if (message.getStatus() == null) {
			message.setStatus("SENT");
		}
		if (message.getReadByUserIds() == null) {
			message.setReadByUserIds(new ArrayList<>());
		}
		if (message.getSenderId() != null) {
			String senderStr = message.getSenderId().toString();
			if (!message.getReadByUserIds().contains(senderStr)) {
				message.getReadByUserIds().add(senderStr);
			}
		}

		Message savedMessage = messageRepository.save(message);

		// Cache recent message into Redis List: LTRIM -50 -1 keeps last 50 messages
		if (savedMessage.getRoomId() != null) {
			String cacheKey = "chat:room:" + savedMessage.getRoomId() + ":recent";
			try {
				String json = objectMapper.writeValueAsString(savedMessage);
				redisTemplate.opsForList().rightPush(cacheKey, json);
				redisTemplate.opsForList().trim(cacheKey, -50, -1);
				redisTemplate.expire(cacheKey, Duration.ofDays(14));
			} catch (Exception e) {
				log.warn("Failed to push message to Redis list {}: {}", cacheKey, e.getMessage());
			}
		}

		// Update room last message info
		chatRoomRepository.findById(message.getRoomId()).ifPresent(room -> {
			String preview = message.getContent();
			if ("IMAGE".equalsIgnoreCase(message.getType())) {
				preview = "📷 [Photo]";
			} else if ("VIDEO".equalsIgnoreCase(message.getType())) {
				preview = "🎥 [Video]";
			} else if ("LOCATION".equalsIgnoreCase(message.getType())) {
				preview = "📍 [Location]";
			} else if ("TRIP".equalsIgnoreCase(message.getType())) {
				preview = "🏍️ [Trip Card]";
			} else if ("AI_RESPONSE".equalsIgnoreCase(message.getType())) {
				preview = "🤖 [WayMate AI Assistant]";
			}

			room.setLastMessageContent(preview);
			room.setLastMessageSender(message.getSenderName());
			room.setLastMessageTime(message.getCreatedAt());
			room.setUpdatedAt(Instant.now());
			chatRoomRepository.save(room);
		});

		return savedMessage;
	}

	/**
	 * Update message status (DELIVERED or READ).
	 */
	public Message updateMessageStatus(UUID messageId, String status, UUID userId) {
		Message msg = messageRepository.findById(messageId)
			.orElseThrow(() -> new IllegalArgumentException("Message not found with id: " + messageId));

		msg.setStatus(status);
		if ("READ".equalsIgnoreCase(status) && userId != null) {
			List<String> readers = msg.getReadByUserIds();
			if (readers == null) {
				readers = new ArrayList<>();
			}
			String uidStr = userId.toString();
			if (!readers.contains(uidStr)) {
				readers.add(uidStr);
				msg.setReadByUserIds(readers);
			}
		}
		msg.setUpdatedAt(Instant.now());
		return messageRepository.save(msg);
	}

	/**
	 * Get room by ID.
	 */
	public Optional<ChatRoom> getRoomById(UUID roomId) {
		return chatRoomRepository.findById(roomId);
	}

	/**
	 * Helper to resolve direct conversation title & avatar based on who is viewing it.
	 */
	private void resolveDirectRoomMetadata(ChatRoom room, UUID viewerUserId) {
		if (room.getMemberIds() == null) return;
		String viewerStr = viewerUserId.toString();
		for (String midStr : room.getMemberIds()) {
			if (!midStr.equalsIgnoreCase(viewerStr)) {
				try {
					UUID otherId = UUID.fromString(midStr);
					userRepository.findById(otherId).ifPresent(otherUser -> {
						room.setName(otherUser.getName());
						if (otherUser.getAvatar() != null && !otherUser.getAvatar().isBlank()) {
							room.setAvatar(otherUser.getAvatar());
						}
					});
				} catch (Exception ignored) {}
				break;
			}
		}
	}
}
