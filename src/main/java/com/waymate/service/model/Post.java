package com.waymate.service.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

/**
 * Post Neo4j Node entity.
 * Represents community social posts by riders and drivers, with rich media, location tags, and linked trips.
 */
@Node("Post")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Post extends BaseModel {

	@Id
	@GeneratedValue(GeneratedValue.UUIDGenerator.class)
	@Property("id")
	private UUID id;

	@Property("author_id")
	private UUID authorId;

	@Property("author_name")
	private String authorName;

	@Property("author_avatar")
	private String authorAvatar;

	@Property("content")
	private String content;

	@Property("media_urls")
	@Builder.Default
	private List<String> mediaUrls = new ArrayList<>();

	@Property("trip_id")
	private String tripId;

	@Property("location_name")
	private String locationName;

	@Property("like_count")
	@Builder.Default
	private Long likeCount = 0L;

	@Property("comment_count")
	@Builder.Default
	private Long commentCount = 0L;

	@Property("share_count")
	@Builder.Default
	private Long shareCount = 0L;

	@Property("liked_by_user_ids")
	@Builder.Default
	private List<String> likedByUserIds = new ArrayList<>();
}
