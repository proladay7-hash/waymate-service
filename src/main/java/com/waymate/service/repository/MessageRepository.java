package com.waymate.service.repository;

import com.waymate.service.model.Message;
import java.util.List;
import java.util.UUID;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data Neo4j repository for Message node entities.
 */
@Repository
public interface MessageRepository extends Neo4jRepository<Message, UUID> {

	@Query("MATCH (m:Message) WHERE toString(m.room_id) = toString($roomId) AND (m.is_deleted = false OR m.is_deleted IS NULL) " +
	       "RETURN m ORDER BY m.created_at ASC")
	List<Message> findMessagesByRoomId(@Param("roomId") UUID roomId);

	@Query("MATCH (m:Message) WHERE toString(m.room_id) = toString($roomId) AND (m.is_deleted = false OR m.is_deleted IS NULL) " +
	       "RETURN m ORDER BY m.created_at DESC LIMIT $limit")
	List<Message> findRecentMessagesByRoomId(@Param("roomId") UUID roomId, @Param("limit") int limit);

	@Query("MATCH (m:Message) WHERE toString(m.room_id) = toString($roomId) " +
	       "AND toString(m.sender_id) <> toString($userId) " +
	       "AND NOT (toString($userId) IN [uid IN coalesce(m.read_by_user_ids, []) | toString(uid)]) " +
	       "AND (m.status <> 'READ' OR m.status IS NULL) " +
	       "AND (m.is_deleted = false OR m.is_deleted IS NULL) " +
	       "RETURN count(m)")
	long countUnreadMessagesByRoom(@Param("roomId") UUID roomId, @Param("userId") UUID userId);

	@Query("MATCH (r:ChatRoom) WHERE toString($userIdStr) IN [m IN coalesce(r.member_ids, []) | toString(m)] " +
	       "MATCH (m:Message) WHERE toString(m.room_id) = toString(r.id) " +
	       "AND toString(m.sender_id) <> toString($userId) " +
	       "AND NOT (toString($userId) IN [uid IN coalesce(m.read_by_user_ids, []) | toString(uid)]) " +
	       "AND (m.status <> 'READ' OR m.status IS NULL) " +
	       "AND (m.is_deleted = false OR m.is_deleted IS NULL) " +
	       "RETURN count(m)")
	long countTotalUnreadForUser(@Param("userId") UUID userId, @Param("userIdStr") String userIdStr);
}
