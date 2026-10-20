package com.waymate.service.service;

import com.waymate.service.model.Maintenance;
import com.waymate.service.model.MaintenanceHistory;
import com.waymate.service.model.Vehicle;
import com.waymate.service.payload.request.CreateMaintenanceHistoryRequest;
import com.waymate.service.payload.request.CreateMaintenanceRequest;
import com.waymate.service.payload.request.UpdateMaintenanceHistoryRequest;
import com.waymate.service.payload.response.MaintenanceHistoryResponse;
import com.waymate.service.repository.MaintenanceHistoryRepository;
import com.waymate.service.repository.MaintenanceRepository;
import com.waymate.service.repository.VehicleRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceService {
    private final MaintenanceRepository maintenanceRepository;
    private final MaintenanceHistoryRepository maintenanceHistoryRepository;
    private final VehicleRepository vehicleRepository;

    public List<MaintenanceHistoryResponse> getAllByVehicleId(UUID vehicleId) {
        List<MaintenanceHistory> histories = maintenanceHistoryRepository.getAllByVehicleId(vehicleId);
        return histories.stream()
                .map(history -> {
                    MaintenanceHistoryResponse response = MaintenanceHistoryResponse.fromMaintenanceHistory(history);
                    if (response.getVehicleId() == null) {
                        response.setVehicleId(vehicleId);
                    }
                    return response;
                })
                .toList();
    }

    public MaintenanceHistoryResponse createMaintenanceHistory(UUID vehicleId, CreateMaintenanceHistoryRequest request) {
        if (vehicleId == null) {
            throw new IllegalArgumentException("Vehicle ID must not be null");
        }
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with id: " + vehicleId));

        List<Maintenance> maintenances = request.getMaintenanceRequests() != null
                ? request.getMaintenanceRequests().stream()
                        .map(req -> Maintenance.builder()
                                .name(req.getName())
                                .price(req.getPrice())
                                .build())
                        .collect(Collectors.toList())
                : List.of();

        MaintenanceHistory maintenanceHistory = MaintenanceHistory.builder()
                .name(request.getName())
                .total(request.getTotal())
                .location(request.getLocation())
                .vehicle(vehicle)
                .maintenances(maintenances)
                .build();

        MaintenanceHistory saved = maintenanceHistoryRepository.save(maintenanceHistory);
        log.info("Successfully created maintenance history");
        return MaintenanceHistoryResponse.fromMaintenanceHistory(saved);
    }

    public MaintenanceHistoryResponse updateMaintenanceHistory(UpdateMaintenanceHistoryRequest request) {
        if (request == null || request.getId() == null) {
            throw new IllegalArgumentException("Maintenance history ID must not be null or blank");
        }

        UUID historyId = request.getId();
        MaintenanceHistory history = maintenanceHistoryRepository.findById(historyId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance history not found with id: " + historyId));

        if (request.getName() != null && !request.getName().isBlank()) {
            history.setName(request.getName().trim());
        }
        if (request.getLocation() != null && !request.getLocation().isBlank()) {
            history.setLocation(request.getLocation().trim());
        }
        if (request.getTotal() != null) {
            history.setTotal(request.getTotal());
        }

        // Manage nested maintenance service items
        if (request.getMaintenanceRequests() != null) {
            // Retrieve current maintenances linked to this history
            Map<UUID, Maintenance> currentMap = new LinkedHashMap<>();
            if (history.getMaintenances() != null) {
                for (Maintenance m : history.getMaintenances()) {
                    if (m != null && m.getId() != null) {
                        currentMap.put(m.getId(), m);
                    }
                }
            }
            List<Maintenance> fromRepo = maintenanceRepository.findAllByHistoryId(historyId);
            if (fromRepo != null) {
                for (Maintenance m : fromRepo) {
                    if (m != null && m.getId() != null) {
                        currentMap.putIfAbsent(m.getId(), m);
                    }
                }
            }
            List<Maintenance> currentMaintenances = new ArrayList<>(currentMap.values());

            Set<UUID> matchedExistingIds = new HashSet<>();
            List<Maintenance> finalMaintenances = new ArrayList<>();

            for (CreateMaintenanceRequest reqItem : request.getMaintenanceRequests()) {
                if (reqItem == null) {
                    continue;
                }

                Maintenance matched = null;

                // 1. Try matching by ID first if ID is present
                if (reqItem.getId() != null && !reqItem.getId().isBlank()) {
                    try {
                        UUID reqItemId = UUID.fromString(reqItem.getId().trim());
                        matched = currentMaintenances.stream()
                                .filter(m -> m.getId() != null && m.getId().equals(reqItemId) && !matchedExistingIds.contains(m.getId()))
                                .findFirst()
                                .orElse(null);
                    } catch (IllegalArgumentException ignored) {
                        // Ignore invalid UUID string, fallback to name matching
                    }
                }

                // 2. If not matched by ID, try matching by Name (case-insensitive)
                if (matched == null && reqItem.getName() != null && !reqItem.getName().isBlank()) {
                    String reqName = reqItem.getName().trim();
                    matched = currentMaintenances.stream()
                            .filter(m -> !matchedExistingIds.contains(m.getId())
                                    && m.getName() != null
                                    && m.getName().trim().equalsIgnoreCase(reqName))
                            .findFirst()
                            .orElse(null);
                }

                if (matched != null) {
                    // Update current maintenance
                    matchedExistingIds.add(matched.getId());
                    if (reqItem.getName() != null && !reqItem.getName().isBlank()) {
                        matched.setName(reqItem.getName().trim());
                    }
                    if (reqItem.getPrice() != null) {
                        matched.setPrice(reqItem.getPrice());
                    }
                    matched.setUpdatedAt(Instant.now());
                    matched.setMaintenanceHistory(history);
                    Maintenance savedExisting = maintenanceRepository.save(matched);
                    finalMaintenances.add(savedExisting);
                } else {
                    // Add only new maintenance
                    Maintenance newMaintenance = Maintenance.builder()
                            .name(reqItem.getName() != null ? reqItem.getName().trim() : "")
                            .price(reqItem.getPrice())
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .maintenanceHistory(history)
                            .build();
                    Maintenance savedNew = maintenanceRepository.save(newMaintenance);
                    finalMaintenances.add(savedNew);
                }
            }

            // Remove old maintenances that were not included in the update request
            List<Maintenance> toDelete = currentMaintenances.stream()
                    .filter(m -> m.getId() != null && !matchedExistingIds.contains(m.getId()))
                    .toList();

            if (!toDelete.isEmpty()) {
                List<UUID> toDeleteIds = toDelete.stream().map(Maintenance::getId).collect(Collectors.toList());
                try {
                    maintenanceRepository.detachDeleteAllById(toDeleteIds);
                } catch (Exception e) {
                    log.warn("detachDeleteAllById failed, falling back to deleteById: {}", e.getMessage());
                    for (Maintenance m : toDelete) {
                        try {
                            maintenanceRepository.deleteById(m.getId());
                        } catch (Exception ex) {
                            log.error("Failed to delete unused maintenance id: {}", m.getId(), ex);
                        }
                    }
                }
            }

            history.setMaintenances(finalMaintenances);
        }

        history.setUpdatedAt(Instant.now());

        MaintenanceHistory updated = maintenanceHistoryRepository.save(history);
        if (request.getMaintenanceRequests() != null && (updated.getMaintenances() == null || updated.getMaintenances().isEmpty())) {
            updated.setMaintenances(history.getMaintenances());
        }
        log.info("Successfully updated maintenance history id: {}", updated.getId());
        return MaintenanceHistoryResponse.fromMaintenanceHistory(updated);
    }

    public void deleteMaintenanceHistory(UUID maintenanceHistoryId) {
        MaintenanceHistory history = maintenanceHistoryRepository.findById(maintenanceHistoryId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance history not found with id: " + maintenanceHistoryId));
        try {
            maintenanceHistoryRepository.deleteCascadeById(maintenanceHistoryId);
        } catch (Exception e) {
            log.warn("deleteCascadeById failed: {}", e.getMessage());
        }
        log.info("Successfully deleted maintenance history id {} and all associated maintenances", maintenanceHistoryId);
    }
}
