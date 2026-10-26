package com.waymate.service.controller;

import com.waymate.service.model.Notification;
import com.waymate.service.payload.response.ApiResponse;
import com.waymate.service.payload.response.NotificationResponse;
import com.waymate.service.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user notifications, unread counters, and delivery state.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications & Alerts", description = "Endpoints for fetching and managing user alerts, chat notifications, and SOS warnings")
public class NotificationController {

	private final NotificationService notificationService;

	/**
	 * Create and persist a new notification in DB.
	 */
	@PostMapping
	@Operation(summary = "Create notification", description = "Persists a notification in DB and returns the response.")
	public ResponseEntity<NotificationResponse> createNotification(@RequestBody com.waymate.service.payload.request.NotificationRequest request) {
		Notification n = notificationService.createNotification(
			request.getUserId(),
			request.getTitle(),
			request.getContent(),
			request.getType() != null ? request.getType() : "SYSTEM",
			request.getSenderId(),
			request.getSenderName(),
			request.getSenderAvatar(),
			request.getReferenceId(),
			request.getMetadataJson()
		);
		return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
			.body(NotificationResponse.fromNotification(n));
	}

	/**
	 * Get all notifications for a user.
	 */
	@GetMapping
	@Operation(summary = "Get user notifications", description = "Fetches the notification feed for a user.")
	public ResponseEntity<List<NotificationResponse>> getUserNotifications(@RequestParam UUID userId) {
		return ResponseEntity.ok(notificationService.getUserNotifications(userId));
	}

	/**
	 * Get unread notification count.
	 */
	@GetMapping("/unread-count")
	@Operation(summary = "Get unread count", description = "Returns the number of unread notifications for a user.")
	public ResponseEntity<Map<String, Long>> getUnreadCount(@RequestParam UUID userId) {
		long count = notificationService.getUnreadCount(userId);
		return ResponseEntity.ok(Map.of("unreadCount", count));
	}

	/**
	 * Mark a single notification as read.
	 */
	@PostMapping("/{id}/read")
	@Operation(summary = "Mark notification as read", description = "Updates isRead to true for a given notification.")
	public ResponseEntity<Notification> markAsRead(@PathVariable UUID id) {
		return ResponseEntity.ok(notificationService.markAsRead(id));
	}

	/**
	 * Mark all notifications as read for a user.
	 */
	@PostMapping("/read-all")
	@Operation(summary = "Mark all as read", description = "Marks all unread notifications as read for the user.")
	public ResponseEntity<ApiResponse> markAllAsRead(@RequestParam UUID userId) {
		notificationService.markAllAsRead(userId);
		return ResponseEntity.ok(ApiResponse.builder().success(true).message("All notifications marked as read").build());
	}
}
