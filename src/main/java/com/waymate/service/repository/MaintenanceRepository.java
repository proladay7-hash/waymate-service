package com.waymate.service.repository;

import com.waymate.service.model.Maintenance;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;
import java.util.UUID;

public interface MaintenanceRepository extends Neo4jRepository<Maintenance, UUID> {
    @Query("MATCH (mh:MaintenanceHistory {id: $historyId})-[:HAS_MAINTENANCE]->(m:Maintenance) RETURN m")
    List<Maintenance> findAllByHistoryId(UUID historyId);

    @Query("MATCH (m:Maintenance) WHERE m.id IN $ids DETACH DELETE m")
    void detachDeleteAllById(List<UUID> ids);
}
