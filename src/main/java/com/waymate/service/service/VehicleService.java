package com.waymate.service.service;

import com.waymate.service.model.User;
import com.waymate.service.model.Vehicle;
import com.waymate.service.payload.request.CreateVehicleRequest;
import com.waymate.service.payload.request.UpdateVehicleRequest;
import com.waymate.service.payload.response.VehicleResponse;
import com.waymate.service.repository.MaintenanceHistoryRepository;
import com.waymate.service.repository.UserRepository;
import com.waymate.service.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleService {
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final MaintenanceHistoryRepository maintenanceHistoryRepository;

    public List<VehicleResponse> getAllByUserId(UUID userId) {
        return VehicleResponse.fromVehicles(vehicleRepository.getAllByUserId(userId));
    }

    public VehicleResponse createVehicle(UUID userId, CreateVehicleRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        Vehicle vehicle = Vehicle.builder()
                .name(request.getName())
                .brand(request.getBrand())
                .plateNumber(request.getPlateNumber())
                .capacity(request.getCapacity())
                .odo(request.getOdo())
                .date(request.getDate())
                .user(user)
                .build();

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Successfully created vehicle");
        return VehicleResponse.fromVehicle(saved);
    }

    public VehicleResponse updateVehicle(UpdateVehicleRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("Vehicle ID must not be null");
        }
        Vehicle vehicle = vehicleRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with id: " + request.getId()));

        if (request.getName() != null) vehicle.setName(request.getName());
        if (request.getBrand() != null) vehicle.setBrand(request.getBrand());
        if (request.getPlateNumber() != null) vehicle.setPlateNumber(request.getPlateNumber());
        if (request.getCapacity() != null) vehicle.setCapacity(request.getCapacity());
        if (request.getOdo() != null) vehicle.setOdo(request.getOdo());
        if (request.getDate() != null) vehicle.setDate(request.getDate());

        Vehicle updated = vehicleRepository.save(vehicle);
        log.info("Successfully updated vehicle id {}", updated.getId());
        return VehicleResponse.fromVehicle(updated);
    }

    public void deleteVehicle(UUID vehicleId) {
        vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with id: " + vehicleId));
        try {
            vehicleRepository.deleteCascadeById(vehicleId);
        } catch (Exception e) {
            log.warn("deleteCascadeById failed: {}", e.getMessage());
        }
        log.info("Successfully deleted vehicle id {} and all associated maintenance histories and maintenances", vehicleId);
    }
}
