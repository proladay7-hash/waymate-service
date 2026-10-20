package com.waymate.service.repository;

import com.waymate.service.model.MaintenanceHistory;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MaintenanceHistoryRepository extends Neo4jRepository<MaintenanceHistory, UUID> {
    @Query("MATCH (v:Vehicle {id: $vehicleId})-[hv:HAS_VEHICLE]->(mh:MaintenanceHistory) " +
           "OPTIONAL MATCH (mh)-[r:HAS_MAINTENANCE]->(m:Maintenance) " +
           "RETURN mh, collect(hv), collect(v), collect(r), collect(m)")
    List<MaintenanceHistory> getAllByVehicleId(@Param("vehicleId") UUID vehicleId);

    @Query("MATCH (mh:MaintenanceHistory {id: $maintenanceHistoryId}) " +
           "OPTIONAL MATCH (mh)-[:HAS_MAINTENANCE]->(m:Maintenance) " +
           "DETACH DELETE m, mh")
    void deleteCascadeById(@Param("maintenanceHistoryId") UUID maintenanceHistoryId);
}

