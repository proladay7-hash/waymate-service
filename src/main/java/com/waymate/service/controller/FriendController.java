package com.waymate.service.controller;

import com.waymate.service.model.Friendship;
import com.waymate.service.model.User;
import com.waymate.service.payload.response.UserResponse;
import com.waymate.service.repository.UserRepository;
import com.waymate.service.service.FriendshipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for friend requests, connections, rider search, and social networking.
 */
@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
@Tag(name = "Friends & Social Network", description = "Endpoints for friend connections, requests, and rider discovery")
public class FriendController {

	private final FriendshipService friendshipService;
	private final UserRepository userRepository;

	@Data @NoArgsConstructor @AllArgsConstructor @Builder
	public static class SendFriendRequestPayload {
		private UUID senderId;
		private UUID targetUserId;
	}

	@Data @NoArgsConstructor @AllArgsConstructor @Builder
	public static class AcceptFriendRequestPayload {
		private UUID friendshipId;
		private UUID currentUserId;
	}

	/**
	 * Get friend list.
	 */
	@GetMapping
	@Operation(summary = "Get friends", description = "Returns the list of accepted friends for a user.")
	public ResponseEntity<List<UserResponse>> getFriends(@RequestParam UUID userId) {
		List<User> friends = friendshipService.getFriends(userId);
		List<UserResponse> responses = friends.stream()
			.map(UserResponse::fromUser)
			.collect(Collectors.toList());
		return ResponseEntity.ok(responses);
	}

	/**
	 * Get pending incoming friend requests.
	 */
	@GetMapping("/pending")
	@Operation(summary = "Get pending requests", description = "Returns incoming pending friend requests.")
	public ResponseEntity<List<Map<String, Object>>> getPendingRequests(@RequestParam UUID userId) {
		return ResponseEntity.ok(friendshipService.getPendingRequests(userId));
	}

	/**
	 * Send a friend request.
	 */
	@PostMapping("/request")
	@Operation(summary = "Send friend request", description = "Sends a new friend request to another rider.")
	public ResponseEntity<Friendship> sendFriendRequest(@RequestBody SendFriendRequestPayload payload) {
		Friendship f = friendshipService.sendFriendRequest(payload.getSenderId(), payload.getTargetUserId());
		return ResponseEntity.ok(f);
	}

	/**
	 * Accept a friend request.
	 */
	@PostMapping("/accept")
	@Operation(summary = "Accept friend request", description = "Accepts a pending friend request.")
	public ResponseEntity<Friendship> acceptFriendRequest(@RequestBody AcceptFriendRequestPayload payload) {
		Friendship f = friendshipService.acceptFriendRequest(payload.getFriendshipId(), payload.getCurrentUserId());
		return ResponseEntity.ok(f);
	}

	/**
	 * Check friendship status between two users.
	 */
	@GetMapping("/status")
	@Operation(summary = "Check friendship status", description = "Returns FRIENDS, PENDING_OUTGOING, PENDING_INCOMING, or NONE.")
	public ResponseEntity<Map<String, String>> getStatus(@RequestParam UUID currentUserId, @RequestParam UUID targetUserId) {
		String status = friendshipService.getStatus(currentUserId, targetUserId);
		return ResponseEntity.ok(Map.of("status", status));
	}

	/**
	 * Search riders by name or email.
	 */
	@GetMapping("/search")
	@Operation(summary = "Search riders", description = "Finds riders by username or email for adding friends or creating groups.")
	public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam String query) {
		List<User> users = userRepository.searchUsers(query);
		List<UserResponse> responses = users.stream()
			.map(UserResponse::fromUser)
			.collect(Collectors.toList());
		return ResponseEntity.ok(responses);
	}
}
