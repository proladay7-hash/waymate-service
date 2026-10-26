package com.waymate.service.repository;

import com.waymate.service.model.Post;
import java.util.List;
import java.util.UUID;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data Neo4j repository for Post node entities.
 */
@Repository
public interface PostRepository extends Neo4jRepository<Post, UUID> {

	@Query("MATCH (p:Post) WHERE (p.is_deleted = false OR p.is_deleted IS NULL) " +
	       "RETURN p ORDER BY p.created_at DESC")
	List<Post> findAllActivePosts();

	@Query("MATCH (p:Post) WHERE p.author_id = $authorId AND (p.is_deleted = false OR p.is_deleted IS NULL) " +
	       "RETURN p ORDER BY p.created_at DESC")
	List<Post> findPostsByAuthorId(@Param("authorId") UUID authorId);
}
