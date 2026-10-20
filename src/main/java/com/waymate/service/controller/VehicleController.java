package com.waymate.service.controller;

import com.waymate.service.payload.request.CreateVehicleRequest;
import com.waymate.service.payload.request.UpdateVehicleRequest;
import com.waymate.service.payload.response.VehicleResponse;
import com.waymate.service.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vehicle")
@RequiredArgsConstructor
@Tag(name = "Vehicle", description = "Endpoints for managing vehicles")
public class VehicleController {
    private final VehicleService vehicleService;

    @GetMapping
    @Operation(summary = "Get user vehicles", description = "Get all vehicle by user id.")
    public ResponseEntity<List<VehicleResponse>> getAllByUser(@RequestParam("userId") UUID userId) {
        var response = vehicleService.getAllByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create user vehicle", description = "Create a new vehicle for a user by user id.")
    public ResponseEntity<VehicleResponse> createVehicle(@RequestParam("userId") UUID userId, @RequestBody CreateVehicleRequest request) {
        var response = vehicleService.createVehicle(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping
    @Operation(summary = "Update user vehicle", description = "Update a vehicle.")
    public ResponseEntity<VehicleResponse> updateVehicle(@RequestBody UpdateVehicleRequest request) {
        var response = vehicleService.updateVehicle(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @DeleteMapping("/{vehicleId}")
    @Operation(summary = "Delete user vehicle", description = "Delete a vehicle.")
    public ResponseEntity<Void> deleteVehicle(@PathVariable("vehicleId") UUID vehicleId) {
        vehicleService.deleteVehicle(vehicleId);
        return ResponseEntity.noContent().build();
    }
}
