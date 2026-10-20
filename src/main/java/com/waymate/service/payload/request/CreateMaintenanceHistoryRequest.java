package com.waymate.service.payload.request;

import com.waymate.service.model.Maintenance;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMaintenanceHistoryRequest {
    @Schema(example = "Period service", requiredMode = REQUIRED)
    private String name;
    @Schema(example = "10000", requiredMode = REQUIRED)
    private Double total;
    @Schema(example = "Yamaha3s", requiredMode = REQUIRED)
    private String location;
    @Schema(
        example = """
        [
          {
            "name": "Replace oil",
            "price": "500"
          }
        ]
        """, requiredMode = REQUIRED)
    private List<CreateMaintenanceRequest> maintenanceRequests;
}
