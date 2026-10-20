package com.waymate.service.payload.response;

import com.waymate.service.model.Maintenance;
import com.waymate.service.model.MaintenanceHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceHistoryResponse {
    private UUID id;
    private String name;
    private Double total;
    private String location;
    private List<MaintenanceResponse> maintenances;
    private UUID vehicleId;
    private Instant createdAt;
    private Instant updatedAt;

    public static MaintenanceHistoryResponse fromMaintenanceHistory(MaintenanceHistory maintenanceHistory) {
        if (maintenanceHistory == null) {
            return null;
        }
        return MaintenanceHistoryResponse.builder()
                .id(maintenanceHistory.getId())
                .name(maintenanceHistory.getName())
                .total(maintenanceHistory.getTotal())
                .location(maintenanceHistory.getLocation())
                .vehicleId(maintenanceHistory.getVehicle() != null ? maintenanceHistory.getVehicle().getId() : null)
                .maintenances(MaintenanceResponse.fromMaintenances(maintenanceHistory.getMaintenances()))
                .createdAt(maintenanceHistory.getCreatedAt())
                .updatedAt(maintenanceHistory.getUpdatedAt())
                .build();
    }

    public static List<MaintenanceHistoryResponse> fromMaintenanceHistories(List<MaintenanceHistory> maintenanceHistories) {
        if (maintenanceHistories == null) {
            return Collections.emptyList();
        }
        return maintenanceHistories.stream()
                .map(MaintenanceHistoryResponse::fromMaintenanceHistory)
                .toList();
    }
}
