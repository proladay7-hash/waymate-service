package com.waymate.service.repository;

import com.waymate.service.model.Friendship;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data Neo4j repository for Friendship node entities.
 */
@Repository
public interface FriendshipRepository extends Neo4jRepository<Friendship, UUID> {

	@Query("MATCH (f:Friendship) WHERE (f.user1_id = $userId OR f.user2_id = $userId) " +
	       "AND f.status = 'ACCEPTED' AND (f.is_deleted = false OR f.is_deleted IS NULL) " +
	       "RETURN f")
	List<Friendship> findAcceptedFriends(@Param("userId") UUID userId);

	@Query("MATCH (f:Friendship) WHERE (f.user1_id = $userId OR f.user2_id = $userId) " +
	       "AND f.status = 'PENDING' AND f.action_user_id <> $userId AND (f.is_deleted = false OR f.is_deleted IS NULL) " +
	       "RETURN f")
	List<Friendship> findPendingIncomingRequests(@Param("userId") UUID userId);

	@Query("MATCH (f:Friendship) WHERE ((f.user1_id = $u1 AND f.user2_id = $u2) OR (f.user1_id = $u2 AND f.user2_id = $u1)) " +
	       "AND (f.is_deleted = false OR f.is_deleted IS NULL) RETURN f LIMIT 1")
	Optional<Friendship> findRelationshipBetween(@Param("u1") UUID u1, @Param("u2") UUID u2);
}
