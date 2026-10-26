package com.waymate.service.repository;

import com.waymate.service.model.ChatRoom;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data Neo4j repository for ChatRoom node entities.
 */
@Repository
public interface ChatRoomRepository extends Neo4jRepository<ChatRoom, UUID> {

	@Query("MATCH (r:ChatRoom) WHERE $userId IN r.member_ids AND (r.is_deleted = false OR r.is_deleted IS NULL) " +
	       "RETURN r ORDER BY coalesce(r.last_message_time, r.created_at) DESC")
	List<ChatRoom> findRoomsByUserId(@Param("userId") String userId);

	@Query("MATCH (r:ChatRoom) WHERE r.type = 'DIRECT' AND $u1 IN r.member_ids AND $u2 IN r.member_ids " +
	       "AND (r.is_deleted = false OR r.is_deleted IS NULL) RETURN r LIMIT 1")
	Optional<ChatRoom> findDirectRoomBetween(@Param("u1") String u1, @Param("u2") String u2);
}
