package com.waymate.service.payload.response;

import com.waymate.service.model.Vehicle;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleResponse {

    private UUID id;
    private String name;
    private Double capacity;
    private Integer odo;
    private Integer date;
    private String brand;
    private String plateNumber;
    private Instant createdAt;
    private Instant updatedAt;

    public static VehicleResponse fromVehicle(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .name(vehicle.getName())
                .capacity(vehicle.getCapacity())
                .odo(vehicle.getOdo())
                .date(vehicle.getDate())
                .brand(vehicle.getBrand())
                .plateNumber(vehicle.getPlateNumber())
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .build();
    }

    public static List<VehicleResponse> fromVehicles(List<Vehicle> vehicles) {
        if (vehicles == null) {
            return Collections.emptyList();
        }
        return vehicles.stream()
                .map(VehicleResponse::fromVehicle)
                .toList();
    }
}
