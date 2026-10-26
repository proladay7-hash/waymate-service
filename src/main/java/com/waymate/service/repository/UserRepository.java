package com.waymate.service.repository;

import com.waymate.service.model.User;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data Neo4j repository for User node entities.
 */
@Repository
public interface UserRepository extends Neo4jRepository<User, UUID> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	Optional<User> findByName(String name);

	@org.springframework.data.neo4j.repository.query.Query(
		"MATCH (u:User) WHERE (toLower(u.name) CONTAINS toLower($query) OR toLower(u.email) CONTAINS toLower($query)) " +
		"AND (u.is_deleted = false OR u.is_deleted IS NULL) RETURN u LIMIT 25"
	)
	java.util.List<User> searchUsers(@org.springframework.data.repository.query.Param("query") String query);
}