package com.waymate.service.service;

import com.waymate.service.model.Post;
import com.waymate.service.model.PostComment;
import com.waymate.service.repository.PostCommentRepository;
import com.waymate.service.repository.PostRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service managing rider community social posts, likes, comments, and media sharing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

	private final PostRepository postRepository;
	private final PostCommentRepository postCommentRepository;
	private final NotificationService notificationService;
	private final com.waymate.service.repository.UserRepository userRepository;

	/**
	 * Get all active posts for the community feed.
	 */
	public List<Post> getCommunityFeed() {
		return postRepository.findAllActivePosts();
	}

	/**
	 * Get all posts created by a specific rider.
	 */
	public List<Post> getPostsByAuthor(UUID authorId) {
		return postRepository.findPostsByAuthorId(authorId);
	}

	/**
	 * Publish a new social ride post.
	 */
	public Post createPost(UUID authorId, String authorName, String authorAvatar, String content,
	                       List<String> mediaUrls, String tripId, String locationName) {
		Post post = Post.builder()
			.authorId(authorId)
			.authorName(authorName)
			.authorAvatar(authorAvatar)
			.content(content)
			.mediaUrls(mediaUrls != null ? mediaUrls : new ArrayList<>())
			.tripId(tripId)
			.locationName(locationName)
			.likeCount(0L)
			.commentCount(0L)
			.shareCount(0L)
			.likedByUserIds(new ArrayList<>())
			.createdAt(Instant.now())
			.updatedAt(Instant.now())
			.build();

		return postRepository.save(post);
	}

	/**
	 * Toggle Like/Unlike on a post.
	 * Returns updated post.
	 */
	public Post toggleLike(UUID postId, UUID userId) {
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + postId));

		List<String> likedUsers = post.getLikedByUserIds();
		if (likedUsers == null) {
			likedUsers = new ArrayList<>();
		}

		String uidStr = userId.toString();
		if (likedUsers.contains(uidStr)) {
			likedUsers.remove(uidStr);
			post.setLikeCount(Math.max(0, (post.getLikeCount() != null ? post.getLikeCount() : 1) - 1));
		} else {
			likedUsers.add(uidStr);
			post.setLikeCount((post.getLikeCount() != null ? post.getLikeCount() : 0) + 1);

			// Produce notification to post author if not self
			if (post.getAuthorId() != null && !post.getAuthorId().equals(userId)) {
				String likerName = userRepository.findById(userId).map(com.waymate.service.model.User::getName).orElse("A rider");
				String likerAvatar = userRepository.findById(userId).map(com.waymate.service.model.User::getAvatar).orElse(null);
				try {
					notificationService.createNotification(
						post.getAuthorId(),
						likerName + " liked your post ❤️",
						likerName + " reacted to your ride story.",
						"POST_LIKE",
						userId,
						likerName,
						likerAvatar,
						postId.toString(),
						null
					);
				} catch (Exception e) {
					log.warn("Failed to notify post like: {}", e.getMessage());
				}
			}
		}

		post.setLikedByUserIds(likedUsers);
		post.setUpdatedAt(Instant.now());
		return postRepository.save(post);
	}

	/**
	/**
	 * Add a comment or reply to a post.
	 */
	public PostComment addComment(
		UUID postId,
		UUID authorId,
		String authorName,
		String authorAvatar,
		String content,
		UUID parentId,
		UUID rootCommentId,
		UUID replyToUserId,
		String replyToUserName
	) {
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + postId));

		UUID actualRootCommentId = rootCommentId;
		if (parentId != null && actualRootCommentId == null) {
			PostComment parent = postCommentRepository.findById(parentId).orElse(null);
			if (parent != null) {
				actualRootCommentId = parent.getRootCommentId() != null ? parent.getRootCommentId() : parent.getId();
				if (replyToUserId == null) {
					replyToUserId = parent.getAuthorId();
				}
				if (replyToUserName == null) {
					replyToUserName = parent.getAuthorName();
				}
			}
		}

		PostComment comment = PostComment.builder()
			.postId(postId)
			.authorId(authorId)
			.authorName(authorName)
			.authorAvatar(authorAvatar)
			.content(content)
			.parentId(parentId)
			.rootCommentId(actualRootCommentId)
			.replyToUserId(replyToUserId)
			.replyToUserName(replyToUserName)
			.likeCount(0L)
			.likedByUserIds(new ArrayList<>())
			.createdAt(Instant.now())
			.updatedAt(Instant.now())
			.build();

		PostComment savedComment = postCommentRepository.save(comment);

		// Increment comment count on Post
		post.setCommentCount((post.getCommentCount() != null ? post.getCommentCount() : 0) + 1);
		post.setUpdatedAt(Instant.now());
		postRepository.save(post);

		String preview = (content != null && content.length() > 60) ? content.substring(0, 57) + "..." : (content != null ? content : "");

		// If this is a reply to a comment, notify the comment author being replied to
		if (replyToUserId != null && !replyToUserId.equals(authorId)) {
			try {
				notificationService.createNotification(
					replyToUserId,
					authorName + " replied to your comment 💬",
					authorName + ": \"" + preview + "\"",
					"POST_REPLY",
					authorId,
					authorName,
					authorAvatar,
					postId.toString(),
					null
				);
			} catch (Exception e) {
				log.warn("Failed to notify comment reply: {}", e.getMessage());
			}
		}

		// Produce notification to post author if commenter is not author (and not already notified as reply target)
		if (post.getAuthorId() != null && !post.getAuthorId().equals(authorId) && !post.getAuthorId().equals(replyToUserId)) {
			try {
				notificationService.createNotification(
					post.getAuthorId(),
					authorName + " commented on your post 💬",
					authorName + ": \"" + preview + "\"",
					"POST_COMMENT",
					authorId,
					authorName,
					authorAvatar,
					postId.toString(),
					null
				);
			} catch (Exception e) {
				log.warn("Failed to notify post comment: {}", e.getMessage());
			}
		}

		return savedComment;
	}

	/**
	 * Overload for backward compatibility.
	 */
	public PostComment addComment(UUID postId, UUID authorId, String authorName, String authorAvatar, String content) {
		return addComment(postId, authorId, authorName, authorAvatar, content, null, null, null, null);
	}

	/**
	 * Toggle like/unlike on a comment.
	 */
	public PostComment toggleCommentLike(UUID commentId, UUID userId) {
		PostComment comment = postCommentRepository.findById(commentId)
			.orElseThrow(() -> new IllegalArgumentException("Comment not found with id: " + commentId));

		List<String> likedUsers = comment.getLikedByUserIds();
		if (likedUsers == null) {
			likedUsers = new ArrayList<>();
		}

		String uidStr = userId.toString();
		if (likedUsers.contains(uidStr)) {
			likedUsers.remove(uidStr);
			comment.setLikeCount(Math.max(0, (comment.getLikeCount() != null ? comment.getLikeCount() : 1) - 1));
		} else {
			likedUsers.add(uidStr);
			comment.setLikeCount((comment.getLikeCount() != null ? comment.getLikeCount() : 0) + 1);

			// Produce notification to comment author if not self
			if (comment.getAuthorId() != null && !comment.getAuthorId().equals(userId)) {
				String likerName = userRepository.findById(userId).map(com.waymate.service.model.User::getName).orElse("A rider");
				String likerAvatar = userRepository.findById(userId).map(com.waymate.service.model.User::getAvatar).orElse(null);
				try {
					notificationService.createNotification(
						comment.getAuthorId(),
						likerName + " liked your comment ❤️",
						likerName + " reacted to your comment.",
						"COMMENT_LIKE",
						userId,
						likerName,
						likerAvatar,
						comment.getPostId() != null ? comment.getPostId().toString() : null,
						null
					);
				} catch (Exception e) {
					log.warn("Failed to notify comment like: {}", e.getMessage());
				}
			}
		}

		comment.setLikedByUserIds(likedUsers);
		comment.setUpdatedAt(Instant.now());
		return postCommentRepository.save(comment);
	}

	/**
	 * Get all comments for a post.
	 */
	public List<PostComment> getComments(UUID postId) {
		return postCommentRepository.findCommentsByPostId(postId);
	}

	/**
	 * Increment share count and notify author.
	 */
	public Post incrementShare(UUID postId) {
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + postId));

		post.setShareCount((post.getShareCount() != null ? post.getShareCount() : 0) + 1);
		post.setUpdatedAt(Instant.now());
		Post saved = postRepository.save(post);

		if (post.getAuthorId() != null) {
			try {
				notificationService.createNotification(
					post.getAuthorId(),
					"Your post was shared! 🚀",
					"A rider shared your ride post with the community",
					"POST_SHARE",
					null,
					"WayMate Community",
					null,
					postId.toString(),
					null
				);
			} catch (Exception e) {
				log.warn("Failed to notify post share: {}", e.getMessage());
			}
		}

		return saved;
	}
}
