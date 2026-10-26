package com.waymate.service.controller;

import com.waymate.service.model.Post;
import com.waymate.service.model.PostComment;
import com.waymate.service.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for community social posts, likes, comments, and sharing.
 */
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Tag(name = "Community Social Posts", description = "Social media endpoints for ride posts, photo/video sharing, likes, and comments")
public class PostController {

	private final PostService postService;

	@Data @NoArgsConstructor @AllArgsConstructor @Builder
	public static class CreatePostRequest {
		private UUID authorId;
		private String authorName;
		private String authorAvatar;
		private String content;
		private List<String> mediaUrls;
		private String tripId;
		private String locationName;
	}

	@Data @NoArgsConstructor @AllArgsConstructor @Builder
	public static class ToggleLikeRequest {
		private UUID userId;
	}

	@Data @NoArgsConstructor @AllArgsConstructor @Builder
	public static class AddCommentRequest {
		private UUID authorId;
		private String authorName;
		private String authorAvatar;
		private String content;
		private UUID parentId;
		private UUID rootCommentId;
		private UUID replyToUserId;
		private String replyToUserName;
	}

	/**
	 * Get community feed posts.
	 */
	@GetMapping
	@Operation(summary = "Get community feed", description = "Returns active ride posts in reverse chronological order.")
	public ResponseEntity<List<Post>> getCommunityFeed() {
		return ResponseEntity.ok(postService.getCommunityFeed());
	}

	/**
	 * Get posts by a specific user.
	 */
	@GetMapping("/user/{userId}")
	@Operation(summary = "Get user posts", description = "Returns all posts authored by a specific user.")
	public ResponseEntity<List<Post>> getUserPosts(@PathVariable UUID userId) {
		return ResponseEntity.ok(postService.getPostsByAuthor(userId));
	}

	/**
	 * Create a new social post.
	 */
	@PostMapping
	@Operation(summary = "Create post", description = "Publishes a new ride post with optional pictures, location, or trip.")
	public ResponseEntity<Post> createPost(@RequestBody CreatePostRequest request) {
		Post post = postService.createPost(
			request.getAuthorId(),
			request.getAuthorName() != null ? request.getAuthorName() : "Rider",
			request.getAuthorAvatar(),
			request.getContent(),
			request.getMediaUrls(),
			request.getTripId(),
			request.getLocationName()
		);
		return ResponseEntity.status(HttpStatus.CREATED).body(post);
	}

	/**
	 * Toggle Like/Unlike on a post.
	 */
	@PostMapping("/{postId}/like")
	@Operation(summary = "Toggle like on post", description = "Likes or unlikes a post; updates likeCount accordingly.")
	public ResponseEntity<Post> toggleLike(@PathVariable UUID postId, @RequestBody ToggleLikeRequest request) {
		Post updated = postService.toggleLike(postId, request.getUserId());
		return ResponseEntity.ok(updated);
	}

	/**
	 * Get comments for a post.
	 */
	@GetMapping("/{postId}/comments")
	@Operation(summary = "Get post comments", description = "Returns all comments for a post.")
	public ResponseEntity<List<PostComment>> getComments(@PathVariable UUID postId) {
		return ResponseEntity.ok(postService.getComments(postId));
	}

	/**
	 * Add a comment or reply to a post.
	 */
	@PostMapping("/{postId}/comments")
	@Operation(summary = "Add comment", description = "Adds a new comment or reply to a post and increments comment count.")
	public ResponseEntity<PostComment> addComment(@PathVariable UUID postId, @RequestBody AddCommentRequest request) {
		PostComment comment = postService.addComment(
			postId,
			request.getAuthorId(),
			request.getAuthorName() != null ? request.getAuthorName() : "Rider",
			request.getAuthorAvatar(),
			request.getContent(),
			request.getParentId(),
			request.getRootCommentId(),
			request.getReplyToUserId(),
			request.getReplyToUserName()
		);
		return ResponseEntity.status(HttpStatus.CREATED).body(comment);
	}

	/**
	 * Toggle Like/Unlike on a comment.
	 */
	@PostMapping("/{postId}/comments/{commentId}/like")
	@Operation(summary = "Toggle like on comment", description = "Likes or unlikes a comment.")
	public ResponseEntity<PostComment> toggleCommentLike(
		@PathVariable UUID postId,
		@PathVariable UUID commentId,
		@RequestBody ToggleLikeRequest request
	) {
		return ResponseEntity.ok(postService.toggleCommentLike(commentId, request.getUserId()));
	}

	/**
	 * Increment share count.
	 */
	@PostMapping("/{postId}/share")
	@Operation(summary = "Share post", description = "Increments post share count.")
	public ResponseEntity<Post> sharePost(@PathVariable UUID postId) {
		return ResponseEntity.ok(postService.incrementShare(postId));
	}
}
