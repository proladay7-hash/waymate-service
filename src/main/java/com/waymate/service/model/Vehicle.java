package com.waymate.service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.*;

import java.util.List;
import java.util.UUID;

@Node("Vehicle")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Vehicle extends BaseModel {
    @Id
    @GeneratedValue(GeneratedValue.UUIDGenerator.class)
    @Property("id")
    private UUID id;

    @Property("name")
    private String name;

    @Property("capacity")
    private Double capacity;

    @Property("odo")
    private Integer odo;

    @Property("date")
    private Integer date;

    @Property("brand")
    private String brand;

    @Property("plate_number")
    private String plateNumber;

    @Relationship(type = "HAS_VEHICLE", direction = Relationship.Direction.OUTGOING)
    private List<MaintenanceHistory> maintenanceHistories;

    @Relationship(type = "HAS_USER", direction = Relationship.Direction.INCOMING)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private User user;
}
