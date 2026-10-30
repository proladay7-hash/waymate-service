package com.waymate.service.service;

import com.waymate.service.model.Post;
import com.waymate.service.repository.PostRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * High-speed Redis in-memory counters and Write-behind worker for social post interactions.
 * Handles millions of Likes, Shares, and Comments on RAM via atomic INCR/DECR and SADD,
 * and flushes dirty counters to the primary database in batches every 60 seconds.
 */
@Service
@Slf4j
public class RedisPostCounterService {

	public static final String DIRTY_POSTS_SET = "posts:dirty_counters";

	private final StringRedisTemplate redisTemplate;
	private final PostRepository postRepository;

	public RedisPostCounterService(
		StringRedisTemplate redisTemplate,
		@Lazy PostRepository postRepository
	) {
		this.redisTemplate = redisTemplate;
		this.postRepository = postRepository;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ToggleLikeResult {
		private boolean liked;
		private long likeCount;
	}

	/**
	 * Toggle Like / Unlike on a post with atomic Redis Set and INCR/DECR.
	 */
	public ToggleLikeResult toggleLike(UUID postId, UUID userId) {
		String pId = postId.toString();
		String uId = userId.toString();

		String likeKey = "post:" + pId + ":likes";
		String userSetKey = "post:" + pId + ":liked_users";

		ensureCountersInitialized(postId);

		Boolean alreadyLiked = redisTemplate.opsForSet().isMember(userSetKey, uId);
		boolean isLiked;
		long newLikeCount;

		if (Boolean.TRUE.equals(alreadyLiked)) {
			// Unlike
			redisTemplate.opsForSet().remove(userSetKey, uId);
			Long decr = redisTemplate.opsForValue().decrement(likeKey);
			newLikeCount = (decr != null && decr >= 0) ? decr : 0;
			if (decr != null && decr < 0) {
				redisTemplate.opsForValue().set(likeKey, "0");
			}
			isLiked = false;
		} else {
			// Like
			redisTemplate.opsForSet().add(userSetKey, uId);
			Long incr = redisTemplate.opsForValue().increment(likeKey);
			newLikeCount = incr != null ? incr : 1;
			isLiked = true;
		}

		// Mark post as dirty for background batch update to Neo4j
		redisTemplate.opsForSet().add(DIRTY_POSTS_SET, pId);

		return ToggleLikeResult.builder()
			.liked(isLiked)
			.likeCount(newLikeCount)
			.build();
	}

	/**
	 * Increment share counter atomically in Redis.
	 */
	public long incrementShare(UUID postId) {
		String pId = postId.toString();
		String shareKey = "post:" + pId + ":shares";

		ensureCountersInitialized(postId);

		Long shares = redisTemplate.opsForValue().increment(shareKey);
		redisTemplate.opsForSet().add(DIRTY_POSTS_SET, pId);
		return shares != null ? shares : 1;
	}

	/**
	 * Increment comment counter atomically in Redis.
	 */
	public long incrementComment(UUID postId) {
		String pId = postId.toString();
		String commentKey = "post:" + pId + ":comments";

		ensureCountersInitialized(postId);

		Long comments = redisTemplate.opsForValue().increment(commentKey);
		redisTemplate.opsForSet().add(DIRTY_POSTS_SET, pId);
		return comments != null ? comments : 1;
	}

	/**
	 * Check if a specific user has liked a post.
	 */
	public boolean hasUserLiked(UUID postId, UUID userId) {
		if (postId == null || userId == null) return false;
		String userSetKey = "post:" + postId + ":liked_users";
		Boolean isMember = redisTemplate.opsForSet().isMember(userSetKey, userId.toString());
		return Boolean.TRUE.equals(isMember);
	}

	/**
	 * Merges real-time in-memory Redis counters and liked status into a Post entity.
	 */
	public void populateLiveCounters(Post post, UUID viewerUserId) {
		if (post == null || post.getId() == null) return;
		String pId = post.getId().toString();

		try {
			String likesStr = redisTemplate.opsForValue().get("post:" + pId + ":likes");
			if (likesStr != null) {
				post.setLikeCount(Long.parseLong(likesStr));
			}

			String sharesStr = redisTemplate.opsForValue().get("post:" + pId + ":shares");
			if (sharesStr != null) {
				post.setShareCount(Long.parseLong(sharesStr));
			}

			String commentsStr = redisTemplate.opsForValue().get("post:" + pId + ":comments");
			if (commentsStr != null) {
				post.setCommentCount(Long.parseLong(commentsStr));
			}

			if (viewerUserId != null) {
				boolean liked = hasUserLiked(post.getId(), viewerUserId);
				List<String> likedUsers = post.getLikedByUserIds();
				if (likedUsers == null) likedUsers = new ArrayList<>();
				String vIdStr = viewerUserId.toString();
				if (liked && !likedUsers.contains(vIdStr)) {
					likedUsers.add(vIdStr);
				} else if (!liked) {
					likedUsers.remove(vIdStr);
				}
				post.setLikedByUserIds(likedUsers);
			}
		} catch (Exception e) {
			log.debug("Could not populate live Redis counters for post {}: {}", pId, e.getMessage());
		}
	}

	/**
	 * Ensure keys exist in Redis by lazily seeding from Neo4j if absent.
	 */
	private void ensureCountersInitialized(UUID postId) {
		String pId = postId.toString();
		String likeKey = "post:" + pId + ":likes";

		if (Boolean.FALSE.equals(redisTemplate.hasKey(likeKey))) {
			postRepository.findById(postId).ifPresent(post -> {
				long likes = post.getLikeCount() != null ? post.getLikeCount() : 0L;
				long shares = post.getShareCount() != null ? post.getShareCount() : 0L;
				long comments = post.getCommentCount() != null ? post.getCommentCount() : 0L;

				redisTemplate.opsForValue().setIfAbsent(likeKey, String.valueOf(likes));
				redisTemplate.opsForValue().setIfAbsent("post:" + pId + ":shares", String.valueOf(shares));
				redisTemplate.opsForValue().setIfAbsent("post:" + pId + ":comments", String.valueOf(comments));

				if (post.getLikedByUserIds() != null && !post.getLikedByUserIds().isEmpty()) {
					String userSetKey = "post:" + pId + ":liked_users";
					redisTemplate.opsForSet().add(userSetKey, post.getLikedByUserIds().toArray(new String[0]));
				}
			});
		}
	}

	/**
	 * Write-Behind scheduled worker:
	 * Runs every 60 seconds (1 minute), batches all modified counters from Redis RAM,
	 * and persists them into the Neo4j database once.
	 */
	@Scheduled(fixedRate = 60000)
	public void flushDirtyCountersToDatabase() {
		try {
			Set<String> dirtyPostIds = redisTemplate.opsForSet().members(DIRTY_POSTS_SET);
			if (dirtyPostIds == null || dirtyPostIds.isEmpty()) {
				return;
			}

			log.info("Write-behind worker starting: flushing {} dirty posts to Neo4j database", dirtyPostIds.size());
			int flushedCount = 0;

			for (String postIdStr : dirtyPostIds) {
				try {
					UUID postId = UUID.fromString(postIdStr);
					Optional<Post> postOpt = postRepository.findById(postId);
					if (postOpt.isPresent()) {
						Post post = postOpt.get();

						String likesStr = redisTemplate.opsForValue().get("post:" + postIdStr + ":likes");
						if (likesStr != null) {
							post.setLikeCount(Long.parseLong(likesStr));
						}

						String sharesStr = redisTemplate.opsForValue().get("post:" + postIdStr + ":shares");
						if (sharesStr != null) {
							post.setShareCount(Long.parseLong(sharesStr));
						}

						String commentsStr = redisTemplate.opsForValue().get("post:" + postIdStr + ":comments");
						if (commentsStr != null) {
							post.setCommentCount(Long.parseLong(commentsStr));
						}

						Set<String> likedUsers = redisTemplate.opsForSet().members("post:" + postIdStr + ":liked_users");
						if (likedUsers != null) {
							post.setLikedByUserIds(new ArrayList<>(likedUsers));
						}

						post.setUpdatedAt(Instant.now());
						postRepository.save(post);
						flushedCount++;
					}
					// Remove from dirty set once successfully persisted
					redisTemplate.opsForSet().remove(DIRTY_POSTS_SET, postIdStr);
				} catch (Exception e) {
					log.error("Failed to flush dirty post {} to DB: {}", postIdStr, e.getMessage());
				}
			}

			log.info("Write-behind worker complete: successfully flushed {} posts to Neo4j", flushedCount);
		} catch (Exception e) {
			log.error("Error in write-behind worker: {}", e.getMessage(), e);
		}
	}
}
