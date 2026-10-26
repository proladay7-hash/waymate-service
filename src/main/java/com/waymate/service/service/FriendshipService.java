package com.waymate.service.service;

import com.waymate.service.model.Friendship;
import com.waymate.service.model.User;
import com.waymate.service.repository.FriendshipRepository;
import com.waymate.service.repository.UserRepository;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service managing friend requests, connections, and social rider networks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FriendshipService {

	private final FriendshipRepository friendshipRepository;
	private final UserRepository userRepository;

	/**
	 * Send a friend request to another rider.
	 */
	public Friendship sendFriendRequest(UUID senderId, UUID targetUserId) {
		if (senderId.equals(targetUserId)) {
			throw new IllegalArgumentException("Cannot send friend request to yourself");
		}

		Optional<Friendship> existing = friendshipRepository.findRelationshipBetween(senderId, targetUserId);
		if (existing.isPresent()) {
			Friendship f = existing.get();
			if ("ACCEPTED".equalsIgnoreCase(f.getStatus())) {
				return f; // Already friends
			}
			f.setStatus("PENDING");
			f.setActionUserId(senderId);
			f.setUpdatedAt(Instant.now());
			return friendshipRepository.save(f);
		}

		Friendship friendship = Friendship.builder()
			.user1Id(senderId)
			.user2Id(targetUserId)
			.status("PENDING")
			.actionUserId(senderId)
			.createdAt(Instant.now())
			.updatedAt(Instant.now())
			.build();

		return friendshipRepository.save(friendship);
	}

	/**
	 * Accept a friend request.
	 */
	public Friendship acceptFriendRequest(UUID friendshipId, UUID currentUserId) {
		Friendship f = friendshipRepository.findById(friendshipId)
			.orElseThrow(() -> new IllegalArgumentException("Friend request not found"));

		f.setStatus("ACCEPTED");
		f.setActionUserId(currentUserId);
		f.setUpdatedAt(Instant.now());
		return friendshipRepository.save(f);
	}

	/**
	 * Get list of accepted friends for a user.
	 */
	public List<User> getFriends(UUID userId) {
		List<Friendship> friendships = friendshipRepository.findAcceptedFriends(userId);
		List<User> friends = new ArrayList<>();

		for (Friendship f : friendships) {
			UUID friendId = f.getUser1Id().equals(userId) ? f.getUser2Id() : f.getUser1Id();
			userRepository.findById(friendId).ifPresent(friends::add);
		}
		return friends;
	}

	/**
	 * Get incoming pending friend requests for a user.
	 */
	public List<Map<String, Object>> getPendingRequests(UUID userId) {
		List<Friendship> pendingList = friendshipRepository.findPendingIncomingRequests(userId);
		List<Map<String, Object>> results = new ArrayList<>();

		for (Friendship f : pendingList) {
			UUID requesterId = f.getActionUserId();
			userRepository.findById(requesterId).ifPresent(user -> {
				Map<String, Object> item = new HashMap<>();
				item.put("friendshipId", f.getId());
				item.put("requesterId", user.getId());
				item.put("requesterName", user.getName());
				item.put("requesterAvatar", user.getAvatar());
				item.put("requesterBio", user.getBio());
				item.put("requestedAt", f.getCreatedAt());
				results.add(item);
			});
		}
		return results;
	}

	/**
	 * Check friendship status between two users.
	 * Returns: "FRIENDS", "PENDING_OUTGOING", "PENDING_INCOMING", or "NONE".
	 */
	public String getStatus(UUID currentUserId, UUID targetUserId) {
		if (currentUserId.equals(targetUserId)) {
			return "SELF";
		}
		Optional<Friendship> opt = friendshipRepository.findRelationshipBetween(currentUserId, targetUserId);
		if (opt.isEmpty()) {
			return "NONE";
		}
		Friendship f = opt.get();
		if ("ACCEPTED".equalsIgnoreCase(f.getStatus())) {
			return "FRIENDS";
		}
		if ("PENDING".equalsIgnoreCase(f.getStatus())) {
			return currentUserId.equals(f.getActionUserId()) ? "PENDING_OUTGOING" : "PENDING_INCOMING";
		}
		return "NONE";
	}
}
