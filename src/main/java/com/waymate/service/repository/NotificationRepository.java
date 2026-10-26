package com.waymate.service.repository;

import com.waymate.service.model.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data Neo4j repository for Notification node entities.
 */
@Repository
public interface NotificationRepository extends Neo4jRepository<Notification, UUID> {

	@Query("MATCH (n:Notification) WHERE n.user_id = $userId AND (n.type <> 'CHAT_MESSAGE' OR n.type IS NULL) AND (n.is_deleted = false OR n.is_deleted IS NULL) " +
	       "RETURN n ORDER BY n.created_at DESC")
	List<Notification> findByUserId(@Param("userId") UUID userId);

	@Query("MATCH (n:Notification) WHERE n.user_id = $userId AND (n.type <> 'CHAT_MESSAGE' OR n.type IS NULL) AND n.is_read = false " +
	       "AND (n.is_deleted = false OR n.is_deleted IS NULL) RETURN count(n)")
	long countUnreadByUserId(@Param("userId") UUID userId);
}
