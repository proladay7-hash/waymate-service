package com.waymate.service.repository;

import com.waymate.service.model.Vehicle;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VehicleRepository extends Neo4jRepository<Vehicle, UUID> {

    @Query("MATCH (u:User {id: $userId})-[:HAS_USER]->(v:Vehicle) RETURN v")
    List<Vehicle> getAllByUserId(@Param("userId") UUID userId);

    @Query("MATCH (v:Vehicle {id: $vehicleId}) " +
           "OPTIONAL MATCH (v)-[:HAS_VEHICLE]->(mh:MaintenanceHistory) " +
           "OPTIONAL MATCH (mh)-[:HAS_MAINTENANCE]->(m:Maintenance) " +
           "DETACH DELETE m, mh, v")
    void deleteCascadeById(@Param("vehicleId") UUID vehicleId);
}
