package com.waymate.service.service;

import com.waymate.service.exception.RateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * High-performance Redis-based Rate Limiter preventing spam on posts, comments, likes, and chat.
 * Uses atomic INCR + EXPIRE sliding counters directly on Redis RAM without querying the primary DB.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisRateLimiterService {

	private final StringRedisTemplate stringRedisTemplate;

	// Limits configuration: max requests per 60 seconds window
	public static final int LIMIT_POST_CREATE = 5;       // 5 posts / minute
	public static final int LIMIT_POST_LIKE = 60;        // 60 likes / minute
	public static final int LIMIT_POST_COMMENT = 20;     // 20 comments / minute
	public static final int LIMIT_CHAT_MESSAGE = 30;     // 30 messages / minute
	public static final int WINDOW_SECONDS = 60;

	/**
	 * Tries to acquire rate limit slot. Returns true if within limit, false if exceeded.
	 */
	public boolean tryAcquire(String action, String userId, int maxRequests, int windowSeconds) {
		if (userId == null || userId.isBlank()) {
			return true;
		}

		String key = "rate:" + action + ":" + userId;
		try {
			Long currentCount = stringRedisTemplate.opsForValue().increment(key);
			if (currentCount != null && currentCount == 1) {
				stringRedisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
			}
			return currentCount != null && currentCount <= maxRequests;
		} catch (Exception e) {
			log.warn("Redis rate limiter error for key {}: {}. Allowing request as fallback.", key, e.getMessage());
			return true;
		}
	}

	/**
	 * Checks rate limit and throws RateLimitException (HTTP 429) if exceeded.
	 */
	public void checkLimit(String action, String userId, int maxRequests, int windowSeconds) {
		if (userId == null || userId.isBlank()) {
			return;
		}

		String key = "rate:" + action + ":" + userId;
		if (!tryAcquire(action, userId, maxRequests, windowSeconds)) {
			Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
			long retryAfter = (ttl != null && ttl > 0) ? ttl : windowSeconds;
			log.warn("Rate limit exceeded for action [{}] by user [{}]. Retry after {}s", action, userId, retryAfter);
			throw new RateLimitException(
				"Too many requests for " + action + ". You are allowed at most " + maxRequests + " requests per " + windowSeconds + " seconds.",
				retryAfter
			);
		}
	}

	public void checkPostCreation(UUID userId) {
		if (userId != null) {
			checkLimit("create_post", userId.toString(), LIMIT_POST_CREATE, WINDOW_SECONDS);
		}
	}

	public void checkPostLike(UUID userId) {
		if (userId != null) {
			checkLimit("like_post", userId.toString(), LIMIT_POST_LIKE, WINDOW_SECONDS);
		}
	}

	public void checkPostComment(UUID userId) {
		if (userId != null) {
			checkLimit("comment_post", userId.toString(), LIMIT_POST_COMMENT, WINDOW_SECONDS);
		}
	}

	public boolean allowChatMessage(String userId) {
		return tryAcquire("chat_message", userId, LIMIT_CHAT_MESSAGE, WINDOW_SECONDS);
	}
}
