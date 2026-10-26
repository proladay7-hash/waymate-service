package com.waymate.service.repository;

import com.waymate.service.model.PostComment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data Neo4j repository for PostComment node entities.
 */
@Repository
public interface PostCommentRepository extends Neo4jRepository<PostComment, UUID> {

	@Query("MATCH (c:PostComment) WHERE c.post_id = $postId AND (c.is_deleted = false OR c.is_deleted IS NULL) " +
	       "RETURN c ORDER BY c.created_at ASC")
	List<PostComment> findCommentsByPostId(@Param("postId") UUID postId);

	@Query("MATCH (c:PostComment) WHERE c.post_id = $postId AND (c.is_deleted = false OR c.is_deleted IS NULL) " +
	       "RETURN count(c)")
	Long countCommentsByPostId(@Param("postId") UUID postId);
}
