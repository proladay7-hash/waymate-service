package com.waymate.service.service;

import com.waymate.service.model.Notification;
import com.waymate.service.payload.response.NotificationResponse;
import com.waymate.service.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service managing user notifications and alerts across WayMate.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

	private final NotificationRepository notificationRepository;

	/**
	 * Create and persist a new notification in the database.
	 */
	public Notification createNotification(
		UUID userId,
		String title,
		String content,
		String type,
		UUID senderId,
		String senderName,
		String senderAvatar,
		String referenceId,
		String metadataJson
	) {
		// Do not push notifications for user's own actions (self-posts, self-comments, self-likes)
		if (userId != null && senderId != null && userId.equals(senderId)) {
			log.debug("Skipping self-notification for user {}", userId);
			return null;
		}

		Notification n = Notification.builder()
			.userId(userId)
			.title(title)
			.content(content)
			.type(type)
			.senderId(senderId)
			.senderName(senderName)
			.senderAvatar(senderAvatar)
			.referenceId(referenceId)
			.metadataJson(metadataJson)
			.isRead(false)
			.createdAt(Instant.now())
			.updatedAt(Instant.now())
			.build();

		Notification saved = notificationRepository.save(n);
		log.info("Created notification [id={}, type={}, userId={}]", saved.getId(), type, userId);
		return saved;
	}

	/**
	 * Get notifications for a user.
	 */
	public List<NotificationResponse> getUserNotifications(UUID userId) {
		return notificationRepository.findByUserId(userId).stream()
			.filter(n -> !"CHAT_MESSAGE".equalsIgnoreCase(n.getType()))
			.map(NotificationResponse::fromNotification)
			.collect(Collectors.toList());
	}

	/**
	 * Mark a specific notification as read.
	 */
	public Notification markAsRead(UUID notificationId) {
		Notification n = notificationRepository.findById(notificationId)
			.orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + notificationId));
		n.setIsRead(true);
		n.setUpdatedAt(Instant.now());
		return notificationRepository.save(n);
	}

	/**
	 * Mark all notifications for a user as read.
	 */
	public void markAllAsRead(UUID userId) {
		List<Notification> list = notificationRepository.findByUserId(userId);
		for (Notification n : list) {
			if (!Boolean.TRUE.equals(n.getIsRead())) {
				n.setIsRead(true);
				n.setUpdatedAt(Instant.now());
				notificationRepository.save(n);
			}
		}
	}

	/**
	 * Get count of unread notifications for a user.
	 */
	public long getUnreadCount(UUID userId) {
		return notificationRepository.countUnreadByUserId(userId);
	}
}
