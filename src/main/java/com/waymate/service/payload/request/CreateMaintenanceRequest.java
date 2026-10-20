package com.waymate.service.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMaintenanceRequest {
    @Schema(example = "d46ca61a-1fec-44ad-ad11-8b4c7953828c", requiredMode = NOT_REQUIRED)
    private String id;

    @Schema(example = "Period service", requiredMode = REQUIRED)
    private String name;

    @Schema(example = "10000", requiredMode = REQUIRED)
    private Double price;
}
