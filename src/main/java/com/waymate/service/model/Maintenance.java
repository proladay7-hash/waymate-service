package com.waymate.service.model;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.UUID;

@Node("Maintenance")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Maintenance extends BaseModel {
    @Id
    @GeneratedValue(GeneratedValue.UUIDGenerator.class)
    @Property("id")
    private UUID id;

    @Property("name")
    private String name;

    @Property("price")
    private Double price;

    @Relationship(type = "HAS_MAINTENANCE", direction = Relationship.Direction.INCOMING)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MaintenanceHistory maintenanceHistory;
}
