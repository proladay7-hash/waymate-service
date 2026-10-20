package com.waymate.service.model;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;
import java.util.UUID;

@Node("MaintenanceHistory")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MaintenanceHistory extends BaseModel {
    @Id
    @GeneratedValue(GeneratedValue.UUIDGenerator.class)
    @Property("id")
    private UUID id;

    @Property("name")
    private String name;

    @Property("total")
    private Double total;

    @Property("location")
    private String location;

    @Relationship(type = "HAS_MAINTENANCE", direction = Relationship.Direction.OUTGOING)
    private List<Maintenance> maintenances;

    @Relationship(type = "HAS_VEHICLE", direction = Relationship.Direction.INCOMING)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Vehicle vehicle;
}
