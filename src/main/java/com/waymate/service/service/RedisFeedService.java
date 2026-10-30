package com.waymate.service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waymate.service.model.Post;
import com.waymate.service.repository.PostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * High-performance Timeline & Feed service utilizing Redis Sorted Sets (ZSET).
 * Feed items are indexed by timestamp scores, enabling O(log N + M) reverse range pagination
 * via ZREVRANGE without heavy SQL/Cypher database joins.
 */
@Service
@Slf4j
public class RedisFeedService {

	public static final String COMMUNITY_FEED_KEY = "community:feed";
	public static final String USER_FEED_KEY_PREFIX = "user:feed:";
	public static final String POST_CACHE_PREFIX = "post:cache:";
	public static final Duration POST_CACHE_TTL = Duration.ofHours(24);

	private final StringRedisTemplate redisTemplate;
	private final PostRepository postRepository;
	private final RedisPostCounterService counterService;
	private final ObjectMapper objectMapper;

	public RedisFeedService(
		StringRedisTemplate redisTemplate,
		@Lazy PostRepository postRepository,
		RedisPostCounterService counterService,
		ObjectMapper objectMapper
	) {
		this.redisTemplate = redisTemplate;
		this.postRepository = postRepository;
		this.counterService = counterService;
		this.objectMapper = objectMapper;
	}

	/**
	 * Indexes a newly published post into community feed and author feed ZSETs.
	 * Score is post creation epoch milliseconds.
	 */
	public void addPostToFeed(Post post) {
		if (post == null || post.getId() == null) return;

		String postIdStr = post.getId().toString();
		long score = post.getCreatedAt() != null ? post.getCreatedAt().toEpochMilli() : Instant.now().toEpochMilli();

		try {
			// 1. ZADD community:feed <timestamp> <postId>
			redisTemplate.opsForZSet().add(COMMUNITY_FEED_KEY, postIdStr, score);

			// 2. ZADD user:feed:{authorId} <timestamp> <postId>
			if (post.getAuthorId() != null) {
				String userFeedKey = USER_FEED_KEY_PREFIX + post.getAuthorId();
				redisTemplate.opsForZSet().add(userFeedKey, postIdStr, score);
			}

			// 3. Cache post JSON payload in Redis with TTL
			String json = objectMapper.writeValueAsString(post);
			redisTemplate.opsForValue().set(POST_CACHE_PREFIX + postIdStr, json, POST_CACHE_TTL);

			log.info("Indexed post {} to Redis feed ZSET with score {}", postIdStr, score);
		} catch (Exception e) {
			log.error("Failed to index post {} to Redis feed: {}", postIdStr, e.getMessage(), e);
		}
	}

	/**
	 * Retrieves community feed posts using ZREVRANGE on Redis Sorted Set.
	 * Merges real-time in-memory counters and viewer like status.
	 */
	public List<Post> getCommunityFeed(int page, int size, UUID viewerUserId) {
		int start = Math.max(0, page) * Math.max(1, size);
		int end = start + Math.max(1, size) - 1;

		try {
			// ZREVRANGE community:feed <start> <end>
			Set<String> postIds = redisTemplate.opsForZSet().reverseRange(COMMUNITY_FEED_KEY, start, end);

			// Cold start / Cache warming: if ZSET is empty on first page, warm it up from DB
			if ((postIds == null || postIds.isEmpty()) && page == 0) {
				warmUpCommunityFeed();
				postIds = redisTemplate.opsForZSet().reverseRange(COMMUNITY_FEED_KEY, start, end);
			}

			if (postIds == null || postIds.isEmpty()) {
				return Collections.emptyList();
			}

			List<Post> feed = new ArrayList<>();
			for (String postIdStr : postIds) {
				Post post = resolvePost(postIdStr);
				if (post != null) {
					counterService.populateLiveCounters(post, viewerUserId);
					feed.add(post);
				}
			}

			return feed;
		} catch (Exception e) {
			log.error("Error retrieving community feed from Redis: {}. Falling back to Neo4j.", e.getMessage());
			List<Post> fallback = postRepository.findAllActivePosts();
			for (Post p : fallback) {
				counterService.populateLiveCounters(p, viewerUserId);
			}
			return fallback;
		}
	}

	/**
	 * Retrieves user-specific authored feed using ZREVRANGE on user:feed:{userId}.
	 */
	public List<Post> getUserFeed(UUID authorId, int page, int size, UUID viewerUserId) {
		if (authorId == null) return Collections.emptyList();

		int start = Math.max(0, page) * Math.max(1, size);
		int end = start + Math.max(1, size) - 1;
		String userFeedKey = USER_FEED_KEY_PREFIX + authorId;

		try {
			Set<String> postIds = redisTemplate.opsForZSet().reverseRange(userFeedKey, start, end);
			if (postIds == null || postIds.isEmpty()) {
				// Fallback to DB
				List<Post> userPosts = postRepository.findPostsByAuthorId(authorId);
				for (Post p : userPosts) {
					addPostToFeed(p);
					counterService.populateLiveCounters(p, viewerUserId);
				}
				return userPosts;
			}

			List<Post> posts = new ArrayList<>();
			for (String postIdStr : postIds) {
				Post post = resolvePost(postIdStr);
				if (post != null) {
					counterService.populateLiveCounters(post, viewerUserId);
					posts.add(post);
				}
			}
			return posts;
		} catch (Exception e) {
			log.error("Error retrieving user feed from Redis for author {}: {}", authorId, e.getMessage());
			return postRepository.findPostsByAuthorId(authorId);
		}
	}

	/**
	 * Resolves post from Redis cache; falls back to Neo4j if cache missed.
	 */
	private Post resolvePost(String postIdStr) {
		try {
			String cachedJson = redisTemplate.opsForValue().get(POST_CACHE_PREFIX + postIdStr);
			if (cachedJson != null && !cachedJson.isBlank()) {
				return objectMapper.readValue(cachedJson, Post.class);
			}
		} catch (Exception ignored) {}

		try {
			UUID postId = UUID.fromString(postIdStr);
			Optional<Post> postOpt = postRepository.findById(postId);
			if (postOpt.isPresent()) {
				Post post = postOpt.get();
				try {
					String json = objectMapper.writeValueAsString(post);
					redisTemplate.opsForValue().set(POST_CACHE_PREFIX + postIdStr, json, POST_CACHE_TTL);
				} catch (Exception ignored) {}
				return post;
			}
		} catch (Exception e) {
			log.debug("Could not resolve post {}: {}", postIdStr, e.getMessage());
		}
		return null;
	}

	/**
	 * Removes a deleted post from feeds and cache.
	 */
	public void removePostFromFeed(UUID postId, UUID authorId) {
		if (postId == null) return;
		String pId = postId.toString();
		redisTemplate.opsForZSet().remove(COMMUNITY_FEED_KEY, pId);
		if (authorId != null) {
			redisTemplate.opsForZSet().remove(USER_FEED_KEY_PREFIX + authorId, pId);
		}
		redisTemplate.delete(POST_CACHE_PREFIX + pId);
	}

	/**
	 * Cache warming for community feed from Neo4j database on initial load.
	 */
	public synchronized void warmUpCommunityFeed() {
		try {
			List<Post> activePosts = postRepository.findAllActivePosts();
			log.info("Warming up Redis community feed ZSET with {} posts from database...", activePosts.size());
			for (Post post : activePosts) {
				addPostToFeed(post);
			}
		} catch (Exception e) {
			log.error("Failed to warm up Redis community feed: {}", e.getMessage());
		}
	}
}
