package com.waymate.service.repository;

import com.waymate.service.model.Sos;
import java.util.List;
import java.util.UUID;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data Neo4j repository for SOS node entities.
 */
@Repository
public interface SosRepository extends Neo4jRepository<Sos, UUID> {

	@Query("MATCH (s:SOS) WHERE (s.is_deleted = false OR s.is_deleted IS NULL) " +
	       "OPTIONAL MATCH (s)-[:REQUESTED_BY]->(u:User) " +
	       "RETURN s, collect(u) ORDER BY s.created_at DESC")
	List<Sos> findAllActiveSos();

	@Query("MATCH (s:SOS) WHERE s.user_id = $userId AND (s.is_deleted = false OR s.is_deleted IS NULL) " +
	       "OPTIONAL MATCH (s)-[:REQUESTED_BY]->(u:User) " +
	       "RETURN s, collect(u) ORDER BY s.created_at DESC")
	List<Sos> findByUserId(@Param("userId") UUID userId);
}
