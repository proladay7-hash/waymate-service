package com.waymate.service.model;

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
 * PostComment Neo4j Node entity.
 * Represents a comment on a rider's social post.
 */
@Node("PostComment")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PostComment extends BaseModel {

	@Id
	@GeneratedValue(GeneratedValue.UUIDGenerator.class)
	@Property("id")
	private UUID id;

	@Property("post_id")
	private UUID postId;

	@Property("author_id")
	private UUID authorId;

	@Property("author_name")
	private String authorName;

	@Property("author_avatar")
	private String authorAvatar;

	@Property("content")
	private String content;

	@Property("parent_id")
	private UUID parentId;

	@Property("root_comment_id")
	private UUID rootCommentId;

	@Property("reply_to_user_id")
	private UUID replyToUserId;

	@Property("reply_to_user_name")
	private String replyToUserName;

	@Property("like_count")
	@Builder.Default
	private Long likeCount = 0L;

	@Property("liked_by_user_ids")
	@Builder.Default
	private java.util.List<String> likedByUserIds = new java.util.ArrayList<>();
}
