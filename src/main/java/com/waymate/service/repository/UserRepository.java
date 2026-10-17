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
}