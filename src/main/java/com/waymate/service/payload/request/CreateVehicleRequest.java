package com.waymate.service.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateVehicleRequest {
    @Schema(example = "MT-03", requiredMode = REQUIRED)
    private String name;
    @Schema(example = "321", requiredMode = REQUIRED)
    private Double capacity;
    @Schema(example = "1000", requiredMode = REQUIRED)
    private Integer odo;
    @Schema(example = "2018", requiredMode = REQUIRED)
    private Integer date;
    @Schema(example = "YAMAHA", requiredMode = REQUIRED)
    private String brand;
    @Schema(example = "59A3-249.22", requiredMode = REQUIRED)
    private String plateNumber;
}
