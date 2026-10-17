package com.waymate.service.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.neo4j.core.schema.Property;

/**
 * Base model for Neo4j domain entities providing auditing and soft-delete properties.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class BaseModel {

	@CreatedDate
	@Property("created_at")
	@Builder.Default
	private Instant createdAt = Instant.now();

	@LastModifiedDate
	@Property("updated_at")
	@Builder.Default
	private Instant updatedAt = Instant.now();

	@Property("is_deleted")
	@Builder.Default
	private Boolean isDeleted = false;
}