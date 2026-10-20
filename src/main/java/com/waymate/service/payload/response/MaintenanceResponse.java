package com.waymate.service.payload.response;

import com.waymate.service.model.Maintenance;
import com.waymate.service.model.Vehicle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Property;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceResponse {
    private UUID id;
    private String name;
    private Double price;
    private Instant createdAt;
    private Instant updatedAt;

    public static MaintenanceResponse fromMaintenance(Maintenance maintenance) {
        if (maintenance == null) {
            return null;
        }
        return MaintenanceResponse.builder()
                .id(maintenance.getId())
                .name(maintenance.getName())
                .price(maintenance.getPrice())
                .createdAt(maintenance.getCreatedAt())
                .updatedAt(maintenance.getUpdatedAt())
                .build();
    }

    public static List<MaintenanceResponse> fromMaintenances(List<Maintenance> maintenances) {
        if (maintenances == null) {
            return Collections.emptyList();
        }
        return maintenances.stream()
                .map(MaintenanceResponse::fromMaintenance)
                .toList();
    }
}
