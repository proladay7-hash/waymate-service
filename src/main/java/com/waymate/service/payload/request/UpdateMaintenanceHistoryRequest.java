package com.waymate.service.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMaintenanceHistoryRequest {
    @Schema(example = "d46ca61a-1fec-44ad-ad11-8b4c7953828c", requiredMode = REQUIRED)
    private UUID id;
    @Schema(example = "Period service", requiredMode = NOT_REQUIRED)
    private String name;
    @Schema(example = "10000", requiredMode = NOT_REQUIRED)
    private Double total;
    @Schema(example = "Yamaha3s", requiredMode = NOT_REQUIRED)
    private String location;
    @Schema(
        example = """
        [
          {
            "name": "Replace oil",
            "price": "500"
          }
        ]
        """, requiredMode = NOT_REQUIRED)
    private List<CreateMaintenanceRequest> maintenanceRequests;
}
