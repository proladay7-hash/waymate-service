package com.waymate.service.controller;

import com.waymate.service.payload.request.CreateMaintenanceHistoryRequest;
import com.waymate.service.payload.request.UpdateMaintenanceHistoryRequest;
import com.waymate.service.payload.response.MaintenanceHistoryResponse;
import com.waymate.service.service.MaintenanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/maintenance")
@RequiredArgsConstructor
@Tag(name = "Maintenance", description = "Endpoints for managing maintenance history")
public class MaintenanceController {
    private final MaintenanceService maintenanceService;

    @GetMapping
    @Operation(summary = "Get vehicle's maintenance histories", description = "Get all maintenance histories by user id.")
    public ResponseEntity<List<MaintenanceHistoryResponse>> getAllByVehicle(@RequestParam("vehicleId") UUID vehicleId) {
        var response = maintenanceService.getAllByVehicleId(vehicleId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create vehicle's maintenance history", description = "Create a new maintenance history.")
    public ResponseEntity<MaintenanceHistoryResponse> createMaintenanceHistory(@RequestParam("vehicleId") UUID vehicleId,
                                                                               @RequestBody CreateMaintenanceHistoryRequest request) {
        var response = maintenanceService.createMaintenanceHistory(vehicleId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping
    @Operation(summary = "Update vehicle's maintenance history", description = "Update a maintenance history.")
    public ResponseEntity<MaintenanceHistoryResponse> updateMaintenanceHistory(@RequestBody UpdateMaintenanceHistoryRequest request) {
        var response = maintenanceService.updateMaintenanceHistory(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @DeleteMapping("/{maintenanceHistoryId}")
    @Operation(summary = "Delete vehicle's maintenance history", description = "Delete a maintenance history.")
    public ResponseEntity<Void> deleteMaintenanceHistory(@PathVariable("maintenanceHistoryId") UUID maintenanceHistoryId) {
        maintenanceService.deleteMaintenanceHistory(maintenanceHistoryId);
        return ResponseEntity.noContent().build();
    }
}
