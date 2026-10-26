package com.waymate.service.model;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

/**
 * Friendship Neo4j Node entity.
 * Represents friend connection and requests between riders.
 */
@Node("Friendship")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Friendship extends BaseModel {

	@Id
	@GeneratedValue(GeneratedValue.UUIDGenerator.class)
	@Property("id")
	private UUID id;

	@Property("user1_id")
	private UUID user1Id;

	@Property("user2_id")
	private UUID user2Id;

	/**
	 * Status:
	 * - PENDING: Friend request sent
	 * - ACCEPTED: Mutual friends
	 */
	@Property("status")
	@Builder.Default
	private String status = "PENDING";

	@Property("action_user_id")
	private UUID actionUserId;
}
