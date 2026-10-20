package com.waymate.service.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateVehicleRequest {
    @Schema(example = "ed582b19-d9d8-4e97-826f-371c0e91d705", requiredMode = REQUIRED)
    private UUID id;
    @Schema(example = "MT-03", requiredMode = NOT_REQUIRED)
    private String name;
    @Schema(example = "321", requiredMode = NOT_REQUIRED)
    private Double capacity;
    @Schema(example = "1000", requiredMode = NOT_REQUIRED)
    private Integer odo;
    @Schema(example = "2018", requiredMode = NOT_REQUIRED)
    private Integer date;
    @Schema(example = "YAMAHA", requiredMode = NOT_REQUIRED)
    private String brand;
    @Schema(example = "59A3-249.22", requiredMode = NOT_REQUIRED)
    private String plateNumber;
}
