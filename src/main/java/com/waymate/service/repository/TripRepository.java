package com.waymate.service.repository;

import com.waymate.service.model.Trip;
import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

/**
 * Spring Data Neo4j repository for Trip node entities.
 */
@Repository
public interface TripRepository extends Neo4jRepository<Trip, String> {

	@Query("MATCH (t:Trip) WHERE (t.is_deleted = false OR t.is_deleted IS NULL) RETURN t ORDER BY t.created_at DESC")
	List<Trip> findAllActiveTrips();
}
